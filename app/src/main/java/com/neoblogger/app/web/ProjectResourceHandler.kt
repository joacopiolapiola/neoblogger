package com.neoblogger.app.web

import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream

class ProjectResourceHandler(
    private val projectRoot: File,
    private val projectId: String,
) : WebViewAssetLoader.PathHandler {
    private val pathResolver = ProjectPathResolver(projectRoot, projectId)

    override fun handle(path: String): WebResourceResponse? {
        val resource = pathResolver.resolve(path) ?: return null

        val extension = resource.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "js" -> "text/javascript"
                "svg" -> "image/svg+xml"
                "woff" -> "font/woff"
                "woff2" -> "font/woff2"
                else -> "application/octet-stream"
            }
        return WebResourceResponse(mimeType, "UTF-8", FileInputStream(resource))
    }
}