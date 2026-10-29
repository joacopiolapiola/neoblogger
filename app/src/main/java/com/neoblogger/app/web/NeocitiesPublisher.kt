package com.neoblogger.app.web

import com.neoblogger.app.storage.NeocitiesConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object NeocitiesPublisher {

    private const val NEOCITIES_UPLOAD_URL = "https://neocities.org/api/upload"

    fun uploadProject(
        projectDir: File,
        config: NeocitiesConfig,
        onProgress: (String) -> Unit
    ) {
        require(config.isValid) { "Invalid Neocities Configuration. API Token or Site Name is required." }
        require(projectDir.isDirectory) { "Project directory not found." }

        onProgress("Preparing files for Neocities upload...")
        val filesToUpload = projectDir.walkTopDown()
            .filter { it.isFile && it.name != "project-name.txt" && !it.name.startsWith(".") }
            .toList()

        if (filesToUpload.isEmpty()) {
            onProgress("No files to upload to Neocities.")
            return
        }

        val boundary = "---NeobloggerBoundary" + UUID.randomUUID().toString().take(12)
        val url = URL(NEOCITIES_UPLOAD_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.useCaches = false
        conn.connectTimeout = 20000
        conn.readTimeout = 30000

        // Set Authentication
        if (config.apiToken.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer ${config.apiToken.trim()}")
        } else if (config.siteName.isNotBlank()) {
            val encodedAuth = android.util.Base64.encodeToString(
                "${config.siteName.trim()}:".toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            conn.setRequestProperty("Authorization", "Basic $encodedAuth")
        }

        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        val outputStream: OutputStream = conn.outputStream
        val writer = PrintWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), true)

        for ((index, file) in filesToUpload.withIndex()) {
            val relativePath = projectDir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            onProgress("Uploading to Neocities ($index/${filesToUpload.size}): $relativePath")

            writer.append("--").append(boundary).append("\r\n")
            writer.append("Content-Disposition: form-data; name=\"$relativePath\"; filename=\"${file.name}\"\r\n")
            writer.append("Content-Type: application/octet-stream\r\n\r\n")
            writer.flush()

            file.inputStream().use { input ->
                input.copyTo(outputStream)
            }
            outputStream.flush()
            writer.append("\r\n")
            writer.flush()
        }

        writer.append("--").append(boundary).append("--\r\n")
        writer.flush()

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            onProgress("Neocities Upload Complete!")
        } else {
            val errReader = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream, Charsets.UTF_8))
            val errMessage = errReader.readText()
            throw IllegalStateException("Neocities Error ($responseCode): $errMessage")
        }
    }
}