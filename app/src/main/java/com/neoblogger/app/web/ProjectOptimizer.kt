package com.neoblogger.app.web

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

data class OptimizationReport(
    val unusedAssetFiles: List<String> = emptyList(),
    val unusedCssRules: List<String> = emptyList(),
    val orphanHtmlPages: List<String> = emptyList(),
    val compressableMediaFiles: List<String> = emptyList(),
    val estimatedBytesSavable: Long = 0L
)

data class OptimizationOptions(
    val purgeUnusedAssets: Boolean = true,
    val cleanUnusedCss: Boolean = true,
    val compressMedia: Boolean = true
)

data class OptimizationSummary(
    val purgedAssetsCount: Int = 0,
    val cleanedCssRulesCount: Int = 0,
    val compressedMediaCount: Int = 0,
    val bytesSaved: Long = 0L,
    val warnings: List<String> = emptyList()
)

object ProjectOptimizer {

    fun analyzeProject(projectDir: File): OptimizationReport {
        if (!projectDir.isDirectory) return OptimizationReport()

        val allFiles = projectDir.walkTopDown()
            .filter { it.isFile && !it.name.startsWith(".") && it.name != "project-name.txt" }
            .toList()

        val htmlFiles = allFiles.filter { it.extension.lowercase() == "html" }
        val cssFiles = allFiles.filter { it.extension.lowercase() == "css" }
        val jsFiles = allFiles.filter { it.extension.lowercase() == "js" }

        val assetsDir = File(projectDir, "assets")
        val assetFiles = if (assetsDir.isDirectory) {
            assetsDir.walkTopDown().filter { it.isFile && !it.name.startsWith(".") }.toList()
        } else emptyList()

        // Read all HTML/CSS/JS text into memory
        val htmlTexts = htmlFiles.associate { projectDir.toPath().relativize(it.toPath()).toString().replace('\\', '/') to it.readText(Charsets.UTF_8) }
        val cssTexts = cssFiles.associate { projectDir.toPath().relativize(it.toPath()).toString().replace('\\', '/') to it.readText(Charsets.UTF_8) }
        val jsTexts = jsFiles.associate { projectDir.toPath().relativize(it.toPath()).toString().replace('\\', '/') to it.readText(Charsets.UTF_8) }

        val combinedCodeText = (htmlTexts.values + cssTexts.values + jsTexts.values).joinToString("\n")

        // 1. Detect Unused Assets
        val unusedAssets = mutableListOf<String>()
        var savableAssetBytes = 0L

        for (asset in assetFiles) {
            val relPath = projectDir.toPath().relativize(asset.toPath()).toString().replace('\\', '/')
            val assetName = asset.name
            if (!combinedCodeText.contains(relPath) && !combinedCodeText.contains(assetName)) {
                unusedAssets.add(relPath)
                savableAssetBytes += asset.length()
            }
        }

        // 2. Detect Orphan HTML Pages (Pages not linked from any other page)
        val orphanPages = mutableListOf<String>()
        val allHtmlLinks = htmlTexts.values.flatMap { text ->
            Regex("href=[\"']([^\"']+\\.html)[\"']", RegexOption.IGNORE_CASE)
                .findAll(text)
                .map { it.groupValues[1].lowercase().trim() }
                .toList()
        }.toSet()

        for ((relPath, _) in htmlTexts) {
            val lowerRel = relPath.lowercase()
            if (lowerRel != "index.html" && !allHtmlLinks.contains(lowerRel) && !allHtmlLinks.contains("./$lowerRel")) {
                orphanPages.add(relPath)
            }
        }

        // 3. Detect Compressable Media
        val compressableMedia = assetFiles.filter { file ->
            val ext = file.extension.lowercase()
            (ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "webp") && file.length() > 50_000
        }.map { projectDir.toPath().relativize(it.toPath()).toString().replace('\\', '/') }

        // 4. Detect Unused CSS Rules
        val unusedCssSelectors = mutableListOf<String>()
        val combinedHtml = htmlTexts.values.joinToString("\n")

        for ((_, cssContent) in cssTexts) {
            val selectorMatches = Regex("([.#][a-zA-Z0-9_-]+)\\s*\\{").findAll(cssContent)
            for (match in selectorMatches) {
                val selector = match.groupValues[1]
                val nameWithoutPrefix = selector.drop(1)
                if (selector.startsWith(".")) {
                    if (!combinedHtml.contains("class=\"$nameWithoutPrefix\"") && !combinedHtml.contains("class='$nameWithoutPrefix'") && !combinedHtml.contains(nameWithoutPrefix)) {
                        unusedCssSelectors.add(selector)
                    }
                } else if (selector.startsWith("#")) {
                    if (!combinedHtml.contains("id=\"$nameWithoutPrefix\"") && !combinedHtml.contains("id='$nameWithoutPrefix'") && !combinedHtml.contains(nameWithoutPrefix)) {
                        unusedCssSelectors.add(selector)
                    }
                }
            }
        }

        return OptimizationReport(
            unusedAssetFiles = unusedAssets,
            unusedCssRules = unusedCssSelectors.distinct(),
            orphanHtmlPages = orphanPages,
            compressableMediaFiles = compressableMedia,
            estimatedBytesSavable = savableAssetBytes
        )
    }

    fun optimizeProject(
        projectDir: File,
        options: OptimizationOptions,
        onProgress: (String) -> Unit
    ): OptimizationSummary {
        val report = analyzeProject(projectDir)
        var totalBytesSaved = 0L
        var purgedCount = 0
        var compressedCount = 0
        var cleanedCssCount = 0
        val warningList = mutableListOf<String>()

        if (report.orphanHtmlPages.isNotEmpty()) {
            for (orphan in report.orphanHtmlPages) {
                warningList.add("⚠️ '$orphan' is an orphan page (not linked from index.html or any other page). Only accessible via direct URL.")
            }
        }

        // 1. Purge Unused Assets
        if (options.purgeUnusedAssets && report.unusedAssetFiles.isNotEmpty()) {
            for (relAsset in report.unusedAssetFiles) {
                val file = File(projectDir, relAsset)
                if (file.exists()) {
                    val size = file.length()
                    if (file.delete()) {
                        purgedCount++
                        totalBytesSaved += size
                        onProgress("Deleted unused asset: $relAsset")
                    }
                }
            }
        }

        // 2. Compress Media Assets Losslessly
        if (options.compressMedia && report.compressableMediaFiles.isNotEmpty()) {
            for (relMedia in report.compressableMediaFiles) {
                val file = File(projectDir, relMedia)
                if (file.isFile) {
                    val originalSize = file.length()
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val tempFile = File(file.parentFile, "${file.name}.tmp")
                        FileOutputStream(tempFile).use { out ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                            } else {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        }
                        bitmap.recycle()

                        if (tempFile.exists() && tempFile.length() < originalSize && tempFile.length() > 0) {
                            val saved = originalSize - tempFile.length()
                            tempFile.copyTo(file, overwrite = true)
                            tempFile.delete()
                            compressedCount++
                            totalBytesSaved += saved
                            onProgress("Compressed media $relMedia (saved ${saved / 1024} KB)")
                        } else {
                            tempFile.delete()
                        }
                    }
                }
            }
        }

        // 3. Clean Unused CSS Rules
        if (options.cleanUnusedCss && report.unusedCssRules.isNotEmpty()) {
            val cssFiles = projectDir.walkTopDown().filter { it.isFile && it.extension.lowercase() == "css" }
            for (cssFile in cssFiles) {
                var cssText = cssFile.readText(Charsets.UTF_8)
                val origLen = cssText.length
                for (rule in report.unusedCssRules) {
                    val ruleRegex = Regex(Regex.escape(rule) + "\\s*\\{[^}]*\\}", RegexOption.IGNORE_CASE)
                    cssText = ruleRegex.replace(cssText, "")
                }
                if (cssText.length < origLen) {
                    val saved = (origLen - cssText.length).toLong()
                    cssFile.writeText(cssText, Charsets.UTF_8)
                    cleanedCssCount += report.unusedCssRules.size
                    totalBytesSaved += saved
                    onProgress("Cleaned stylesheet ${cssFile.name}")
                }
            }
        }

        return OptimizationSummary(
            purgedAssetsCount = purgedCount,
            cleanedCssRulesCount = cleanedCssCount,
            compressedMediaCount = compressedCount,
            bytesSaved = totalBytesSaved,
            warnings = warningList
        )
    }
}