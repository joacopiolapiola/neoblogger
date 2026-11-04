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

    fun resolveAbsoluteUrl(baseUrl: String, relativeRef: String): String? {
        val trimmedRef = relativeRef.trim()
        if (trimmedRef.isBlank() || trimmedRef.startsWith("data:") || trimmedRef.startsWith("#") || trimmedRef.startsWith("javascript:")) {
            return null
        }
        return runCatching {
            URL(URL(baseUrl), trimmedRef).toString().substringBefore('#')
        }.getOrNull()
    }

    fun deriveRelativePath(rootUrl: String, absoluteUrl: String): String? {
        val cleanRoot = rootUrl.trimEnd('/')
        val cleanAbs = absoluteUrl.substringBefore('?')

        if (!cleanAbs.startsWith(cleanRoot, ignoreCase = true)) return null

        var rel = cleanAbs.substring(cleanRoot.length).trimStart('/')
        if (rel.isBlank()) rel = "index.html"
        return rel
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

        val pageQueue = ArrayDeque<String>()
        val visitedPageUrls = mutableSetOf<String>()
        val downloadedResourcePaths = mutableSetOf<String>()

        fun downloadText(urlString: String): String? {
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

        fun downloadBinary(urlString: String, relPath: String) {
            runCatching {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) Neoblogger/0.1.0")
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.use { input ->
                        repository.addProjectAsset(project.id, relPath, input)
                    }
                }
            }
        }

        val initialIndexUrl = "$rootUrl/index.html"
        pageQueue.addLast(initialIndexUrl)
        visitedPageUrls.add(initialIndexUrl)
        visitedPageUrls.add(rootUrl)
        visitedPageUrls.add("$rootUrl/")

        onProgress("Starting website crawl from $rootUrl...")

        while (pageQueue.isNotEmpty()) {
            val currentPageUrl = pageQueue.removeFirst()
            val relHtmlPath = deriveRelativePath(rootUrl, currentPageUrl) ?: "index.html"

            onProgress("Downloading HTML ($relHtmlPath)...")
            val htmlContent = downloadText(currentPageUrl) ?: if (currentPageUrl == initialIndexUrl) downloadText(rootUrl) else null

            if (htmlContent.isNullOrBlank()) continue

            // 1. Scan and Download CSS Stylesheets
            val cssMatches = Regex("(?i)<link[^>]*href=[\"']([^\"']+\\.css[^\"']*)[\"'][^>]*>").findAll(htmlContent)
            for (match in cssMatches) {
                val cssRef = match.groupValues[1]
                val absCssUrl = resolveAbsoluteUrl(currentPageUrl, cssRef) ?: continue
                val relCssPath = deriveRelativePath(rootUrl, absCssUrl) ?: continue

                if (!downloadedResourcePaths.contains(relCssPath)) {
                    downloadedResourcePaths.add(relCssPath)
                    onProgress("Downloading CSS: $relCssPath...")
                    val cssText = downloadText(absCssUrl)
                    if (cssText != null) {
                        repository.createProjectFile(project.id, relCssPath, cssText)

                        // Scan CSS for url(...) assets
                        val urlMatches = Regex("(?i)url\\([\"']?([^\"')]+)[\"']?\\)").findAll(cssText)
                        for (uMatch in urlMatches) {
                            val assetRef = uMatch.groupValues[1]
                            val absAssetUrl = resolveAbsoluteUrl(absCssUrl, assetRef) ?: continue
                            val relAssetPath = deriveRelativePath(rootUrl, absAssetUrl) ?: continue
                            if (!downloadedResourcePaths.contains(relAssetPath)) {
                                downloadedResourcePaths.add(relAssetPath)
                                onProgress("Downloading CSS asset: $relAssetPath...")
                                downloadBinary(absAssetUrl, relAssetPath)
                            }
                        }
                    }
                }
            }

            // 2. Scan and Download JS Scripts
            val jsMatches = Regex("(?i)<script[^>]*src=[\"']([^\"']+\\.js[^\"']*)[\"'][^>]*>").findAll(htmlContent)
            for (match in jsMatches) {
                val jsRef = match.groupValues[1]
                val absJsUrl = resolveAbsoluteUrl(currentPageUrl, jsRef) ?: continue
                val relJsPath = deriveRelativePath(rootUrl, absJsUrl) ?: continue

                if (!downloadedResourcePaths.contains(relJsPath)) {
                    downloadedResourcePaths.add(relJsPath)
                    onProgress("Downloading JS: $relJsPath...")
                    val jsText = downloadText(absJsUrl)
                    if (jsText != null) {
                        repository.createProjectFile(project.id, relJsPath, jsText)
                    }
                }
            }

            // 3. Scan and Download Images & Media
            val mediaMatches = Regex("(?i)<(img|source|video|audio)[^>]*src=[\"']([^\"']+)[\"'][^>]*>").findAll(htmlContent)
            for (match in mediaMatches) {
                val mediaRef = match.groupValues[2]
                val absMediaUrl = resolveAbsoluteUrl(currentPageUrl, mediaRef) ?: continue
                val relMediaPath = deriveRelativePath(rootUrl, absMediaUrl) ?: continue

                if (!downloadedResourcePaths.contains(relMediaPath)) {
                    downloadedResourcePaths.add(relMediaPath)
                    onProgress("Downloading media: $relMediaPath...")
                    downloadBinary(absMediaUrl, relMediaPath)
                }
            }

            // Save HTML File
            repository.createProjectFile(project.id, relHtmlPath, htmlContent)

            // 4. Scan and Enqueue Internal HTML Links
            val pageMatches = Regex("(?i)<a[^>]*href=[\"']([^\"']+\\.html[^\"']*)[\"'][^>]*>").findAll(htmlContent)
            for (match in pageMatches) {
                val pageRef = match.groupValues[1]
                val absPageUrl = resolveAbsoluteUrl(currentPageUrl, pageRef) ?: continue

                if (absPageUrl.startsWith(rootUrl, ignoreCase = true) && !visitedPageUrls.contains(absPageUrl)) {
                    visitedPageUrls.add(absPageUrl)
                    pageQueue.addLast(absPageUrl)
                }
            }
        }

        onProgress("Website cloning complete! All pages and assets downloaded.")
        return project
    }
}