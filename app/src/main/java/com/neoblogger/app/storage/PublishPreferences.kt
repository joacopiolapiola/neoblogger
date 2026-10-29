package com.neoblogger.app.storage

import android.content.Context
import android.content.SharedPreferences

data class FtpConfig(
    val host: String = "",
    val port: Int = 21,
    val user: String = "",
    val pass: String = "",
    val path: String = "/"
) {
    val isValid: Boolean get() = host.isNotBlank() && user.isNotBlank()
}

data class NeocitiesConfig(
    val apiToken: String = "",
    val siteName: String = ""
) {
    val isValid: Boolean get() = apiToken.isNotBlank() || siteName.isNotBlank()
}

class PublishPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("neoblogger_publish_prefs", Context.MODE_PRIVATE)

    fun getFtpConfig(projectId: String): FtpConfig {
        val host = prefs.getString("ftp_host_$projectId", "").orEmpty()
        val port = prefs.getInt("ftp_port_$projectId", 21)
        val user = prefs.getString("ftp_user_$projectId", "").orEmpty()
        val pass = prefs.getString("ftp_pass_$projectId", "").orEmpty()
        val path = prefs.getString("ftp_path_$projectId", "/").orEmpty()
        return FtpConfig(host, port, user, pass, path)
    }

    fun saveFtpConfig(projectId: String, config: FtpConfig) {
        prefs.edit()
            .putString("ftp_host_$projectId", config.host.trim())
            .putInt("ftp_port_$projectId", config.port)
            .putString("ftp_user_$projectId", config.user.trim())
            .putString("ftp_pass_$projectId", config.pass)
            .putString("ftp_path_$projectId", config.path.trim())
            .apply()
    }

    fun getNeocitiesConfig(projectId: String): NeocitiesConfig {
        val token = prefs.getString("neocities_token_$projectId", "").orEmpty()
        val site = prefs.getString("neocities_site_$projectId", "").orEmpty()
        return NeocitiesConfig(token, site)
    }

    fun saveNeocitiesConfig(projectId: String, config: NeocitiesConfig) {
        prefs.edit()
            .putString("neocities_token_$projectId", config.apiToken.trim())
            .putString("neocities_site_$projectId", config.siteName.trim())
            .apply()
    }
}