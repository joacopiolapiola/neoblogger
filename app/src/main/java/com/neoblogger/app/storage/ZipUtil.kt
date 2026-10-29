package com.neoblogger.app.storage

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtil {
    fun unzip(zipStream: InputStream, targetDir: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        val targetCanonicalPath = targetDir.canonicalPath

        ZipInputStream(BufferedInputStream(zipStream)).use { zipInputStream ->
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                val entryName = entry.name.replace('\\', '/')
                val destFile = File(targetDir, entryName)
                val destCanonicalPath = destFile.canonicalPath

                if (entryName.contains("../") || entryName.startsWith("/") ||
                    !(destCanonicalPath.startsWith(targetCanonicalPath + File.separator) || destCanonicalPath == targetCanonicalPath)
                ) {
                    throw SecurityException("Zip Slip path traversal attempt detected: ${entry.name}")
                }

                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    FileOutputStream(destFile).use { out ->
                        zipInputStream.copyTo(out)
                    }
                    extractedFiles.add(destFile)
                }

                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        }
        return extractedFiles
    }

    fun zipDirectory(sourceDir: File, outputStream: OutputStream) {
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            compressFile(sourceDir, sourceDir, zipOut)
        }
    }

    private fun compressFile(rootDir: File, fileToCompress: File, zipOut: ZipOutputStream) {
        if (fileToCompress.isHidden) return

        if (fileToCompress.isDirectory) {
            val children = fileToCompress.listFiles() ?: return
            for (child in children) {
                compressFile(rootDir, child, zipOut)
            }
        } else {
            val relativePath = rootDir.toPath().relativize(fileToCompress.toPath()).toString().replace('\\', '/')
            val entry = ZipEntry(relativePath)
            zipOut.putNextEntry(entry)
            FileInputStream(fileToCompress).use { input ->
                input.copyTo(zipOut)
            }
            zipOut.closeEntry()
        }
    }
}