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
}