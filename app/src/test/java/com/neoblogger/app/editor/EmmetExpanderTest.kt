package com.neoblogger.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EmmetExpanderTest {

    @Test
    fun expandHtml_simpleTag() {
        val result = EmmetExpander.expand("div")
        assertEquals("<div></div>", result)
    }

    @Test
    fun expandHtml_tagWithClassAndId() {
        val result = EmmetExpander.expand("h1.title#header")
        assertEquals("<h1 id=\"header\" class=\"title\"></h1>", result)
    }

    @Test
    fun expandHtml_tagWithAttributesAndText() {
        val result = EmmetExpander.expand("a[href=\"https://example.com\"]{Click Here}")
        assertEquals("<a href=\"https://example.com\">Click Here</a>", result)
    }

    @Test
    fun expandHtml_nestedTags() {
        val result = EmmetExpander.expand("div.container>h1.title")
        val expected = "<div class=\"container\">\n  <h1 class=\"title\"></h1>\n</div>"
        assertEquals(expected, result)
    }

    @Test
    fun expandHtml_multiplication() {
        val result = EmmetExpander.expand("p*2")
        assertEquals("<p></p>\n<p></p>", result)
    }

    @Test
    fun expandCss_abbreviations() {
        assertEquals("width: 100%;", EmmetExpander.expand("w100p", isCss = true))
        assertEquals("margin: 10px;", EmmetExpander.expand("m10", isCss = true))
        assertEquals("background: #ffffff;", EmmetExpander.expand("bg#ffffff", isCss = true))
        assertEquals("display: flex;", EmmetExpander.expand("df", isCss = true))
    }

    @Test
    fun expandHtml_complexEmmet() {
        val result = EmmetExpander.expand("ul.nav>li.item*2>a{Link}")
        assertNotNull(result)
        assertEquals(true, result?.contains("<ul class=\"nav\">"))
        assertEquals(true, result?.contains("<li class=\"item\">"))
        assertEquals(true, result?.contains("<a href=\"\">Link</a>"))
    }

    @Test
    fun expandHtml_exclamationBoilerplate() {
        val result = EmmetExpander.expand("!")
        assertNotNull(result)
        assertEquals(true, result?.contains("<!DOCTYPE html>"))
        assertEquals(true, result?.contains("<title>Document</title>"))
        assertEquals(true, result?.contains("<body>"))
    }

    @Test
    fun expandHtml_linkCssAndSpecialAliases() {
        assertEquals("<link rel=\"stylesheet\" href=\"style.css\">", EmmetExpander.expand("link:css"))
        assertEquals("<script src=\"script.js\"></script>", EmmetExpander.expand("script:src"))
        assertEquals("<input type=\"password\" name=\"\" id=\"\">", EmmetExpander.expand("input:p"))
    }

    @Test
    fun expandHtml_numberingMultiplication() {
        val result = EmmetExpander.expand("li.item-$*3")
        assertNotNull(result)
        assertEquals(true, result?.contains("class=\"item-1\""))
        assertEquals(true, result?.contains("class=\"item-2\""))
        assertEquals(true, result?.contains("class=\"item-3\""))
    }

    @Test
    fun expandHtml_climbUpAndGrouping() {
        val result = EmmetExpander.expand("div>header>h1^main>p{Hello}")
        assertNotNull(result)
        assertEquals(true, result?.contains("<header>\n    <h1></h1>\n  </header>"))
        assertEquals(true, result?.contains("<main>\n    <p>Hello</p>\n  </main>"))
    }
}