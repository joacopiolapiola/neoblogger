package com.neoblogger.app.data

import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "neoblogger_repo_test_" + System.currentTimeMillis())
        tempDir.mkdirs()
        repository = ProjectRepository(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun createProject_generatesExpectedStarterFiles() {
        val project = repository.createProject("My First Blog")

        assertNotNull(project.id)
        assertEquals("My First Blog", project.name)

        val projects = repository.listProjects()
        assertEquals(1, projects.size)
        assertEquals("My First Blog", projects[0].name)

        val html = repository.readFile(project.id, "index.html")
        assertTrue(html.contains("<!doctype html>"))
        assertTrue(html.contains("<title>My new site</title>"))

        val css = repository.readFile(project.id, "style.css")
        assertTrue(css.contains("box-sizing"))

        val js = repository.readFile(project.id, "script.js")
        assertTrue(js.contains("hello-button"))
    }

    @Test
    fun saveFile_writesAtomically() {
        val project = repository.createProject("Atomic Test")
        val updatedHtml = "<!doctype html><html><body><h1>Updated Page</h1></body></html>"

        repository.saveFile(project.id, "index.html", updatedHtml)

        val readBack = repository.readFile(project.id, "index.html")
        assertEquals(updatedHtml, readBack)
    }

    @Test
    fun listProjectFiles_listsFilesAndDirectories() {
        val project = repository.createProject("File List Test")
        repository.createProjectFile(project.id, "about.html", "<h1>About</h1>")

        val files = repository.listProjectFiles(project.id)
        assertTrue(files.any { it.relativePath == "index.html" })
        assertTrue(files.any { it.relativePath == "style.css" })
        assertTrue(files.any { it.relativePath == "script.js" })
        assertTrue(files.any { it.relativePath == "about.html" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun readFile_rejectsInvalidTraversalPath() {
        val project = repository.createProject("Security Test")
        repository.readFile(project.id, "../outside.txt")
    }

    @Test
    fun deleteProject_removesProjectDirectory() {
        val project = repository.createProject("To Delete")
        assertEquals(1, repository.listProjects().size)

        repository.deleteProject(project.id)
        assertEquals(0, repository.listProjects().size)
    }

    @Test
    fun createProjectFile_html_automaticallyCreatesPairedCss() {
        val project = repository.createProject("Auto Pair Test")
        repository.createProjectFile(project.id, "about.html", "")

        val html = repository.readFile(project.id, "about.html")
        assertTrue(html.contains("<link rel=\"stylesheet\" href=\"about.css\">"))

        val css = repository.readFile(project.id, "about.css")
        assertTrue(css.contains("Styles for about.html"))
    }

    @Test
    fun ensureFontFaceInCss_appendsFontFaceOnDemand() {
        val project = repository.createProject("Font Test")
        val fontsDir = File(repository.projectRoot(project.id), "assets/fonts")
        fontsDir.mkdirs()
        File(fontsDir, "MyCustomFont.ttf").writeText("dummy font data")

        val fontNames = repository.getImportedFontNames(project.id)
        assertTrue(fontNames.contains("MyCustomFont"))

        repository.ensureFontFaceInCss(project.id, "about.css", "MyCustomFont")
        val aboutCss = repository.readFile(project.id, "about.css")
        assertTrue(aboutCss.contains("font-family: 'MyCustomFont'"))
        assertTrue(aboutCss.contains("url('assets/fonts/MyCustomFont.ttf')"))
    }
}