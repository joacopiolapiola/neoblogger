package com.neoblogger.app.design

import java.util.UUID

class DesignDocument(
    var doctype: String = "<!doctype html>",
    var headHtml: String = "",
    val bodyNode: DesignNode = DesignNode(nodeId = "body-root", tagName = "body"),
    var scriptsAndFooterHtml: String = "",
) {
    fun toHtml(includeDesignAttributes: Boolean = false): String {
        val sb = StringBuilder()
        if (doctype.isNotEmpty()) sb.append(doctype).append("\n")
        sb.append("<html lang=\"en\">\n")
        if (headHtml.isNotEmpty()) {
            sb.append("<head>\n").append(headHtml.trimIndent()).append("\n</head>\n")
        }
        sb.append("<body>\n")
        sb.append(renderNodeChildren(bodyNode, 1, includeDesignAttributes))
        if (scriptsAndFooterHtml.isNotEmpty()) {
            sb.append(scriptsAndFooterHtml.trimIndent()).append("\n")
        }
        sb.append("</body>\n</html>\n")
        return sb.toString()
    }

    private fun renderNodeChildren(parent: DesignNode, indentLevel: Int, includeDesignAttributes: Boolean = false): String {
        val sb = StringBuilder()
        val indent = "  ".repeat(indentLevel)

        for (child in parent.children) {
            if (child.isRawTextNode) {
                sb.append(indent).append(child.rawTextContent).append("\n")
                continue
            }

            val attrsMap = child.attributes.toMutableMap()
            if (includeDesignAttributes) {
                attrsMap["data-nb-id"] = child.nodeId
            } else {
                attrsMap.remove("data-nb-id")
            }

            val attrsStr = if (attrsMap.isNotEmpty()) {
                " " + attrsMap.entries.joinToString(" ") { (k, v) -> if (v.isEmpty()) k else "$k=\"$v\"" }
            } else ""

            if (child.isSelfClosing) {
                sb.append(indent).append("<${child.tagName}$attrsStr>\n")
            } else {
                sb.append(indent).append("<${child.tagName}$attrsStr>")
                if (child.children.isNotEmpty()) {
                    sb.append("\n")
                    sb.append(renderNodeChildren(child, indentLevel + 1, includeDesignAttributes))
                    sb.append(indent)
                } else if (child.textContent.isNotEmpty()) {
                    sb.append(child.textContent)
                }
                sb.append("</${child.tagName}>\n")
            }
        }
        return sb.toString()
    }
}

object HtmlDomParser {

    private val selfClosingTags = setOf("img", "input", "br", "hr", "meta", "link", "source", "embed")

    fun parse(html: String): DesignDocument {
        val doc = DesignDocument()

        if (html.isBlank()) {
            populateDefaultBody(doc.bodyNode)
            return doc
        }

        val doctypeMatch = Regex("(?i)<!doctype[^>]*>", RegexOption.IGNORE_CASE).find(html)
        if (doctypeMatch != null) {
            doc.doctype = doctypeMatch.value
        }

        val headMatch = Regex("(?i)<head[^>]*>(.*?)</head>", RegexOption.DOT_MATCHES_ALL).find(html)
        if (headMatch != null) {
            doc.headHtml = headMatch.groupValues[1].trim()
        }

        val bodyMatch = Regex("(?i)<body[^>]*>(.*?)</body>", RegexOption.DOT_MATCHES_ALL).find(html)
        val bodyContent = if (bodyMatch != null) bodyMatch.groupValues[1] else html

        val styleRegex = Regex("(?i)<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL)
        val scriptRegex = Regex("(?i)<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL)

        val stylesInBody = styleRegex.findAll(bodyContent).map { it.value }.toList()
        val scriptsInBody = scriptRegex.findAll(bodyContent).map { it.value }.toList()

        if (stylesInBody.isNotEmpty()) {
            val existingHead = doc.headHtml
            val newStyles = stylesInBody.joinToString("\n")
            doc.headHtml = if (existingHead.isBlank()) newStyles else "$existingHead\n$newStyles"
        }

        if (scriptsInBody.isNotEmpty()) {
            val existingScripts = doc.scriptsAndFooterHtml
            val newScripts = scriptsInBody.joinToString("\n")
            doc.scriptsAndFooterHtml = if (existingScripts.isBlank()) newScripts else "$existingScripts\n$newScripts"
        }

        var cleanBodyContent = styleRegex.replace(bodyContent, "")
        cleanBodyContent = scriptRegex.replace(cleanBodyContent, "")

        parseBodyContentIntoNode(cleanBodyContent, doc.bodyNode)

        if (doc.bodyNode.children.isEmpty()) {
            populateDefaultBody(doc.bodyNode)
        }

        return doc
    }

    private fun populateDefaultBody(body: DesignNode) {
        val main = createNode("main")
        val h1 = createNode("h1", textContent = "Welcome to Neoblogger")
        val p = createNode("p", textContent = "Start editing visually in Design or directly in Code.")
        val btn = createNode("button", textContent = "Click me")
        btn.attributes["type"] = "button"

        main.children.add(h1)
        main.children.add(p)
        main.children.add(btn)
        body.children.add(main)
    }

    private fun parseBodyContentIntoNode(content: String, root: DesignNode) {
        val tagRegex = Regex("<(/?[a-zA-Z0-9-]+)([^>]*)>|([^<]+)")
        val stack = mutableListOf<DesignNode>()
        stack.add(root)

        val matches = tagRegex.findAll(content)
        for (match in matches) {
            val fullMatch = match.value
            val tagName = match.groupValues[1]
            val attrString = match.groupValues[2]
            val textPart = match.groupValues[3]

            if (textPart.isNotEmpty()) {
                val trimmedText = textPart.trim()
                if (trimmedText.isNotEmpty()) {
                    val currentParent = stack.last()
                    if (currentParent.children.isEmpty() && !currentParent.isSelfClosing && currentParent != root) {
                        currentParent.textContent = trimmedText
                    } else {
                        currentParent.children.add(
                            DesignNode(
                                nodeId = generateNodeId(),
                                tagName = "text",
                                isRawTextNode = true,
                                rawTextContent = trimmedText,
                            )
                        )
                    }
                }
            } else if (tagName.isNotEmpty()) {
                if (tagName.startsWith("/")) {
                    val closingTag = tagName.substring(1).lowercase()
                    if (stack.size > 1 && stack.last().tagName.equals(closingTag, ignoreCase = true)) {
                        stack.removeAt(stack.size - 1)
                    }
                } else {
                    val cleanTag = tagName.lowercase()
                    if (cleanTag == "script" || cleanTag == "style") {
                        val currentParent = stack.last()
                        currentParent.children.add(
                            DesignNode(
                                nodeId = generateNodeId(),
                                tagName = cleanTag,
                                isRawTextNode = true,
                                rawTextContent = fullMatch,
                            )
                        )
                        continue
                    }

                    val isSelfClosing = selfClosingTags.contains(cleanTag) || fullMatch.endsWith("/>")
                    val newNode = createNode(cleanTag, isSelfClosing = isSelfClosing)
                    parseAttributes(attrString, newNode)

                    val currentParent = stack.last()
                    currentParent.children.add(newNode)

                    if (!isSelfClosing) {
                        stack.add(newNode)
                    }
                }
            }
        }
    }

    private fun parseAttributes(attrString: String, node: DesignNode) {
        if (attrString.isBlank()) return
        val attrRegex = Regex("([a-zA-Z0-9-:]+)(?:=(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+)))?")
        for (m in attrRegex.findAll(attrString)) {
            val key = m.groupValues[1]
            val value = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }

            if (key.equals("data-nb-id", ignoreCase = true) && value.isNotEmpty()) {
                node.nodeId = value
            } else {
                node.attributes[key] = value

                if (key.equals("style", ignoreCase = true) && value.isNotEmpty()) {
                    val declarations = value.split(';')
                    for (decl in declarations) {
                        val parts = decl.split(':', limit = 2)
                        if (parts.size == 2) {
                            node.inlineStyles[parts[0].trim().lowercase()] = parts[1].trim()
                        }
                    }
                }
            }
        }
    }

    fun createNode(
        tagName: String,
        textContent: String = "",
        isSelfClosing: Boolean = selfClosingTags.contains(tagName.lowercase()),
    ): DesignNode {
        return DesignNode(
            nodeId = generateNodeId(),
            tagName = tagName.lowercase(),
            textContent = textContent,
            isSelfClosing = isSelfClosing,
        )
    }

    fun generateNodeId(): String = "node-" + UUID.randomUUID().toString().take(8)

    fun hasFixedMobileViewport(headHtml: String): Boolean {
        return headHtml.contains("width=375") || headHtml.contains("width=390") || headHtml.contains("user-scalable=no")
    }

    fun setFixedMobileViewport(doc: DesignDocument, enable: Boolean) {
        val fixedMeta = "<meta name=\"viewport\" content=\"width=375, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">"
        val standardMeta = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"

        val viewportRegex = Regex("(?i)<meta[^>]*name=[\"']viewport[\"'][^>]*>", RegexOption.IGNORE_CASE)

        if (enable) {
            if (viewportRegex.containsMatchIn(doc.headHtml)) {
                doc.headHtml = viewportRegex.replace(doc.headHtml, fixedMeta)
            } else {
                doc.headHtml = "$fixedMeta\n${doc.headHtml}".trim()
            }
        } else {
            if (viewportRegex.containsMatchIn(doc.headHtml)) {
                doc.headHtml = viewportRegex.replace(doc.headHtml, standardMeta)
            } else {
                doc.headHtml = "$standardMeta\n${doc.headHtml}".trim()
            }
        }
    }

    fun findLineNumberInHtml(html: String, node: DesignNode): Int {
        if (html.isBlank()) return 1
        val lines = html.lines()
        val id = node.attributes["id"]
        val className = node.attributes["class"]

        if (!id.isNullOrBlank()) {
            for ((index, line) in lines.withIndex()) {
                if (line.contains("id=\"$id\"") || line.contains("id='$id'")) {
                    return index + 1
                }
            }
        }

        if (!className.isNullOrBlank()) {
            for ((index, line) in lines.withIndex()) {
                if (line.contains("<${node.tagName}") && (line.contains("class=\"$className\"") || line.contains("class='$className'"))) {
                    return index + 1
                }
            }
        }

        if (node.textContent.isNotBlank()) {
            val snippet = node.textContent.trim().take(15)
            for ((index, line) in lines.withIndex()) {
                if (line.contains("<${node.tagName}") || (snippet.length >= 3 && line.contains(snippet))) {
                    return index + 1
                }
            }
        }

        for ((index, line) in lines.withIndex()) {
            if (line.contains("<${node.tagName}")) {
                return index + 1
            }
        }

        return 1
    }

    fun findLineNumberInCss(css: String, selector: String): Int {
        if (css.isBlank()) return 1
        val lines = css.lines()
        for ((index, line) in lines.withIndex()) {
            if (line.contains(selector)) {
                return index + 1
            }
        }
        return 1
    }
}