package com.neoblogger.app.web

import com.neoblogger.app.storage.FtpConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket
import java.util.regex.Pattern

object FtpPublisher {

    fun uploadProject(
        projectDir: File,
        config: FtpConfig,
        onProgress: (String) -> Unit
    ) {
        require(config.isValid) { "Invalid FTP Configuration. Host and Username are required." }
        require(projectDir.isDirectory) { "Project directory not found." }

        onProgress("Connecting to FTP host ${config.host}:${config.port}...")
        val controlSocket = Socket(config.host, config.port)
        controlSocket.soTimeout = 15000

        val reader = BufferedReader(InputStreamReader(controlSocket.getInputStream(), Charsets.UTF_8))
        val writer = PrintWriter(controlSocket.getOutputStream(), true)

        fun readResponse(): String {
            val line = reader.readLine().orEmpty()
            if (line.startsWith("4") || line.startsWith("5")) {
                throw IllegalStateException("FTP Error: $line")
            }
            return line
        }

        fun sendCommand(cmd: String): String {
            writer.println(cmd)
            return readResponse()
        }

        try {
            readResponse() // 220 Welcome

            onProgress("Authenticating user ${config.user}...")
            sendCommand("USER ${config.user}")
            sendCommand("PASS ${config.pass}")

            sendCommand("TYPE I") // Binary mode

            if (config.path.isNotBlank() && config.path != "/") {
                onProgress("Changing remote directory to ${config.path}...")
                runCatching { sendCommand("CWD ${config.path}") }
                    .onFailure {
                        runCatching { sendCommand("MKD ${config.path}") }
                        sendCommand("CWD ${config.path}")
                    }
            }

            val filesToUpload = projectDir.walkTopDown()
                .filter { it.isFile && it.name != "project-name.txt" && !it.name.startsWith(".") }
                .toList()

            for ((index, file) in filesToUpload.withIndex()) {
                val relativePath = projectDir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                onProgress("Uploading ($index/${filesToUpload.size}): $relativePath")

                // Open Passive Data Connection
                val pasvResponse = sendCommand("PASV")
                val dataSocket = openPassiveDataSocket(config.host, pasvResponse)

                // Ensure remote parent directories exist
                val remoteParent = file.parentFile
                if (remoteParent != null && remoteParent != projectDir) {
                    val relParent = projectDir.toPath().relativize(remoteParent.toPath()).toString().replace('\\', '/')
                    ensureRemoteDirExists(relParent, ::sendCommand)
                }

                sendCommand("STOR $relativePath")

                // Write file data
                val dataOut: OutputStream = dataSocket.getOutputStream()
                file.inputStream().use { input ->
                    input.copyTo(dataOut)
                }
                dataOut.flush()
                dataSocket.close()

                readResponse() // 226 Transfer complete
            }

            onProgress("FTP Upload Complete!")
            runCatching { sendCommand("QUIT") }
        } finally {
            runCatching { controlSocket.close() }
        }
    }

    private fun openPassiveDataSocket(defaultHost: String, pasvResponse: String): Socket {
        val pattern = Pattern.compile("(?i)227.*\\((\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)\\)")
        val matcher = pattern.matcher(pasvResponse)
        if (!matcher.find()) {
            throw IllegalStateException("Failed to parse PASV response: $pasvResponse")
        }

        val ip = "${matcher.group(1)}.${matcher.group(2)}.${matcher.group(3)}.${matcher.group(4)}"
        val p1 = matcher.group(5).toInt()
        val p2 = matcher.group(6).toInt()
        val port = p1 * 256 + p2

        val socket = Socket(ip, port)
        socket.soTimeout = 15000
        return socket
    }

    private fun ensureRemoteDirExists(relPath: String, sendCommand: (String) -> String) {
        val segments = relPath.split('/')
        for (seg in segments) {
            if (seg.isBlank()) continue
            runCatching { sendCommand("CWD $seg") }.onFailure {
                runCatching { sendCommand("MKD $seg") }
                runCatching { sendCommand("CWD $seg") }
            }
        }
    }
}