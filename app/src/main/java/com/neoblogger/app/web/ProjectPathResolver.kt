package com.neoblogger.app.web

import java.io.File

internal class ProjectPathResolver(
    private val projectRoot: File,
    private val projectId: String,
) {
    fun resolve(path: String): File? {
        if (path.contains('\\') || path.startsWith('/')) return null
        val prefix = "$projectId/"
        if (!path.startsWith(prefix)) return null
        val relativePath = path.removePrefix(prefix)
        val segments = relativePath.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) return null

        val canonicalPaths = runCatching {
            val root = projectRoot.canonicalFile
            root to File(root, segments.joinToString(File.separator)).canonicalFile
        }.getOrNull() ?: return null
        val (root, resource) = canonicalPaths
        return resource.takeIf {
            it.path.startsWith(root.path + File.separator) && it.isFile
        }
    }
}