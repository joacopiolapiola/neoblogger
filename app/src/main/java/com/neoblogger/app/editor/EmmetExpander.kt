package com.neoblogger.app.editor

object EmmetExpander {

    fun expand(abbreviation: String, isCss: Boolean = false): String? {
        val trimmed = abbreviation.trim()
        if (trimmed.isEmpty()) return null

        if (!isCss && (trimmed == "!" || trimmed == "doc")) {
            return """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Document</title>
</head>
<body>

</body>
</html>"""
        }

        if (isCss) {
            return expandCss(trimmed)
        }

        return runCatching { parseAndRenderEmmet(trimmed) }.getOrNull()
    }

    private fun expandCss(abbr: String): String? {
        return when {
            abbr == "df" || abbr == "flex" -> "display: flex;"
            abbr == "db" -> "display: block;"
            abbr == "dib" -> "display: inline-block;"
            abbr == "dn" -> "display: none;"
            abbr == "dg" -> "display: grid;"
            abbr == "posa" -> "position: absolute;"
            abbr == "posr" -> "position: relative;"
            abbr == "posf" -> "position: fixed;"
            abbr == "tac" -> "text-align: center;"
            abbr == "tal" -> "text-align: left;"
            abbr == "tar" -> "text-align: right;"
            abbr == "m0a" || abbr == "m0-auto" -> "margin: 0 auto;"
            abbr.startsWith("w") -> parseCssLength("width", abbr.substring(1))
            abbr.startsWith("h") -> parseCssLength("height", abbr.substring(1))
            abbr.startsWith("m") -> parseCssLength("margin", abbr.substring(1))
            abbr.startsWith("p") -> parseCssLength("padding", abbr.substring(1))
            abbr.startsWith("fz") -> parseCssLength("font-size", abbr.substring(2))
            abbr.startsWith("bg#") -> "background: #${abbr.substring(3)};"
            abbr.startsWith("c#") -> "color: #${abbr.substring(2)};"
            abbr.startsWith("bd") -> "border: ${abbr.substring(2)}px solid #000;"
            abbr.startsWith("bdr") -> parseCssLength("border-radius", abbr.substring(3))
            else -> null
        }
    }

    private fun parseCssLength(prop: String, valuePart: String): String? {
        if (valuePart.isEmpty()) return null
        return if (valuePart.endsWith("p")) {
            val num = valuePart.dropLast(1)
            "$prop: ${num}%;"
        } else if (valuePart.endsWith("rem") || valuePart.endsWith("em") || valuePart.endsWith("px") || valuePart.endsWith("vh") || valuePart.endsWith("vw")) {
            "$prop: $valuePart;"
        } else if (valuePart.all { it.isDigit() || it == '.' }) {
            "$prop: ${valuePart}px;"
        } else null
    }

    // Official Emmet Aliases Map
    private val HTML_ALIASES = mapOf(
        "link:css" to "<link rel=\"stylesheet\" href=\"style.css\">",
        "link:favicon" to "<link rel=\"shortcut icon\" href=\"favicon.ico\" type=\"image/x-icon\">",
        "script:src" to "<script src=\"script.js\"></script>",
        "a:link" to "<a href=\"http://\"></a>",
        "a:mail" to "<a href=\"mailto:\"></a>",
        "input:t" to "<input type=\"text\" name=\"\" id=\"\">",
        "input:text" to "<input type=\"text\" name=\"\" id=\"\">",
        "input:p" to "<input type=\"password\" name=\"\" id=\"\">",
        "input:password" to "<input type=\"password\" name=\"\" id=\"\">",
        "input:m" to "<input type=\"email\" name=\"\" id=\"\">",
        "input:email" to "<input type=\"email\" name=\"\" id=\"\">",
        "input:b" to "<input type=\"button\" value=\"\">",
        "input:button" to "<input type=\"button\" value=\"\">",
        "input:s" to "<input type=\"submit\" value=\"\">",
        "input:submit" to "<input type=\"submit\" value=\"\">",
        "input:c" to "<input type=\"checkbox\" name=\"\" id=\"\">",
        "input:r" to "<input type=\"radio\" name=\"\" id=\"\">",
        "input:f" to "<input type=\"file\" name=\"\" id=\"\">",
        "btn" to "<button></button>",
        "btn:s" to "<button type=\"submit\"></button>",
        "btn:b" to "<button type=\"button\"></button>",
        "form:get" to "<form action=\"\" method=\"get\"></form>",
        "form:post" to "<form action=\"\" method=\"post\"></form>"
    )

    private fun parseAndRenderEmmet(abbr: String): String? {
        val lower = abbr.lowercase().trim()
        if (HTML_ALIASES.containsKey(lower)) {
            return HTML_ALIASES[lower]
        }

        val rootNode = EmmetParser(abbr).parse() ?: return null
        val sb = StringBuilder()
        renderEmmetTree(rootNode, sb, 0, parentTag = "")
        return sb.toString().trimEnd()
    }

    private class EmmetElement(
        var tagName: String = "",
        var id: String = "",
        val classes: MutableList<String> = mutableListOf(),
        val attributes: MutableMap<String, String> = mutableMapOf(),
        var textContent: String = "",
        var multiplier: Int = 1,
        val children: MutableList<EmmetElement> = mutableListOf()
    )

    private class EmmetParser(private val input: String) {
        private var pos = 0

        fun parse(): EmmetElement? {
            val root = EmmetElement(tagName = "root")
            var currentParent = root
            val parentStack = mutableListOf<EmmetElement>()
            parentStack.add(root)

            while (pos < input.length) {
                val ch = input[pos]
                when (ch) {
                    '>' -> {
                        pos++
                        val lastChild = currentParent.children.lastOrNull()
                        if (lastChild != null) {
                            parentStack.add(lastChild)
                            currentParent = lastChild
                        }
                    }
                    '+' -> {
                        pos++
                    }
                    '^' -> {
                        pos++
                        if (parentStack.size > 1) {
                            parentStack.removeAt(parentStack.size - 1)
                            currentParent = parentStack.last()
                        }
                    }
                    '(' -> {
                        pos++
                        val groupTree = parseGroup()
                        if (groupTree != null) {
                            currentParent.children.addAll(groupTree)
                        }
                    }
                    else -> {
                        val el = parseSingleElement() ?: break
                        currentParent.children.add(el)
                    }
                }
            }

            return if (root.children.isNotEmpty()) root else null
        }

        private fun parseGroup(): List<EmmetElement>? {
            val startPos = pos
            var depth = 1
            while (pos < input.length) {
                val c = input[pos]
                if (c == '(') depth++
                else if (c == ')') {
                    depth--
                    if (depth == 0) {
                        val groupExpr = input.substring(startPos, pos)
                        pos++ // skip ')'
                        var mult = 1
                        if (pos < input.length && input[pos] == '*') {
                            pos++ // skip '*'
                            val numSb = StringBuilder()
                            while (pos < input.length && input[pos].isDigit()) {
                                numSb.append(input[pos])
                                pos++
                            }
                            mult = numSb.toString().toIntOrNull() ?: 1
                        }

                        val parsedGroup = EmmetParser(groupExpr).parse() ?: return null
                        val resultList = mutableListOf<EmmetElement>()
                        for (i in 0 until mult) {
                            for (child in parsedGroup.children) {
                                resultList.add(cloneElementWithNumbering(child, i + 1))
                            }
                        }
                        return resultList
                    }
                }
                pos++
            }
            return null
        }

        private fun parseSingleElement(): EmmetElement? {
            val el = EmmetElement()
            val tagSb = StringBuilder()

            // Parse tag name
            while (pos < input.length) {
                val c = input[pos]
                if (c in setOf('#', '.', '[', '{', '*', '>', '+', '^', '(', ')')) break
                tagSb.append(c)
                pos++
            }
            el.tagName = tagSb.toString()

            // Parse modifiers (#id, .class, [attr], {text})
            while (pos < input.length) {
                val c = input[pos]
                when (c) {
                    '#' -> {
                        pos++
                        val idSb = StringBuilder()
                        while (pos < input.length && input[pos] !in setOf('#', '.', '[', '{', '*', '>', '+', '^', '(', ')')) {
                            idSb.append(input[pos])
                            pos++
                        }
                        el.id = idSb.toString()
                    }
                    '.' -> {
                        pos++
                        val clsSb = StringBuilder()
                        while (pos < input.length && input[pos] !in setOf('#', '.', '[', '{', '*', '>', '+', '^', '(', ')')) {
                            clsSb.append(input[pos])
                            pos++
                        }
                        if (clsSb.isNotEmpty()) el.classes.add(clsSb.toString())
                    }
                    '[' -> {
                        pos++
                        val attrSb = StringBuilder()
                        while (pos < input.length && input[pos] != ']') {
                            attrSb.append(input[pos])
                            pos++
                        }
                        if (pos < input.length && input[pos] == ']') pos++

                        val attrStr = attrSb.toString()
                        val attrPair = attrStr.split('=', limit = 2)
                        if (attrPair.size == 2) {
                            var v = attrPair[1].trim()
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length - 1)
                            }
                            el.attributes[attrPair[0].trim()] = v
                        } else {
                            el.attributes[attrStr.trim()] = ""
                        }
                    }
                    '{' -> {
                        pos++
                        val txtSb = StringBuilder()
                        while (pos < input.length && input[pos] != '}') {
                            txtSb.append(input[pos])
                            pos++
                        }
                        if (pos < input.length && input[pos] == '}') pos++
                        el.textContent = txtSb.toString()
                    }
                    '*' -> {
                        pos++
                        val numSb = StringBuilder()
                        while (pos < input.length && input[pos].isDigit()) {
                            numSb.append(input[pos])
                            pos++
                        }
                        el.multiplier = numSb.toString().toIntOrNull() ?: 1
                    }
                    else -> break
                }
            }

            return if (el.tagName.isNotEmpty() || el.id.isNotEmpty() || el.classes.isNotEmpty() || el.attributes.isNotEmpty() || el.textContent.isNotEmpty()) {
                el
            } else null
        }

        private fun cloneElementWithNumbering(element: EmmetElement, index: Int): EmmetElement {
            val clone = EmmetElement(
                tagName = element.tagName,
                id = element.id.replace("$", index.toString()),
                classes = element.classes.map { it.replace("$", index.toString()) }.toMutableList(),
                attributes = element.attributes.entries.associate { (k, v) -> k to v.replace("$", index.toString()) }.toMutableMap(),
                textContent = element.textContent.replace("$", index.toString()),
                multiplier = 1
            )
            for (child in element.children) {
                clone.children.add(cloneElementWithNumbering(child, index))
            }
            return clone
        }
    }

    private fun renderEmmetTree(parent: EmmetElement, sb: StringBuilder, indentLevel: Int, parentTag: String) {
        val indent = "  ".repeat(indentLevel)

        for (child in parent.children) {
            val mult = child.multiplier
            for (i in 0 until mult) {
                val idxNum = (i + 1).toString()
                val tag = child.tagName.ifEmpty {
                    when (parentTag.lowercase()) {
                        "ul", "ol" -> "li"
                        "table", "tbody", "thead" -> "tr"
                        "tr" -> "td"
                        "select" -> "option"
                        else -> "div"
                    }
                }.lowercase().replace("$", idxNum)

                val id = child.id.replace("$", idxNum)
                val classes = child.classes.map { it.replace("$", idxNum) }
                val attrsMap = child.attributes.entries.associate { (k, v) -> k to v.replace("$", idxNum) }
                val text = child.textContent.replace("$", idxNum)

                val attrsStr = StringBuilder()
                if (id.isNotEmpty()) attrsStr.append(" id=\"$id\"")
                if (classes.isNotEmpty()) attrsStr.append(" class=\"${classes.joinToString(" ")}\"")
                if (tag == "a" && !attrsMap.containsKey("href")) attrsStr.append(" href=\"\"")
                for ((k, v) in attrsMap) {
                    attrsStr.append(" $k=\"$v\"")
                }

                val selfClosingTags = setOf("img", "input", "br", "hr", "meta", "link", "source", "embed")
                val isSelfClosing = selfClosingTags.contains(tag)

                if (indentLevel > 0 || sb.isNotEmpty()) sb.append(indent)

                if (isSelfClosing) {
                    sb.append("<$tag$attrsStr>\n")
                } else {
                    sb.append("<$tag$attrsStr>")
                    if (child.children.isNotEmpty()) {
                        sb.append("\n")
                        renderEmmetTree(child, sb, indentLevel + 1, parentTag = tag)
                        sb.append(indent)
                    } else if (text.isNotEmpty()) {
                        sb.append(text)
                    }
                    sb.append("</$tag>\n")
                }
            }
        }
    }
}