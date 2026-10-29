package com.neoblogger.app.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishPreferencesTest {

    @Test
    fun ftpConfig_validationLogic() {
        val invalidConfig = FtpConfig(host = "", user = "")
        assertFalse(invalidConfig.isValid)

        val validConfig = FtpConfig(host = "ftp.example.com", user = "myuser")
        assertTrue(validConfig.isValid)
    }

    @Test
    fun neocitiesConfig_validationLogic() {
        val invalidConfig = NeocitiesConfig(apiToken = "", siteName = "")
        assertFalse(invalidConfig.isValid)

        val validConfigToken = NeocitiesConfig(apiToken = "token123")
        assertTrue(validConfigToken.isValid)

        val validConfigSite = NeocitiesConfig(siteName = "my-site")
        assertTrue(validConfigSite.isValid)
    }
}