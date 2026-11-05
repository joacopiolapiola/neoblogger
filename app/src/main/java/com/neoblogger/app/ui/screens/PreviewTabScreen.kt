package com.neoblogger.app.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.neoblogger.app.R
import com.neoblogger.app.data.ProjectRepository
import com.neoblogger.app.data.WebsiteProject
import com.neoblogger.app.web.ProjectResourceHandler
import java.io.ByteArrayInputStream
import java.util.UUID

private data class BrowserTabState(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "index.html",
    var displayUrl: String = "index.html",
    var loadableUrl: String = "index.html",
    var isWebUrl: Boolean = false,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false,
    val historyList: MutableList<String> = mutableListOf()
)

private fun resolveUrlForLoading(projectId: String, input: String): Pair<String, Boolean> {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return Pair(trimmed, true)
    }
    if (trimmed.contains(".") && !trimmed.endsWith(".html") && !trimmed.contains("/")) {
        return Pair("https://$trimmed", true)
    }
    val cleanPath = trimmed.trimStart('/')
    val localUrl = "https://appassets.androidplatform.net/project/$projectId/$cleanPath"
    return Pair(localUrl, false)
}

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
    var showHistoryDialog by remember { mutableStateOf(false) }

    val tabs = remember(project.id) {
        mutableStateListOf(
            BrowserTabState(
                title = project.entryPage,
                displayUrl = project.entryPage,
                loadableUrl = "https://appassets.androidplatform.net/project/${project.id}/${project.entryPage}",
                isWebUrl = false,
                historyList = mutableListOf(project.entryPage)
            )
        )
    }

    var activeTabId by remember(project.id) { mutableStateOf(tabs.first().id) }
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    var addressBarInput by remember(activeTabId, activeTab.displayUrl) { mutableStateOf(activeTab.displayUrl) }

    val assetLoader = remember(project.id) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/project/", ProjectResourceHandler(repository.projectRoot(project.id), project.id))
            .build()
    }

    fun navigateActiveTab(newUrlInput: String) {
        val (loadUrl, isWeb) = resolveUrlForLoading(project.id, newUrlInput)
        activeTab.displayUrl = newUrlInput
        activeTab.loadableUrl = loadUrl
        activeTab.isWebUrl = isWeb
        activeTab.title = newUrlInput.substringAfterLast('/')
        if (activeTab.historyList.lastOrNull() != newUrlInput) {
            activeTab.historyList.add(newUrlInput)
        }
        addressBarInput = newUrlInput
        webViewInstance?.loadUrl(loadUrl)
    }

    fun addNewTab(defaultUrl: String = project.entryPage) {
        val (loadUrl, isWeb) = resolveUrlForLoading(project.id, defaultUrl)
        val newTab = BrowserTabState(
            title = defaultUrl,
            displayUrl = defaultUrl,
            loadableUrl = loadUrl,
            isWebUrl = isWeb,
            historyList = mutableListOf(defaultUrl)
        )
        tabs.add(newTab)
        activeTabId = newTab.id
        addressBarInput = defaultUrl
    }

    fun closeTab(tabId: String) {
        if (tabs.size > 1) {
            val idx = tabs.indexOfFirst { it.id == tabId }
            tabs.removeAll { it.id == tabId }
            if (activeTabId == tabId) {
                val nextTab = tabs.getOrNull(idx.coerceAtMost(tabs.size - 1)) ?: tabs.first()
                activeTabId = nextTab.id
                addressBarInput = nextTab.displayUrl
            }
        } else {
            // Re-initialize single tab
            tabs.first().apply {
                title = project.entryPage
                displayUrl = project.entryPage
                loadableUrl = "https://appassets.androidplatform.net/project/${project.id}/${project.entryPage}"
                isWebUrl = false
                historyList.clear()
                historyList.add(project.entryPage)
            }
            addressBarInput = project.entryPage
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // TOP MULTI-TAB BROWSER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val icon = if (tab.isWebUrl) "🌐" else "📄"

                Card(
                    onClick = {
                        activeTabId = tab.id
                        addressBarInput = tab.displayUrl
                    },
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("$icon ${tab.title}", fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                        TextButton(
                            onClick = { closeTab(tab.id) },
                            modifier = Modifier.height(24.dp).width(24.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("✕", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { addNewTab() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("+ New Tab", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // BROWSER CONTROL & ADDRESS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            OutlinedButton(
                onClick = {
                    if (webViewInstance?.canGoBack() == true) {
                        webViewInstance?.goBack()
                    }
                },
                enabled = activeTab.canGoBack,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text("◄", fontSize = 12.sp)
            }

            // Forward Button
            OutlinedButton(
                onClick = {
                    if (webViewInstance?.canGoForward() == true) {
                        webViewInstance?.goForward()
                    }
                },
                enabled = activeTab.canGoForward,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text("►", fontSize = 12.sp)
            }

            // Reload Button
            Button(
                onClick = {
                    onReloadRequested()
                    webViewInstance?.reload()
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text("🔄", fontSize = 13.sp)
            }

            // URL / Path Address Bar
            OutlinedTextField(
                value = addressBarInput,
                onValueChange = { addressBarInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { navigateActiveTab(addressBarInput) }),
                shape = RoundedCornerShape(8.dp)
            )

            // Go Button
            Button(
                onClick = { navigateActiveTab(addressBarInput) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text("➔", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // History Button
            OutlinedButton(
                onClick = { showHistoryDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text("📜", fontSize = 13.sp)
            }
        }

        if (hasUnsavedChanges) {
            Text(
                text = stringResource(R.string.preview_saved_snapshot),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp, horizontal = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        // WEBVIEW BROWSER CONTAINER
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                val urlStr = request.url.toString()
                                if (urlStr.startsWith("https://appassets.androidplatform.net/project/${project.id}/")) {
                                    return assetLoader.shouldInterceptRequest(request.url)
                                }
                                return null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != null) {
                                    activeTab.canGoBack = view?.canGoBack() == true
                                    activeTab.canGoForward = view?.canGoForward() == true
                                    val pageTitle = view?.title.orEmpty().ifBlank { url.substringAfterLast('/') }
                                    activeTab.title = pageTitle

                                    val localPathPrefix = "https://appassets.androidplatform.net/project/${project.id}/"
                                    if (url.startsWith(localPathPrefix)) {
                                        val relPath = url.substringAfter(localPathPrefix)
                                        activeTab.displayUrl = relPath
                                        activeTab.isWebUrl = false
                                        addressBarInput = relPath
                                        if (activeTab.historyList.lastOrNull() != relPath) {
                                            activeTab.historyList.add(relPath)
                                        }
                                    } else {
                                        activeTab.displayUrl = url
                                        activeTab.isWebUrl = true
                                        addressBarInput = url
                                        if (activeTab.historyList.lastOrNull() != url) {
                                            activeTab.historyList.add(url)
                                        }
                                    }
                                }
                            }
                        }

                        tag = activeTab.id
                        loadUrl(activeTab.loadableUrl)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    if (webView.tag != activeTab.id) {
                        webView.tag = activeTab.id
                        webView.loadUrl(activeTab.loadableUrl)
                    }
                    webViewInstance = webView
                }
            )
        }
    }

    // BROWSING SESSION HISTORY DIALOG
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📜 Browsing History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { activeTab.historyList.clear() }) {
                        Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            text = {
                if (activeTab.historyList.isEmpty()) {
                    Text("No browsing history for this tab yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(activeTab.historyList.reversed()) { historyItem ->
                            Card(
                                onClick = {
                                    navigateActiveTab(historyItem)
                                    showHistoryDialog = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(if (historyItem.startsWith("http")) "🌐" else "📄", fontSize = 14.sp)
                                    Text(
                                        text = historyItem,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", fontSize = 14.sp)
                }
            }
        )
    }
}