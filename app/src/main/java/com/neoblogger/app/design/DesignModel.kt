package com.neoblogger.app.design

data class DesignNode(
    var nodeId: String,
    var tagName: String,
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val inlineStyles: MutableMap<String, String> = mutableMapOf(),
    var textContent: String = "",
    val children: MutableList<DesignNode> = mutableListOf(),
    val isSelfClosing: Boolean = false,
    val isRawTextNode: Boolean = false,
    val rawTextContent: String = "",
) {
    val elementId: String
        get() = attributes["id"].orEmpty()

    val className: String
        get() = attributes["class"].orEmpty()

    val displayLabel: String
        get() {
            val sb = StringBuilder(tagName)
            if (elementId.isNotEmpty()) sb.append("#$elementId")
            if (className.isNotEmpty()) sb.append(".${className.replace(' ', '.')}")
            return sb.toString()
        }

    fun getStyle(propertyName: String): String {
        return inlineStyles[propertyName].orEmpty()
    }

    fun setStyle(propertyName: String, value: String) {
        if (value.isBlank()) {
            inlineStyles.remove(propertyName)
        } else {
            inlineStyles[propertyName] = value.trim()
        }
        syncInlineStyleAttribute()
    }

    private fun syncInlineStyleAttribute() {
        if (inlineStyles.isEmpty()) {
            attributes.remove("style")
        } else {
            attributes["style"] = inlineStyles.entries.joinToString("; ") { "${it.key}: ${it.value}" }
        }
    }

    fun findNodeById(targetId: String): DesignNode? {
        if (nodeId == targetId || elementId == targetId) return this
        for (child in children) {
            val found = child.findNodeById(targetId)
            if (found != null) return found
        }
        return null
    }

    fun findParentOf(targetId: String): DesignNode? {
        for (child in children) {
            if (child.nodeId == targetId) return this
            val parent = child.findParentOf(targetId)
            if (parent != null) return parent
        }
        return null
    }
}

enum class CanvasLayoutMode {
    RESPONSIVE_FLOW,
    FREE_POSITIONING,
}