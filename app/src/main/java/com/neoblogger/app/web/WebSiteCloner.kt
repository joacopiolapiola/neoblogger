package com.neoblogger.app.web

import com.neoblogger.app.data.ProjectRepository
import com.neoblogger.app.data.WebsiteProject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object WebSiteCloner {

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://neocities.org"

        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed.trimEnd('/')
        }

        if (!trimmed.contains(".") && !trimmed.contains("/")) {
            return "https://$trimmed.neocities.org"
        }

        return "https://$trimmed".trimEnd('/')
    }

    fun cloneWebSiteFromUrl(
        targetUrlInput: String,
        suggestedName: String,
        repository: ProjectRepository,
        onProgress: (String) -> Unit
    ): WebsiteProject {
        val rootUrl = normalizeUrl(targetUrlInput)
        val domainName = runCatching { URL(rootUrl).host }.getOrDefault("cloned-site")
        val cleanProjectName = suggestedName.trim().ifBlank { domainName }

        onProgress("Creating project '$cleanProjectName'...")
        val project = repository.createProject(cleanProjectName)

        val visitedUrls = mutableSetOf<String>()
        val downloadedAssets = mutableSetOf<String>()

        fun downloadTextFromUrl(urlString: String): String? {
            return runCatching {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) Neoblogger/0.1.0")
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                    reader.readText()
                } else null
            }.getOrNull()
        }

        fun downloadBinaryFromUrl(urlString: String, destRelativePath: String) {
            runCatching {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) Neoblogger/0.1.0")
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.use { input ->
                        val fileName = destRelativePath.substringAfterLast('/')
                        repository.addProjectAsset(project.id, fileName, input)
                    }
                }
            }
        }

        onProgress("Fetching main page from $rootUrl...")
        val mainHtml = downloadTextFromUrl("$rootUrl/index.html") ?: downloadTextFromUrl(rootUrl)

        if (mainHtml.isNullOrBlank()) {
            throw IllegalStateException("Could not reach or download $rootUrl")
        }

        visitedUrls.add("$rootUrl/index.html")
        visitedUrls.add(rootUrl)

        // Process and Save index.html
        var processedMainHtml: String = mainHtml

        // 1. Discover CSS Stylesheets
        val cssLinks = Regex("(?i)<link[^>]*href=[\"']([^\"']+\\.css)[\"'][^>]*>").findAll(mainHtml).map { it.groupValues[1] }.toList()
        for (cssRel in cssLinks) {
            val absoluteCssUrl = if (cssRel.startsWith("http")) cssRel else "$rootUrl/${cssRel.trimStart('/')}"
            val cssFileName = cssRel.substringAfterLast('/').ifBlank { "style.css" }

            onProgress("Downloading stylesheet: $cssFileName...")
            val cssContent = downloadTextFromUrl(absoluteCssUrl)
            if (cssContent != null) {
                repository.saveFile(project.id, cssFileName, cssContent)
            }
        }

        // 2. Discover JS Scripts
        val jsLinks = Regex("(?i)<script[^>]*src=[\"']([^\"']+\\.js)[\"'][^>]*>").findAll(mainHtml).map { it.groupValues[1] }.toList()
        for (jsRel in jsLinks) {
            val absoluteJsUrl = if (jsRel.startsWith("http")) jsRel else "$rootUrl/${jsRel.trimStart('/')}"
            val jsFileName = jsRel.substringAfterLast('/').ifBlank { "script.js" }

            onProgress("Downloading script: $jsFileName...")
            val jsContent = downloadTextFromUrl(absoluteJsUrl)
            if (jsContent != null) {
                repository.saveFile(project.id, jsFileName, jsContent)
            }
        }

        // 3. Discover Images / Media
        val imgLinks = Regex("(?i)<img[^>]*src=[\"']([^\"']+)[\"'][^>]*>").findAll(mainHtml).map { it.groupValues[1] }.toList()
        for (imgRel in imgLinks) {
            if (imgRel.startsWith("data:")) continue
            val absoluteImgUrl = if (imgRel.startsWith("http")) imgRel else "$rootUrl/${imgRel.trimStart('/')}"
            val imgFileName = imgRel.substringAfterLast('/').ifBlank { "image.png" }

            if (!downloadedAssets.contains(imgFileName)) {
                downloadedAssets.add(imgFileName)
                onProgress("Downloading asset: $imgFileName...")
                downloadBinaryFromUrl(absoluteImgUrl, "assets/$imgFileName")
            }

            processedMainHtml = processedMainHtml.replace(imgRel, "assets/$imgFileName")
        }

        repository.saveFile(project.id, "index.html", processedMainHtml)

        // 4. Discover Linked Internal HTML Pages
        val htmlPageLinks = Regex("(?i)<a[^>]*href=[\"']([^\"']+\\.html)[\"'][^>]*>").findAll(mainHtml).map { it.groupValues[1] }.toList()
        for (pageRel in htmlPageLinks) {
            if (pageRel.startsWith("http") && !pageRel.contains(domainName)) continue
            val pageFileName = pageRel.substringAfterLast('/')
            if (pageFileName.isBlank() || pageFileName.lowercase() == "index.html") continue

            val absolutePageUrl = if (pageRel.startsWith("http")) pageRel else "$rootUrl/${pageRel.trimStart('/')}"
            if (!visitedUrls.contains(absolutePageUrl)) {
                visitedUrls.add(absolutePageUrl)
                onProgress("Downloading page: $pageFileName...")
                val pageHtml = downloadTextFromUrl(absolutePageUrl)
                if (pageHtml != null) {
                    var processedPageHtml: String = pageHtml
                    for (assetName in downloadedAssets) {
                        processedPageHtml = processedPageHtml.replace(Regex("(?i)(src|href)=[\"'][^\"']*$assetName[\"']"), "assets/$assetName")
                    }
                    repository.saveFile(project.id, pageFileName, processedPageHtml)
                }
            }
        }

        onProgress("Website cloning complete!")
        return project
    }
}