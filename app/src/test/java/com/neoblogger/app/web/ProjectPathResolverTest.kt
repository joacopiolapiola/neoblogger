package com.neoblogger.app.web

import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectPathResolverTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun resolvesOnlyFilesInsideTheActiveProject() {
        val projectsRoot = temporaryFolder.newFolder("projects")
        val activeId = UUID.randomUUID().toString()
        val siblingId = UUID.randomUUID().toString()
        val activeProject = File(projectsRoot, activeId).apply { mkdir() }
        val siblingProject = File(projectsRoot, siblingId).apply { mkdir() }
        val expectedFile = File(activeProject, "index.html").apply { writeText("site") }
        File(siblingProject, "secret.html").writeText("private")
        val resolver = ProjectPathResolver(activeProject, activeId)

        assertEquals(expectedFile.canonicalFile, resolver.resolve("$activeId/index.html"))
        assertNull(resolver.resolve("$siblingId/secret.html"))
        assertNull(resolver.resolve("$activeId/../$siblingId/secret.html"))
        assertNull(resolver.resolve("$activeId/%2e%2e/secret.html"))
        assertNotNull(resolver.resolve("$activeId/index.html"))
    }
}