package com.neoblogger.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.neoblogger.app.R
import com.neoblogger.app.data.ProjectRepository
import com.neoblogger.app.data.WebsiteProject
import com.neoblogger.app.web.ProjectResourceHandler
import java.io.ByteArrayInputStream

@Composable
fun PreviewTabScreen(
    repository: ProjectRepository,
    project: WebsiteProject,
    revision: Int,
    hasUnsavedChanges: Boolean,
    onReloadRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pendingExternalUrl by remember { mutableStateOf<String?>(null) }
    var currentPath by remember { mutableStateOf("${project.entryPage}") }

    val assetLoader = remember(project.id) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/project/", ProjectResourceHandler(repository.projectRoot(project.id), project.id))
            .build()
    }
    val startUrl = "https://appassets.androidplatform.net/project/${project.id}/${project.entryPage}"

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { webViewInstance?.goBack() },
                    enabled = webViewInstance?.canGoBack() == true,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("◄ " + stringResource(R.string.back), fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { webViewInstance?.goForward() },
                    enabled = webViewInstance?.canGoForward() == true,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.forward) + " ►", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onReloadRequested()
                        webViewInstance?.reload()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🔄 " + stringResource(R.string.reload), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = currentPath,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (hasUnsavedChanges) {
            Text(
                text = stringResource(R.string.preview_saved_snapshot),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.allowFileAccessFromFileURLs = false
                    settings.allowUniversalAccessFromFileURLs = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            if (request.isForMainFrame && !request.method.equals("GET", ignoreCase = true)) {
                                val warning = """<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Submission blocked</title><body style="font:16px sans-serif;padding:24px"><h1>Submission blocked</h1><p>${context.getString(R.string.external_form_blocked)}</p></body></html>"""
                                return WebResourceResponse(
                                    "text/html",
                                    "UTF-8",
                                    403,
                                    "Blocked",
                                    mapOf("Cache-Control" to "no-store"),
                                    ByteArrayInputStream(warning.toByteArray(Charsets.UTF_8))
                                )
                            }
                            return assetLoader.shouldInterceptRequest(request.url)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val uri = request.url
                            val localProjectPath = "/project/${project.id}/"
                            if (uri.scheme == "https" && uri.host == "appassets.androidplatform.net" &&
                                uri.path.orEmpty().startsWith(localProjectPath)
                            ) {
                                currentPath = uri.path.orEmpty().substringAfter(localProjectPath)
                                return false
                            }
                            if (uri.scheme == "https" || uri.scheme == "http" || uri.scheme == "mailto" || uri.scheme == "tel") {
                                pendingExternalUrl = uri.toString()
                            }
                            return true
                        }
                    }

                    tag = revision
                    loadUrl(startUrl)
                    webViewInstance = this
                }
            },
            update = { webView ->
                if (webView.tag != revision) {
                    webView.tag = revision
                    webView.loadUrl(startUrl)
                }
            }
        )
    }

    pendingExternalUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingExternalUrl = null },
            title = { Text(stringResource(R.string.external_link_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(R.string.external_link_message) + "\n\n$url") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingExternalUrl = null
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    }
                ) {
                    Text(stringResource(R.string.open_link), fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExternalUrl = null }) {
                    Text(stringResource(R.string.cancel), fontSize = 14.sp)
                }
            }
        )
    }
}