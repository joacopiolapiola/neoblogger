package com.neoblogger.app.web

import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectOptimizerTest {

    private lateinit var tempProjectDir: File

    @Before
    fun setUp() {
        tempProjectDir = File(System.getProperty("java.io.tmpdir"), "neoblogger_opt_test_" + System.currentTimeMillis())
        tempProjectDir.mkdirs()

        val assetsDir = File(tempProjectDir, "assets")
        assetsDir.mkdirs()

        File(tempProjectDir, "index.html").writeText("""
            <!doctype html>
            <html>
            <head><link rel="stylesheet" href="style.css"></head>
            <body>
              <h1 class="active-title">Hello</h1>
              <img src="assets/used_image.png">
            </body>
            </html>
        """.trimIndent())

        File(tempProjectDir, "secret.html").writeText("<html><body><h1>Secret Page</h1></body></html>")
        File(tempProjectDir, "style.css").writeText(".active-title { color: red; } .unused-class { color: blue; }")

        File(assetsDir, "used_image.png").writeText("fake image bytes")
        File(assetsDir, "unused_photo.jpg").writeText("fake unused image bytes")
    }

    @After
    fun tearDown() {
        tempProjectDir.deleteRecursively()
    }

    @Test
    fun analyzeProject_detectsUnusedAssetsAndOrphanPages() {
        val report = ProjectOptimizer.analyzeProject(tempProjectDir)

        assertTrue(report.unusedAssetFiles.any { it.contains("unused_photo.jpg") })
        assertFalse(report.unusedAssetFiles.any { it.contains("used_image.png") })

        assertTrue(report.orphanHtmlPages.contains("secret.html"))
        assertFalse(report.orphanHtmlPages.contains("index.html"))

        assertTrue(report.unusedCssRules.contains(".unused-class"))
        assertFalse(report.unusedCssRules.contains(".active-title"))
    }

    @Test
    fun optimizeProject_purgesUnusedAssetsAndCss() {
        val options = OptimizationOptions(
            purgeUnusedAssets = true,
            cleanUnusedCss = true,
            compressMedia = false
        )

        val summary = ProjectOptimizer.optimizeProject(tempProjectDir, options) {}

        assertEquals(1, summary.purgedAssetsCount)
        assertFalse(File(tempProjectDir, "assets/unused_photo.jpg").exists())
        assertTrue(File(tempProjectDir, "assets/used_image.png").exists())

        val css = File(tempProjectDir, "style.css").readText()
        assertTrue(css.contains(".active-title"))
        assertFalse(css.contains(".unused-class"))
    }

    @Test
    fun generateSitemapXml_createsValidSitemapFile() {
        val sitemap = ProjectOptimizer.generateSitemapXml(tempProjectDir)
        assertTrue(sitemap.exists())

        val xml = sitemap.readText()
        assertTrue(xml.contains("<loc>index.html</loc>"))
        assertTrue(xml.contains("<loc>secret.html</loc>"))
        assertTrue(xml.contains("<priority>1.0</priority>"))
    }

    @Test
    fun autoAddAltToLinkedImages_derivesAltFromHrefTarget() {
        val htmlFile = File(tempProjectDir, "index.html")
        htmlFile.writeText("""
            <!doctype html>
            <html>
            <body>
              <a href="hola.html"><img src="123123.jpg"></a>
            </body>
            </html>
        """.trimIndent())

        val altAddedCount = ProjectOptimizer.autoAddAltToLinkedImages(tempProjectDir)
        assertEquals(1, altAddedCount)

        val updatedHtml = htmlFile.readText()
        assertTrue(updatedHtml.contains("alt=\"hola\""))
    }
}