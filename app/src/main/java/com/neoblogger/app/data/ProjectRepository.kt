package com.neoblogger.app.data

import com.neoblogger.app.storage.ZipUtil
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class ProjectRepository(private val projectsDirectory: File) {

    fun listProjects(): List<WebsiteProject> {
        return projectsDirectory.listFiles()
            .orEmpty()
            .filter { it.isDirectory && isProjectId(it.name) }
            .mapNotNull { directory ->
                val nameFile = File(directory, PROJECT_NAME_FILE)
                if (!File(directory, ENTRY_FILE).isFile || !nameFile.isFile) return@mapNotNull null
                WebsiteProject(
                    id = directory.name,
                    name = nameFile.readText(Charsets.UTF_8).trim(),
                    lastOpenedAt = directory.lastModified(),
                )
            }
            .sortedByDescending { it.lastOpenedAt }
    }

    fun createProject(name: String): WebsiteProject {
        val cleanName = validateProjectName(name)

        check(projectsDirectory.exists() || projectsDirectory.mkdirs()) {
            "Could not create the projects folder."
        }

        val project = WebsiteProject(UUID.randomUUID().toString(), cleanName)
        val directory = projectDirectory(project.id)
        check(directory.mkdir()) { "Could not create the project folder." }

        try {
            File(directory, PROJECT_NAME_FILE).writeText(cleanName, Charsets.UTF_8)
            writeAtomically(File(directory, ENTRY_FILE), starterHtml)
            writeAtomically(File(directory, STYLES_FILE), starterCss)
            writeAtomically(File(directory, SCRIPT_FILE), starterJs)
        } catch (error: Exception) {
            directory.deleteRecursively()
            throw error
        }
        return project
    }

    fun deleteProject(projectId: String) {
        val directory = projectDirectory(projectId)
        if (directory.exists()) {
            directory.deleteRecursively()
        }
    }

    fun importProjectFromZip(zipInputStream: InputStream, suggestedName: String): WebsiteProject {
        val cleanName = validateProjectName(suggestedName.ifBlank { "Imported Site" })
        val project = WebsiteProject(UUID.randomUUID().toString(), cleanName)
        val directory = projectDirectory(project.id)
        check(directory.mkdirs()) { "Could not create destination directory for zip import." }

        try {
            ZipUtil.unzip(zipInputStream, directory)

            val entryFile = File(directory, ENTRY_FILE)
            if (!entryFile.exists()) {
                val anyHtml = directory.walkTopDown().firstOrNull { it.isFile && it.extension.lowercase() == "html" }
                if (anyHtml != null) {
                    anyHtml.copyTo(entryFile, overwrite = true)
                } else {
                    writeAtomically(entryFile, starterHtml)
                }
            }

            File(directory, PROJECT_NAME_FILE).writeText(cleanName, Charsets.UTF_8)
            return project
        } catch (e: Exception) {
            directory.deleteRecursively()
            throw e
        }
    }

    fun importProjectFromFolder(sourceFolder: File, suggestedName: String): WebsiteProject {
        require(sourceFolder.isDirectory) { "Source folder must be a directory." }
        val cleanName = validateProjectName(suggestedName.ifBlank { sourceFolder.name })
        val project = WebsiteProject(UUID.randomUUID().toString(), cleanName)
        val directory = projectDirectory(project.id)
        check(directory.mkdirs()) { "Could not create destination directory for folder import." }

        try {
            sourceFolder.copyRecursively(directory, overwrite = true)
            File(directory, PROJECT_NAME_FILE).writeText(cleanName, Charsets.UTF_8)

            val entryFile = File(directory, ENTRY_FILE)
            if (!entryFile.exists()) {
                val anyHtml = directory.walkTopDown().firstOrNull { it.isFile && it.extension.lowercase() == "html" }
                if (anyHtml != null) {
                    anyHtml.copyTo(entryFile, overwrite = true)
                } else {
                    writeAtomically(entryFile, starterHtml)
                }
            }
            return project
        } catch (e: Exception) {
            directory.deleteRecursively()
            throw e
        }
    }

    fun exportProjectToZip(projectId: String, outputStream: OutputStream) {
        val directory = projectDirectory(projectId)
        check(directory.isDirectory) { "Project directory not found." }
        ZipUtil.zipDirectory(directory, outputStream)
    }

    fun listProjectFiles(projectId: String): List<ProjectFile> {
        val root = projectDirectory(projectId)
        if (!root.isDirectory) return emptyList()

        val rootPath = root.toPath()
        return root.walkTopDown()
            .filter { it != root && it.name != PROJECT_NAME_FILE && !it.name.startsWith(".") }
            .map { file ->
                val relative = rootPath.relativize(file.toPath()).toString().replace('\\', '/')
                ProjectFile(
                    relativePath = relative,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isFile) file.length() else 0,
                )
            }
            .sortedWith(compareBy({ !it.isDirectory }, { it.relativePath.lowercase() }))
            .toList()
    }

    fun readFile(projectId: String, relativePath: String): String {
        val file = resolveSafeFile(projectId, relativePath)
        check(file.isFile) { "The file $relativePath does not exist." }
        return file.readText(Charsets.UTF_8)
    }

    fun saveFile(projectId: String, relativePath: String, contents: String) {
        require(contents.toByteArray(Charsets.UTF_8).size <= MAX_SOURCE_BYTES) {
            "File exceeds maximum size limit."
        }
        val file = resolveSafeFile(projectId, relativePath)
        file.parentFile?.mkdirs()
        writeAtomically(file, contents)
    }

    fun createProjectFile(projectId: String, relativePath: String, initialContents: String) {
        var contentsToSave = initialContents
        val cleanPath = relativePath.trim()

        if (cleanPath.endsWith(".html", ignoreCase = true)) {
            val baseName = cleanPath.substringBeforeLast(".html")
            val cssFileName = "$baseName.css"

            val cssFile = resolveSafeFile(projectId, cssFileName)
            if (!cssFile.exists()) {
                val starterCssContent = "/* Styles for $cleanPath */\nbody {\n  font-family: system-ui, sans-serif;\n}\n"
                saveFile(projectId, cssFileName, starterCssContent)
            }

            if (contentsToSave.isBlank() || !contentsToSave.contains("<html")) {
                val capitalizedTitle = baseName.replaceFirstChar { it.uppercase() }
                contentsToSave = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>$capitalizedTitle</title>
  <link rel="stylesheet" href="$cssFileName">
</head>
<body>
  <main>
    <h1>$capitalizedTitle</h1>
  </main>
</body>
</html>"""
            } else if (!contentsToSave.contains(cssFileName)) {
                val linkTag = "  <link rel=\"stylesheet\" href=\"$cssFileName\">\n</head>"
                contentsToSave = contentsToSave.replace("</head>", linkTag)
            }
        }

        saveFile(projectId, cleanPath, contentsToSave)
    }

    fun importFontToProject(projectId: String, fontUri: android.net.Uri, context: android.content.Context): String {
        var rawFileName = "custom_font_" + UUID.randomUUID().toString().take(8) + ".ttf"
        runCatching {
            context.contentResolver.query(fontUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        rawFileName = displayName
                    }
                }
            }
        }

        val cleanFileName = rawFileName.replace('\\', '/').substringAfterLast('/').trim()
        val fontName = cleanFileName.substringBeforeLast('.').replace('-', ' ').replace('_', ' ').replaceFirstChar { it.uppercase() }

        val fontsDir = File(projectDirectory(projectId), "assets/fonts")
        fontsDir.mkdirs()

        val destFontFile = File(fontsDir, cleanFileName)
        context.contentResolver.openInputStream(fontUri)?.use { input ->
            destFontFile.outputStream().use { out -> input.copyTo(out) }
        }

        val styleFile = resolveSafeFile(projectId, "style.css")
        if (styleFile.exists()) {
            ensureFontFaceInCss(projectId, "style.css", fontName)
        }

        return fontName
    }

    fun getImportedFontNames(projectId: String): List<String> {
        val fontsDir = File(projectDirectory(projectId), "assets/fonts")
        if (!fontsDir.isDirectory) return emptyList()
        return fontsDir.listFiles().orEmpty()
            .filter { it.isFile && (it.extension.equals("ttf", true) || it.extension.equals("otf", true) || it.extension.equals("woff", true) || it.extension.equals("woff2", true)) }
            .map { it.nameWithoutExtension.replace('-', ' ').replace('_', ' ').replaceFirstChar { c -> c.uppercase() } }
            .distinct()
    }

    fun ensureFontFaceInCss(projectId: String, cssFileName: String, fontName: String) {
        val fontsDir = File(projectDirectory(projectId), "assets/fonts")
        if (!fontsDir.isDirectory) return

        val matchingFontFile = fontsDir.listFiles().orEmpty().firstOrNull {
            it.isFile && it.nameWithoutExtension.replace('-', ' ').replace('_', ' ').equals(fontName, ignoreCase = true)
        } ?: return

        val relativeFontPath = "assets/fonts/${matchingFontFile.name}"
        val targetCssName = if (cssFileName.isBlank()) "style.css" else cssFileName
        val targetCssFile = resolveSafeFile(projectId, targetCssName)

        val existingCss = if (targetCssFile.exists()) targetCssFile.readText(Charsets.UTF_8) else ""
        if (!existingCss.contains(fontName, ignoreCase = true)) {
            val fontFaceCss = """

@font-face {
  font-family: '$fontName';
  src: url('$relativeFontPath');
}
"""
            saveFile(projectId, targetCssName, existingCss + fontFaceCss)
        }
    }

    fun deleteProjectFile(projectId: String, relativePath: String) {
        val file = resolveSafeFile(projectId, relativePath)
        if (file.exists()) {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }
    }

    fun addProjectAsset(projectId: String, fileName: String, inputStream: InputStream) {
        val cleanName = fileName.replace('\\', '/').substringAfterLast('/')
        require(cleanName.isNotBlank() && !cleanName.contains("..")) { "Invalid asset name." }

        val assetsDir = File(projectDirectory(projectId), "assets")
        assetsDir.mkdirs()
        val destFile = File(assetsDir, cleanName)

        val temp = File(assetsDir, "$cleanName.tmp")
        temp.outputStream().use { out -> inputStream.copyTo(out) }
        Files.move(temp.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    fun copyAssetToProject(projectId: String, sourceUri: android.net.Uri, context: android.content.Context): String {
        var fileName = "media_" + UUID.randomUUID().toString().take(8)
        runCatching {
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        fileName = displayName
                    }
                }
            }
        }

        val cleanName = fileName.replace('\\', '/').substringAfterLast('/').trim().ifBlank { "media_file" }
        val assetsDir = File(projectDirectory(projectId), "assets")
        assetsDir.mkdirs()

        val destFile = File(assetsDir, cleanName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { out ->
                input.copyTo(out)
            }
        }
        return "assets/$cleanName"
    }

    fun projectRoot(projectId: String): File = projectDirectory(projectId)

    private fun resolveSafeFile(projectId: String, relativePath: String): File {
        require(!relativePath.contains('\\') && !relativePath.startsWith('/')) { "Invalid file path." }
        val segments = relativePath.split('/')
        require(segments.none { it.isEmpty() || it == "." || it == ".." }) { "Invalid path navigation." }

        val root = projectDirectory(projectId).canonicalFile
        val target = File(root, relativePath).canonicalFile
        require(target.path.startsWith(root.path + File.separator) || target.path == root.path) {
            "Path traversal attempt blocked."
        }
        return target
    }

    private fun validateProjectName(name: String): String {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Project name cannot be empty." }
        require(cleanName.length <= MAX_NAME_LENGTH) { "Project name is too long." }
        require(cleanName.none { it.isISOControl() }) { "Project name contains unsupported characters." }
        return cleanName
    }

    private fun projectDirectory(projectId: String): File {
        require(isProjectId(projectId)) { "Invalid project identifier." }
        val root = projectsDirectory.canonicalFile
        val target = File(root, projectId).canonicalFile
        require(target.parentFile == root) { "Project path is outside workspace." }
        return target
    }

    private fun isProjectId(value: String): Boolean {
        return runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
    }

    private fun writeAtomically(destination: File, contents: String) {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "${destination.name}.tmp")
        temporary.writeText(contents, Charsets.UTF_8)
        try {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            temporary.delete()
        }
    }

    companion object {
        const val PROJECT_NAME_FILE = "project-name.txt"
        const val ENTRY_FILE = "index.html"
        const val STYLES_FILE = "style.css"
        const val SCRIPT_FILE = "script.js"
        const val MAX_NAME_LENGTH = 60
        const val MAX_SOURCE_BYTES = 2_000_000

        val starterHtml = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>My new site</title>
  <link rel="stylesheet" href="style.css">
  <script src="script.js" defer></script>
</head>
<body>
  <main>
    <p class="eyebrow">A new beginning</p>
    <h1>My little corner of the web</h1>
    <p>Edit this page in Code or Design, then test it in Preview.</p>
    <button id="hello-button" type="button">Say hello</button>
    <p id="greeting" aria-live="polite"></p>
  </main>
</body>
</html>
"""

        val starterCss = """* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 2rem;
  color: #202a26;
  background: #f2f5ed;
  font: 1rem/1.6 system-ui, sans-serif;
}
main { width: min(100%, 42rem); }
.eyebrow { color: #557768; text-transform: uppercase; letter-spacing: .12em; }
h1 { max-width: 12ch; font: 600 clamp(2.5rem, 8vw, 5rem)/.98 Georgia, serif; }
button { padding: .8rem 1.1rem; border: 0; border-radius: .4rem; color: white; background: #315c4c; font: inherit; cursor: pointer; }
"""

        val starterJs = """document.querySelector('#hello-button')?.addEventListener('click', () => {
  document.querySelector('#greeting').textContent = 'Hello from your local site.';
});
"""
    }
}