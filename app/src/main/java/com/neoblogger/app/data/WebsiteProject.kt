package com.neoblogger.app.data

data class WebsiteProject(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val entryPage: String = "index.html",
)

data class ProjectFile(
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0,
    val extension: String = name.substringAfterLast('.', "").lowercase(),
) {
    val isTextFile: Boolean
        get() = isDirectory.not() && extension in TEXT_EXTENSIONS

    companion object {
        val TEXT_EXTENSIONS = setOf("html", "htm", "css", "js", "mjs", "json", "svg", "md", "txt", "webmanifest", "xml")
        val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico")
    }
}