package com.neoblogger.app.web

import org.junit.Assert.assertEquals
import org.junit.Test

class WebSiteClonerTest {

    @Test
    fun normalizeUrl_formatsNeocitiesAndStandardUrls() {
        assertEquals("https://my-blog.neocities.org", WebSiteCloner.normalizeUrl("my-blog"))
        assertEquals("https://my-blog.neocities.org", WebSiteCloner.normalizeUrl("my-blog.neocities.org"))
        assertEquals("https://example.com", WebSiteCloner.normalizeUrl("https://example.com/"))
        assertEquals("https://example.com/site", WebSiteCloner.normalizeUrl("example.com/site"))
    }

    @Test
    fun deriveRelativePath_preservesExactDirectoryHierarchy() {
        val root = "https://mysite.neocities.org"

        assertEquals("index.html", WebSiteCloner.deriveRelativePath(root, "https://mysite.neocities.org/"))
        assertEquals("css/style.css", WebSiteCloner.deriveRelativePath(root, "https://mysite.neocities.org/css/style.css"))
        assertEquals("images/photos/header.jpg", WebSiteCloner.deriveRelativePath(root, "https://mysite.neocities.org/images/photos/header.jpg"))
        assertEquals("pages/about.html", WebSiteCloner.deriveRelativePath(root, "https://mysite.neocities.org/pages/about.html"))
    }

    @Test
    fun resolveAbsoluteUrl_handlesRelativeAndAbsoluteLinks() {
        val pageUrl = "https://mysite.neocities.org/pages/about.html"

        assertEquals("https://mysite.neocities.org/css/style.css", WebSiteCloner.resolveAbsoluteUrl(pageUrl, "../css/style.css"))
        assertEquals("https://mysite.neocities.org/images/pic.png", WebSiteCloner.resolveAbsoluteUrl(pageUrl, "/images/pic.png"))
    }
}