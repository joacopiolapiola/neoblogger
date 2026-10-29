package com.neoblogger.app.storage

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZipUtilTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "neoblogger_zip_test_" + System.currentTimeMillis())
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun zipDirectory_and_unzip_roundtripsCorrectly() {
        val sourceDir = File(tempDir, "source")
        sourceDir.mkdirs()
        File(sourceDir, "index.html").writeText("<h1>Hello Zip</h1>", Charsets.UTF_8)

        val subDir = File(sourceDir, "css")
        subDir.mkdirs()
        File(subDir, "style.css").writeText("body { color: red; }", Charsets.UTF_8)

        val zipOut = ByteArrayOutputStream()
        ZipUtil.zipDirectory(sourceDir, zipOut)

        val targetDir = File(tempDir, "extracted")
        targetDir.mkdirs()

        ZipUtil.unzip(ByteArrayInputStream(zipOut.toByteArray()), targetDir)

        val extractedIndex = File(targetDir, "index.html")
        assertTrue(extractedIndex.exists())
        assertEquals("<h1>Hello Zip</h1>", extractedIndex.readText(Charsets.UTF_8))

        val extractedCss = File(targetDir, "css/style.css")
        assertTrue(extractedCss.exists())
        assertEquals("body { color: red; }", extractedCss.readText(Charsets.UTF_8))
    }

    @Test(expected = SecurityException::class)
    fun unzip_blocksZipSlipPathTraversal() {
        val maliciousZipOut = ByteArrayOutputStream()
        ZipOutputStream(maliciousZipOut).use { zos ->
            zos.putNextEntry(ZipEntry("../malicious.txt"))
            zos.write("malicious payload".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val targetDir = File(tempDir, "safe_target")
        targetDir.mkdirs()

        ZipUtil.unzip(ByteArrayInputStream(maliciousZipOut.toByteArray()), targetDir)
    }
}