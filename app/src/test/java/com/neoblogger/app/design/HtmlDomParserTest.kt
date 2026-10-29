package com.neoblogger.app.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlDomParserTest {

    @Test
    fun parseAndRender_preservesHtmlStructure() {
        val originalHtml = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Test Page</title>
</head>
<body>
  <main>
    <h1 id="title" class="main-title">Hello World</h1>
    <p>Some paragraph text</p>
  </main>
</body>
</html>
"""

        val doc = HtmlDomParser.parse(originalHtml)
        assertNotNull(doc.bodyNode)

        val rendered = doc.toHtml()
        assertTrue(rendered.contains("<!doctype html>"))
        assertTrue(rendered.contains("<title>Test Page</title>"))
        assertTrue(rendered.contains("<h1 id=\"title\" class=\"main-title\">Hello World</h1>"))
        assertTrue(rendered.contains("<p>Some paragraph text</p>"))
    }

    @Test
    fun modifyNodeProperties_updatesRenderedHtmlSource() {
        val originalHtml = """<!doctype html>
<html lang="en">
<body>
  <h1 id="headline">Old Headline</h1>
</body>
</html>
"""

        val doc = HtmlDomParser.parse(originalHtml)
        val h1Node = doc.bodyNode.children.firstOrNull { it.tagName == "h1" }
        assertNotNull(h1Node)

        h1Node?.textContent = "New Updated Headline"
        h1Node?.setStyle("color", "#ff0000")

        val rendered = doc.toHtml()
        assertTrue(rendered.contains("New Updated Headline"))
        assertTrue(rendered.contains("style=\"color: #ff0000\""))
    }

    @Test
    fun findLineNumberInHtml_returnsCorrectLine() {
        val html = """<!doctype html>
<html lang="en">
<body>
  <header>
    <h1 id="main-header">Welcome</h1>
  </header>
  <main>
    <p class="intro">Paragraph</p>
  </main>
</body>
</html>"""

        val doc = HtmlDomParser.parse(html)
        val h1Node = doc.bodyNode.findNodeById("main-header")
        assertNotNull(h1Node)

        val line = HtmlDomParser.findLineNumberInHtml(html, h1Node!!)
        assertEquals(5, line)
    }

    @Test
    fun toHtml_includeDesignAttributes_rendersDataNbIdAndPreservesOnParse() {
        val originalHtml = """<!doctype html>
<html lang="en">
<body>
  <main>
    <h1 id="title">Headline</h1>
    <button>Click</button>
  </main>
</body>
</html>"""

        val doc = HtmlDomParser.parse(originalHtml)
        val h1Node = doc.bodyNode.findNodeById("title")
        assertNotNull(h1Node)
        val originalNodeId = h1Node!!.nodeId

        // Render with design attributes
        val designHtml = doc.toHtml(includeDesignAttributes = true)
        assertTrue(designHtml.contains("data-nb-id=\"$originalNodeId\""))

        // Re-parse design HTML
        val doc2 = HtmlDomParser.parse(designHtml)
        val h1Node2 = doc2.bodyNode.findNodeById("title")
        assertNotNull(h1Node2)
        assertEquals(originalNodeId, h1Node2!!.nodeId)

        // Clean html export removes data-nb-id
        val cleanHtml = doc2.toHtml(includeDesignAttributes = false)
        assertTrue(!cleanHtml.contains("data-nb-id="))
    }

    @Test
    fun parse_extractsStyleTagsToHead() {
        val htmlWithStyle = """<!doctype html>
<html lang="en">
<body>
  <style>body { background-color: black; }</style>
  <h1>Title</h1>
</body>
</html>"""

        val doc = HtmlDomParser.parse(htmlWithStyle)
        assertTrue(doc.headHtml.contains("background-color: black"))
        val bodyChildTags = doc.bodyNode.children.map { it.tagName }
        assertTrue(!bodyChildTags.contains("style"))
    }

    @Test
    fun setFixedMobileViewport_togglesViewportMeta() {
        val html = "<!doctype html><html><head><title>Test</title></head><body><h1>Hi</h1></body></html>"
        val doc = HtmlDomParser.parse(html)

        HtmlDomParser.setFixedMobileViewport(doc, true)
        assertTrue(HtmlDomParser.hasFixedMobileViewport(doc.headHtml))
        assertTrue(doc.headHtml.contains("width=375"))

        HtmlDomParser.setFixedMobileViewport(doc, false)
        assertTrue(!HtmlDomParser.hasFixedMobileViewport(doc.headHtml))
        assertTrue(doc.headHtml.contains("width=device-width"))
    }
}