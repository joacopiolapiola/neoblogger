package com.neoblogger.app.editor

object CodeAutocompleter {

    private val CSS_PROPERTIES = listOf(
        "align-content", "align-items", "align-self", "animation", "background",
        "background-color", "background-image", "background-position", "background-size",
        "border", "border-bottom", "border-color", "border-left", "border-radius",
        "border-right", "border-style", "border-top", "border-width", "bottom",
        "box-shadow", "box-sizing", "color", "cursor", "display", "flex",
        "flex-direction", "flex-wrap", "font-family", "font-size", "font-weight",
        "gap", "grid", "height", "justify-content", "left", "letter-spacing",
        "line-height", "margin", "margin-bottom", "margin-left", "margin-right",
        "margin-top", "max-height", "max-width", "min-height", "min-width",
        "opacity", "overflow", "padding", "padding-bottom", "padding-left",
        "padding-right", "padding-top", "position", "right", "text-align",
        "text-decoration", "text-transform", "top", "transform", "transition",
        "width", "z-index"
    )

    private val CSS_VALUES = listOf(
        "absolute", "auto", "block", "bold", "border-box", "center", "column",
        "contain", "cover", "darkgray", "flex", "flex-end", "flex-start",
        "grid", "hidden", "inline", "inline-block", "inline-flex", "inherit",
        "initial", "left", "none", "no-repeat", "pointer", "relative",
        "repeat", "right", "row", "sans-serif", "space-around", "space-between",
        "space-evenly", "solid", "stretch", "transparent", "uppercase", "visible", "wrap"
    )

    private val JS_KEYWORDS = listOf(
        "addEventListener", "async", "await", "break", "case", "catch", "class",
        "console.log", "console.error", "const", "continue", "debugger", "default",
        "document.getElementById", "document.querySelector", "document.querySelectorAll",
        "else", "export", "extends", "fetch", "for", "function", "if", "import",
        "indexOf", "innerHtml", "innerText", "input", "JSON.parse", "JSON.stringify",
        "let", "map", "new", "null", "preventDefault", "Promise", "push", "return",
        "setInterval", "setTimeout", "stopPropagation", "switch", "this", "throw",
        "try", "typeof", "undefined", "var", "while", "window"
    )

    private val HTML_TAGS = listOf(
        "a", "article", "aside", "audio", "b", "blockquote", "body", "button",
        "canvas", "code", "div", "em", "figcaption", "figure", "footer", "form",
        "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hr", "html",
        "iframe", "img", "input", "label", "li", "link", "main", "meta", "nav",
        "ol", "option", "p", "pre", "script", "section", "select", "span", "strong",
        "style", "table", "tbody", "td", "textarea", "th", "title", "tr", "ul", "video"
    )

    fun getCompletions(currentWord: String, fileName: String): List<String> {
        val trimmed = currentWord.trim().lowercase()
        if (trimmed.length < 2) return emptyList()

        val candidates = when {
            fileName.endsWith(".css") -> CSS_PROPERTIES + CSS_VALUES
            fileName.endsWith(".js") -> JS_KEYWORDS
            else -> HTML_TAGS + CSS_PROPERTIES + JS_KEYWORDS
        }

        return candidates
            .filter { it.lowercase().startsWith(trimmed) && !it.equals(trimmed, ignoreCase = true) }
            .distinct()
            .take(6)
    }
}