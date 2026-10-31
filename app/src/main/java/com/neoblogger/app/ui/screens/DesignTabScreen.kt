package com.neoblogger.app.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.neoblogger.app.data.ProjectRepository
import com.neoblogger.app.design.DesignNode
import com.neoblogger.app.design.HtmlDomParser
import com.neoblogger.app.web.ProjectResourceHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val ALL_HTML_ELEMENTS = listOf(
    "h1", "h2", "h3", "h4", "h5", "h6",
    "p", "span", "b", "i", "strong", "em", "small", "blockquote", "code", "pre",
    "button", "a",
    "img", "video", "audio", "figure", "figcaption",
    "div", "section", "article", "header", "footer", "nav", "main", "aside",
    "ul", "ol", "li",
    "form", "input", "textarea", "select", "option", "label",
    "table", "tbody", "thead", "tr", "td", "th",
    "hr", "br", "canvas", "iframe", "details", "summary"
)

private val CANNOT_HAVE_CHILDREN = setOf(
    "img", "input", "br", "hr", "meta", "link", "source", "embed"
)

private fun getContextualSuggestions(parentTag: String): List<String> {
    return when (parentTag.lowercase()) {
        "ul", "ol" -> listOf("li", "p", "a")
        "table", "tbody", "thead" -> listOf("tr")
        "tr" -> listOf("td", "th")
        "select" -> listOf("option")
        "form" -> listOf("label", "input", "button", "select", "textarea")
        "nav" -> listOf("a", "button", "ul", "div")
        "section", "article", "main", "header", "footer", "aside", "div", "body" ->
            listOf("h1", "h2", "h3", "p", "button", "a", "img", "div", "ul", "form")
        else -> listOf("p", "span", "strong", "button", "a", "img")
    }
}

private fun parsePx(styleValue: String): Int {
    if (styleValue.isBlank()) return 0
    val digits = styleValue.replace("px", "").trim()
    return digits.toIntOrNull() ?: 0
}

private fun parseRotateDeg(transformStyle: String): Int {
    if (transformStyle.isBlank()) return 0
    val match = Regex("rotate\\((-?\\d+)deg\\)").find(transformStyle)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
}

private fun findMatchingNode(parent: DesignNode, tag: String, id: String, text: String): DesignNode? {
    if (tag.isNotEmpty() && parent.tagName.equals(tag, ignoreCase = true)) {
        if (id.isNotEmpty() && parent.elementId == id) return parent
        if (text.isNotEmpty() && parent.textContent.contains(text)) return parent
    }
    for (child in parent.children) {
        val found = findMatchingNode(child, tag, id, text)
        if (found != null) return found
    }
    return null
}

private enum class ActiveToolMode {
    SUMMARY, UNILATERAL_SIZE, ROTATE, NUDGE_POSITION
}

@Composable
private fun ColorSwatchRow(
    currentColor: String,
    onColorSelected: (String) -> Unit,
) {
    val swatches = listOf(
        "#000000" to Color(0xFF000000),
        "#ffffff" to Color(0xFFFFFFFF),
        "#2196f3" to Color(0xFF2196F3),
        "#f44336" to Color(0xFFF44336),
        "#4caf50" to Color(0xFF4CAF50),
        "#ffeb3b" to Color(0xFFFFEB3B),
        "#ff9800" to Color(0xFFFF9800),
        "#9c27b0" to Color(0xFF9C27B0),
        "#9e9e9e" to Color(0xFF9E9E9E),
        "#212121" to Color(0xFF212121),
        "transparent" to Color.Transparent
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.forEach { (hex, color) ->
            val isSelected = currentColor.equals(hex, ignoreCase = true)
            Surface(
                onClick = { onColorSelected(hex) },
                shape = CircleShape,
                color = if (hex == "transparent") Color.Gray.copy(alpha = 0.3f) else color,
                modifier = Modifier
                    .size(28.dp)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
            ) {
                if (hex == "transparent") {
                    Box(contentAlignment = Alignment.Center) {
                        Text("∅", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericStepperField(
    value: String,
    label: String,
    unit: String = "px",
    step: Int = 1,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val numericVal = value.replace(unit, "").replace("px", "").trim().toIntOrNull() ?: 0

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            onClick = {
                val newNum = (numericVal - step).coerceAtLeast(0)
                onValueChange("${newNum}$unit")
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(54.dp)
        ) {
            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )

        OutlinedButton(
            onClick = {
                val newNum = numericVal + step
                onValueChange("${newNum}$unit")
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(54.dp)
        ) {
            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignTabScreen(
    htmlSource: String,
    projectId: String,
    repository: ProjectRepository,
    onHtmlSourceChanged: (String) -> Unit,
    onJumpToCodeRequested: (String, Int) -> Unit,
    onRequestPickMedia: ((String) -> Unit) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastEmittedHtml by remember { mutableStateOf("") }
    var doc by remember(projectId) { mutableStateOf(HtmlDomParser.parse(htmlSource)) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var activeToolMode by remember { mutableStateOf(ActiveToolMode.SUMMARY) }

    var showCreateElementDialog by remember { mutableStateOf(false) }
    var createElementSearchQuery by remember { mutableStateOf("") }

    var showPropertiesSheet by remember { mutableStateOf(false) }
    var showNewChildDialog by remember { mutableStateOf(false) }
    var childSearchQuery by remember { mutableStateOf("") }

    val selectedNode = selectedNodeId?.let { id -> doc.bodyNode.findNodeById(id) }

    // Synchronize doc if htmlSource changed externally (e.g. from Code tab)
    LaunchedEffect(htmlSource) {
        if (htmlSource != lastEmittedHtml && htmlSource.isNotBlank()) {
            doc = HtmlDomParser.parse(htmlSource)
        }
    }

    val assetLoader = remember(projectId) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/project/", ProjectResourceHandler(repository.projectRoot(projectId), projectId))
            .build()
    }
    val baseUrl = "https://appassets.androidplatform.net/project/$projectId/"

    fun updateDocumentAndEmit() {
        val cleanHtml = doc.toHtml(includeDesignAttributes = false)
        lastEmittedHtml = cleanHtml
        onHtmlSourceChanged(cleanHtml)

        // Save cleanly to disk for assetLoader
        scope.launch(Dispatchers.IO) {
            runCatching {
                repository.saveFile(projectId, "index.html", cleanHtml)
            }
        }

        // Render design HTML with node IDs in WebView canvas
        val designHtml = doc.toHtml(includeDesignAttributes = true)
        webViewRef?.loadDataWithBaseURL(baseUrl, designHtml, "text/html", "UTF-8", null)
    }

    // Keep JS highlight synchronized whenever selectedNodeId changes
    LaunchedEffect(selectedNodeId) {
        val targetId = selectedNodeId
        val js = if (targetId != null) {
            "if (window.__neoblogger_selectNode) { window.__neoblogger_selectNode('$targetId'); }"
        } else {
            "if (window.__neoblogger_selectNode) { window.__neoblogger_selectNode(null); }"
        }
        webViewRef?.evaluateJavascript(js, null)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Header
        val isMobileLocked = HtmlDomParser.hasFixedMobileViewport(doc.headHtml)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Visual Canvas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Checkbox(
                    checked = isMobileLocked,
                    onCheckedChange = { checked ->
                        HtmlDomParser.setFixedMobileViewport(doc, checked)
                        updateDocumentAndEmit()
                    }
                )
                Text("📱 Lock Mobile", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (selectedNode != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "<${selectedNode.displayLabel}>",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedButton(
                        onClick = { selectedNodeId = null },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("✕", fontSize = 12.sp)
                    }
                }
            }
        }

        // Rendered Canvas (WebView with touch inspector and multi-touch gestures)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, if (selectedNode != null) MaterialTheme.colorScheme.primary else Color.DarkGray, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                                assetLoader.shouldInterceptRequest(request.url)

                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val uri = request.url
                                if (uri.scheme == "neoblogger-design") {
                                    when (uri.host) {
                                        "tap-empty" -> {
                                            selectedNodeId = null
                                            showCreateElementDialog = true
                                        }
                                        "select" -> {
                                            val nodeIdParam = uri.getQueryParameter("nodeId").orEmpty()
                                            var matched: DesignNode? = null
                                            if (nodeIdParam.isNotEmpty()) {
                                                matched = doc.bodyNode.findNodeById(nodeIdParam)
                                            }
                                            if (matched == null) {
                                                val tag = uri.getQueryParameter("tag").orEmpty()
                                                val id = uri.getQueryParameter("id").orEmpty()
                                                val text = uri.getQueryParameter("text").orEmpty()
                                                if (id.isNotEmpty()) {
                                                    matched = doc.bodyNode.findNodeById(id)
                                                }
                                                if (matched == null) {
                                                    matched = findMatchingNode(doc.bodyNode, tag, id, text)
                                                }
                                            }

                                            if (matched != null) {
                                                selectedNodeId = matched.nodeId
                                            } else {
                                                val tag = uri.getQueryParameter("tag").orEmpty()
                                                val text = uri.getQueryParameter("text").orEmpty()
                                                val newNode = HtmlDomParser.createNode(tag.ifEmpty { "p" }, textContent = text)
                                                doc.bodyNode.children.add(newNode)
                                                selectedNodeId = newNode.nodeId
                                                updateDocumentAndEmit()
                                            }
                                        }
                                        "gesture-end" -> {
                                            val nodeIdParam = uri.getQueryParameter("nodeId").orEmpty()
                                            val node = doc.bodyNode.findNodeById(nodeIdParam)
                                            if (node != null) {
                                                val w = uri.getQueryParameter("w")
                                                val h = uri.getQueryParameter("h")
                                                val left = uri.getQueryParameter("left")
                                                val top = uri.getQueryParameter("top")
                                                val rot = uri.getQueryParameter("rot")

                                                node.setStyle("position", "relative")
                                                if (node.tagName.lowercase() == "a") {
                                                    if (node.getStyle("display").isBlank() || node.getStyle("display") == "inline") {
                                                        node.setStyle("display", "inline-block")
                                                    }
                                                }
                                                if (!w.isNullOrEmpty()) node.setStyle("width", "${w}px")
                                                if (!h.isNullOrEmpty()) node.setStyle("height", "${h}px")
                                                if (!left.isNullOrEmpty()) node.setStyle("left", "${left}px")
                                                if (!top.isNullOrEmpty()) node.setStyle("top", "${top}px")
                                                if (!rot.isNullOrEmpty()) node.setStyle("transform", "rotate(${rot}deg)")

                                                updateDocumentAndEmit()
                                            }
                                        }
                                    }
                                    return true
                                }
                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val currentSel = selectedNodeId
                                val selJsArg = if (currentSel != null) "'$currentSel'" else "null"
                                val js = """
                                    (function() {
                                        var style = document.getElementById('__neoblogger_style');
                                        if (!style) {
                                            style = document.createElement('style');
                                            style.id = '__neoblogger_style';
                                            style.innerHTML = '.nb-selected-element { outline: 3px solid #2196F3 !important; outline-offset: 2px !important; box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.3) !important; transition: outline 0.1s ease-in-out; }';
                                            document.head.appendChild(style);
                                        }

                                        // Restore scroll position to prevent canvas jumps
                                        if (window.__neoblogger_scroll_x !== undefined && window.__neoblogger_scroll_y !== undefined) {
                                            window.scrollTo(window.__neoblogger_scroll_x, window.__neoblogger_scroll_y);
                                        }
                                        window.addEventListener('scroll', function() {
                                            window.__neoblogger_scroll_x = window.scrollX || window.pageXOffset;
                                            window.__neoblogger_scroll_y = window.scrollY || window.pageYOffset;
                                        });

                                        window.__neoblogger_selectNode = function(nodeId) {
                                            window.__neoblogger_selected_node_id = nodeId;
                                            var prev = document.querySelectorAll('.nb-selected-element');
                                            for (var i = 0; i < prev.length; i++) {
                                                prev[i].classList.remove('nb-selected-element');
                                            }
                                            if (nodeId) {
                                                var el = document.querySelector('[data-nb-id="' + nodeId + '"]');
                                                if (el) {
                                                    el.classList.add('nb-selected-element');
                                                }
                                            }
                                        };

                                        if (window.__neoblogger_inspector_injected) {
                                            if (window.__neoblogger_selectNode) {
                                                window.__neoblogger_selectNode($selJsArg);
                                            }
                                            return;
                                        }
                                        window.__neoblogger_inspector_injected = true;

                                        document.addEventListener('click', function(e) {
                                            e.preventDefault();
                                            e.stopPropagation();
                                            var target = e.target;
                                            if (target === document.body || target === document.documentElement) {
                                                window.location.href = 'neoblogger-design://tap-empty';
                                                return;
                                            }
                                            var nodeEl = target.closest('[data-nb-id]');
                                            if (nodeEl) {
                                                var nodeId = nodeEl.getAttribute('data-nb-id');
                                                window.location.href = 'neoblogger-design://select?nodeId=' + encodeURIComponent(nodeId);
                                            } else {
                                                var tag = target.tagName ? target.tagName.toLowerCase() : 'p';
                                                var id = target.id || '';
                                                var text = (target.innerText || target.textContent || '').trim().substring(0, 30);
                                                window.location.href = 'neoblogger-design://select?tag=' + encodeURIComponent(tag) + '&id=' + encodeURIComponent(id) + '&text=' + encodeURIComponent(text);
                                            }
                                        }, true);

                                        // Multi-Touch Gestures (Pinch Scale, 2-Finger Move, 2-Finger Rotation)
                                        var gState = null;

                                        function getTouchDist(t1, t2) {
                                            var dx = t2.clientX - t1.clientX;
                                            var dy = t2.clientY - t1.clientY;
                                            return Math.sqrt(dx * dx + dy * dy);
                                        }

                                        function getTouchAngle(t1, t2) {
                                            var dx = t2.clientX - t1.clientX;
                                            var dy = t2.clientY - t1.clientY;
                                            return Math.atan2(dy, dx) * (180 / Math.PI);
                                        }

                                        function getTouchMidpoint(t1, t2) {
                                            return { x: (t1.clientX + t2.clientX) / 2, y: (t1.clientY + t2.clientY) / 2 };
                                        }

                                        document.addEventListener('touchstart', function(e) {
                                            if (e.touches.length === 2 && window.__neoblogger_selected_node_id) {
                                                var selId = window.__neoblogger_selected_node_id;
                                                var el = document.querySelector('[data-nb-id="' + selId + '"]');
                                                if (el) {
                                                    var t1 = e.touches[0];
                                                    var t2 = e.touches[1];
                                                    var style = window.getComputedStyle(el);
                                                    var rect = el.getBoundingClientRect();
                                                    var transform = el.style.transform || '';
                                                    var matchRot = transform.match(/rotate\((-?\d+)deg\)/);
                                                    var initialRot = matchRot ? parseInt(matchRot[1], 10) : 0;

                                                    var leftVal = parseInt(el.style.left || style.left, 10);
                                                    var topVal = parseInt(el.style.top || style.top, 10);
                                                    var initialLeft = isNaN(leftVal) ? 0 : leftVal;
                                                    var initialTop = isNaN(topVal) ? 0 : topVal;

                                                    gState = {
                                                        nodeId: selId,
                                                        element: el,
                                                        startDist: getTouchDist(t1, t2),
                                                        startAngle: getTouchAngle(t1, t2),
                                                        startMid: getTouchMidpoint(t1, t2),
                                                        startWidth: rect.width || 100,
                                                        startHeight: rect.height || 50,
                                                        startLeft: initialLeft,
                                                        startTop: initialTop,
                                                        startRot: initialRot
                                                    };
                                                }
                                            }
                                        }, { passive: false });

                                        document.addEventListener('touchmove', function(e) {
                                            if (e.touches.length === 2 && gState) {
                                                e.preventDefault();
                                                var t1 = e.touches[0];
                                                var t2 = e.touches[1];
                                                var currDist = getTouchDist(t1, t2);
                                                var currAngle = getTouchAngle(t1, t2);
                                                var currMid = getTouchMidpoint(t1, t2);

                                                var scale = currDist / (gState.startDist || 1);
                                                var deltaAngle = Math.round(currAngle - gState.startAngle);
                                                var deltaX = Math.round(currMid.x - gState.startMid.x);
                                                var deltaY = Math.round(currMid.y - gState.startMid.y);

                                                var newW = Math.max(20, Math.round(gState.startWidth * scale));
                                                var newH = Math.max(20, Math.round(gState.startHeight * scale));
                                                var newLeft = gState.startLeft + deltaX;
                                                var newTop = gState.startTop + deltaY;
                                                var newRot = (gState.startRot + deltaAngle) % 360;

                                                gState.element.style.position = 'relative';
                                                gState.element.style.width = newW + 'px';
                                                gState.element.style.height = newH + 'px';
                                                gState.element.style.left = newLeft + 'px';
                                                gState.element.style.top = newTop + 'px';
                                                gState.element.style.transform = 'rotate(' + newRot + 'deg)';

                                                gState.lastW = newW;
                                                gState.lastH = newH;
                                                gState.lastLeft = newLeft;
                                                gState.lastTop = newTop;
                                                gState.lastRot = newRot;
                                            }
                                        }, { passive: false });

                                        document.addEventListener('touchend', function(e) {
                                            if (gState && e.touches.length < 2) {
                                                if (gState.lastW !== undefined) {
                                                    var url = 'neoblogger-design://gesture-end?nodeId=' + encodeURIComponent(gState.nodeId) +
                                                              '&w=' + gState.lastW +
                                                              '&h=' + gState.lastH +
                                                              '&left=' + gState.lastLeft +
                                                              '&top=' + gState.lastTop +
                                                              '&rot=' + gState.lastRot;
                                                    window.location.href = url;
                                                }
                                                gState = null;
                                            }
                                        });

                                        if (window.__neoblogger_selectNode) {
                                            window.__neoblogger_selectNode($selJsArg);
                                        }
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(js, null)
                            }
                        }

                        val designHtml = doc.toHtml(includeDesignAttributes = true)
                        loadDataWithBaseURL(baseUrl, designHtml, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PERSISTENT Bottom Inspector / Toolbar Panel (Stays active while element is selected)
        if (selectedNode != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header with Deselect button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Editing <${selectedNode.displayLabel}>",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedButton(
                            onClick = { selectedNodeId = null },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("✕ Deselect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mode Switcher Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showPropertiesSheet = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Properties ⚙️", fontSize = 12.sp)
                        }

                        if (activeToolMode == ActiveToolMode.UNILATERAL_SIZE) {
                            Button(onClick = { activeToolMode = ActiveToolMode.SUMMARY }, shape = RoundedCornerShape(8.dp)) {
                                Text("↔ Unilateral Size ✓", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(onClick = { activeToolMode = ActiveToolMode.UNILATERAL_SIZE }, shape = RoundedCornerShape(8.dp)) {
                                Text("↔ Unilateral Size", fontSize = 12.sp)
                            }
                        }

                        if (activeToolMode == ActiveToolMode.ROTATE) {
                            Button(onClick = { activeToolMode = ActiveToolMode.SUMMARY }, shape = RoundedCornerShape(8.dp)) {
                                Text("🔄 Rotate ✓", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(onClick = { activeToolMode = ActiveToolMode.ROTATE }, shape = RoundedCornerShape(8.dp)) {
                                Text("🔄 Rotate", fontSize = 12.sp)
                            }
                        }

                        if (activeToolMode == ActiveToolMode.NUDGE_POSITION) {
                            Button(onClick = { activeToolMode = ActiveToolMode.SUMMARY }, shape = RoundedCornerShape(8.dp)) {
                                Text("🎯 Position ✓", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(onClick = { activeToolMode = ActiveToolMode.NUDGE_POSITION }, shape = RoundedCornerShape(8.dp)) {
                                Text("🎯 Position", fontSize = 12.sp)
                            }
                        }

                        if (!CANNOT_HAVE_CHILDREN.contains(selectedNode.tagName.lowercase())) {
                            OutlinedButton(
                                onClick = {
                                    childSearchQuery = ""
                                    showNewChildDialog = true
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Child", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val parent = doc.bodyNode.findParentOf(selectedNode.nodeId) ?: doc.bodyNode
                                parent.children.removeAll { it.nodeId == selectedNode.nodeId }
                                selectedNodeId = null
                                updateDocumentAndEmit()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Tool Panel
                    when (activeToolMode) {
                        ActiveToolMode.UNILATERAL_SIZE -> {
                            Text(
                                text = "Unilateral Directional Sizing (Expand/Shrink single side):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left controls
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Left Edge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = {
                                            val w = parsePx(selectedNode.getStyle("width"))
                                            val left = parsePx(selectedNode.getStyle("left"))
                                            val newW = (if (w == 0) 100 else w - 20).coerceAtLeast(20)
                                            val newLeft = left + 20
                                            selectedNode.setStyle("position", "relative")
                                            selectedNode.setStyle("width", "${newW}px")
                                            selectedNode.setStyle("left", "${newLeft}px")
                                            updateDocumentAndEmit()
                                        }) { Text("Shrink ◄") }

                                        OutlinedButton(onClick = {
                                            val w = parsePx(selectedNode.getStyle("width"))
                                            val left = parsePx(selectedNode.getStyle("left"))
                                            val newW = if (w == 0) 120 else w + 20
                                            val newLeft = left - 20
                                            selectedNode.setStyle("position", "relative")
                                            selectedNode.setStyle("width", "${newW}px")
                                            selectedNode.setStyle("left", "${newLeft}px")
                                            updateDocumentAndEmit()
                                        }) { Text("Expand ◄") }
                                    }
                                }

                                // Right controls
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Right Edge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = {
                                            val w = parsePx(selectedNode.getStyle("width"))
                                            val newW = (if (w == 0) 100 else w - 20).coerceAtLeast(20)
                                            selectedNode.setStyle("width", "${newW}px")
                                            updateDocumentAndEmit()
                                        }) { Text("Shrink ►") }

                                        OutlinedButton(onClick = {
                                            val w = parsePx(selectedNode.getStyle("width"))
                                            val newW = if (w == 0) 120 else w + 20
                                            selectedNode.setStyle("width", "${newW}px")
                                            updateDocumentAndEmit()
                                        }) { Text("Expand ►") }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Top controls
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Top Edge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = {
                                            val h = parsePx(selectedNode.getStyle("height"))
                                            val top = parsePx(selectedNode.getStyle("top"))
                                            val newH = (if (h == 0) 50 else h - 20).coerceAtLeast(20)
                                            val newTop = top + 20
                                            selectedNode.setStyle("position", "relative")
                                            selectedNode.setStyle("height", "${newH}px")
                                            selectedNode.setStyle("top", "${newTop}px")
                                            updateDocumentAndEmit()
                                        }) { Text("Shrink ▲") }

                                        OutlinedButton(onClick = {
                                            val h = parsePx(selectedNode.getStyle("height"))
                                            val top = parsePx(selectedNode.getStyle("top"))
                                            val newH = if (h == 0) 60 else h + 20
                                            val newTop = top - 20
                                            selectedNode.setStyle("position", "relative")
                                            selectedNode.setStyle("height", "${newH}px")
                                            selectedNode.setStyle("top", "${newTop}px")
                                            updateDocumentAndEmit()
                                        }) { Text("Expand ▲") }
                                    }
                                }

                                // Bottom controls
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Bottom Edge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = {
                                            val h = parsePx(selectedNode.getStyle("height"))
                                            val newH = (if (h == 0) 50 else h - 20).coerceAtLeast(20)
                                            selectedNode.setStyle("height", "${newH}px")
                                            updateDocumentAndEmit()
                                        }) { Text("Shrink ▼") }

                                        OutlinedButton(onClick = {
                                            val h = parsePx(selectedNode.getStyle("height"))
                                            val newH = if (h == 0) 60 else h + 20
                                            selectedNode.setStyle("height", "${newH}px")
                                            updateDocumentAndEmit()
                                        }) { Text("Expand ▼") }
                                    }
                                }
                            }
                        }

                        ActiveToolMode.ROTATE -> {
                            val currentRot = parseRotateDeg(selectedNode.getStyle("transform"))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Element Rotation:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "${currentRot}°",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(onClick = {
                                        val newRot = (currentRot - 15) % 360
                                        selectedNode.setStyle("transform", "rotate(${newRot}deg)")
                                        updateDocumentAndEmit()
                                    }) { Text("-15°") }

                                    OutlinedButton(onClick = {
                                        val newRot = (currentRot - 5) % 360
                                        selectedNode.setStyle("transform", "rotate(${newRot}deg)")
                                        updateDocumentAndEmit()
                                    }) { Text("-5°") }

                                    Button(onClick = {
                                        selectedNode.setStyle("transform", "rotate(0deg)")
                                        updateDocumentAndEmit()
                                    }) { Text("Reset 0°") }

                                    OutlinedButton(onClick = {
                                        val newRot = (currentRot + 5) % 360
                                        selectedNode.setStyle("transform", "rotate(${newRot}deg)")
                                        updateDocumentAndEmit()
                                    }) { Text("+5°") }

                                    OutlinedButton(onClick = {
                                        val newRot = (currentRot + 15) % 360
                                        selectedNode.setStyle("transform", "rotate(${newRot}deg)")
                                        updateDocumentAndEmit()
                                    }) { Text("+15°") }
                                }

                                Slider(
                                    value = currentRot.toFloat(),
                                    onValueChange = { degFloat ->
                                        val deg = degFloat.toInt()
                                        selectedNode.setStyle("transform", "rotate(${deg}deg)")
                                        updateDocumentAndEmit()
                                    },
                                    valueRange = -180f..180f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        ActiveToolMode.NUDGE_POSITION -> {
                            Text(
                                text = "Nudge Position (Offset):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Button(
                                        onClick = {
                                            val currentTop = parsePx(selectedNode.getStyle("top"))
                                            selectedNode.setStyle("position", "relative")
                                            selectedNode.setStyle("top", "${currentTop - 10}px")
                                            updateDocumentAndEmit()
                                        },
                                        shape = CircleShape
                                    ) { Text("▲") }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val currentLeft = parsePx(selectedNode.getStyle("left"))
                                                selectedNode.setStyle("position", "relative")
                                                selectedNode.setStyle("left", "${currentLeft - 10}px")
                                                updateDocumentAndEmit()
                                            },
                                            shape = CircleShape
                                        ) { Text("◄") }

                                        Button(
                                            onClick = {
                                                val currentLeft = parsePx(selectedNode.getStyle("left"))
                                                selectedNode.setStyle("position", "relative")
                                                selectedNode.setStyle("left", "${currentLeft + 10}px")
                                                updateDocumentAndEmit()
                                            },
                                            shape = CircleShape
                                        ) { Text("►") }
                                    }

                                    Button(
                                        onClick = {
                                            val currentTop = parsePx(selectedNode.getStyle("top"))
                                            selectedNode.setStyle("position", "relative")
                                            selectedNode.setStyle("top", "${currentTop + 10}px")
                                            updateDocumentAndEmit()
                                        },
                                        shape = CircleShape
                                    ) { Text("▼") }
                                }
                            }
                        }

                        ActiveToolMode.SUMMARY -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Width: ${selectedNode.getStyle("width").ifEmpty { "auto" }} | Height: ${selectedNode.getStyle("height").ifEmpty { "auto" }}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Rotation: ${parseRotateDeg(selectedNode.getStyle("transform"))}°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // No element selected - Create Element prompt
            Button(
                onClick = {
                    createElementSearchQuery = ""
                    showCreateElementDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ Create Element", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal 1: CREATE ELEMENT Searchbar & Context Suggestions Dialog
    if (showCreateElementDialog) {
        val suggestedRoot = getContextualSuggestions("body")
        val filteredElements = ALL_HTML_ELEMENTS.filter {
            createElementSearchQuery.isBlank() || it.contains(createElementSearchQuery.trim().lowercase())
        }
        val customQuery = createElementSearchQuery.trim().lowercase()

        AlertDialog(
            onDismissRequest = { showCreateElementDialog = false },
            title = { Text("Create Element", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = createElementSearchQuery,
                        onValueChange = { createElementSearchQuery = it },
                        placeholder = { Text("Search HTML element (e.g. h1, button, video)...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (createElementSearchQuery.isBlank()) {
                        Text(
                            text = "Suggested Options:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            suggestedRoot.forEach { tag ->
                                OutlinedButton(
                                    onClick = {
                                        val newText = if (tag.startsWith("h") || tag == "p" || tag == "button" || tag == "a" || tag == "li") "New $tag" else ""
                                        val newNode = HtmlDomParser.createNode(tag, textContent = newText)
                                        val parent = selectedNode ?: doc.bodyNode
                                        parent.children.add(newNode)
                                        selectedNodeId = newNode.nodeId
                                        updateDocumentAndEmit()
                                        showCreateElementDialog = false
                                        createElementSearchQuery = ""
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("<$tag>", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "All HTML Tags:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        if (customQuery.isNotEmpty() && !ALL_HTML_ELEMENTS.contains(customQuery)) {
                            item {
                                Card(
                                    onClick = {
                                        val newNode = HtmlDomParser.createNode(customQuery)
                                        val parent = selectedNode ?: doc.bodyNode
                                        parent.children.add(newNode)
                                        selectedNodeId = newNode.nodeId
                                        updateDocumentAndEmit()
                                        showCreateElementDialog = false
                                        createElementSearchQuery = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("<$customQuery>", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Add Custom Tag", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        items(filteredElements) { tag ->
                            Card(
                                onClick = {
                                    val newText = if (tag.startsWith("h") || tag == "p" || tag == "button" || tag == "a" || tag == "li") "New $tag" else ""
                                    val newNode = HtmlDomParser.createNode(tag, textContent = newText)
                                    val parent = selectedNode ?: doc.bodyNode
                                    parent.children.add(newNode)
                                    selectedNodeId = newNode.nodeId
                                    updateDocumentAndEmit()
                                    showCreateElementDialog = false
                                    createElementSearchQuery = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("<$tag>", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Tap to insert", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreateElementDialog = false }) {
                    Text("Cancel", fontSize = 15.sp)
                }
            }
        )
    }

    // Modal 2: SHOW CONTEXTUAL PROPERTIES Sheet
    if (showPropertiesSheet && selectedNode != null) {
        val lineInHtml = HtmlDomParser.findLineNumberInHtml(htmlSource, selectedNode)
        val tagLower = selectedNode.tagName.lowercase()

        val isTextElement = tagLower in setOf("h1", "h2", "h3", "h4", "h5", "h6", "p", "span", "b", "i", "strong", "em", "small", "blockquote", "button", "a", "label", "li", "td", "th")
        val isMediaElement = tagLower in setOf("img", "video", "audio")
        val isLinkElement = tagLower == "a"
        val isInputElement = tagLower in setOf("input", "textarea", "select")

        var tempTag by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.tagName) }
        var tempText by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.textContent) }
        var tempId by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes["id"].orEmpty()) }
        var tempClass by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes["class"].orEmpty()) }
        var tempSrc by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes["src"].orEmpty()) }
        var tempAlt by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes["alt"].orEmpty()) }
        var tempHref by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes["href"].orEmpty()) }
        var tempTarget by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes["target"].orEmpty()) }
        var tempControls by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes.containsKey("controls")) }
        var tempAutoplay by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes.containsKey("autoplay")) }
        var tempLoop by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.attributes.containsKey("loop")) }

        var tempWidth by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("width")) }
        var tempHeight by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("height")) }
        var tempTransform by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("transform")) }
        var tempColor by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("color")) }
        var tempBgColor by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("background-color")) }
        var tempFontSize by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("font-size")) }
        var tempMargin by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("margin")) }
        var tempPadding by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("padding")) }
        var tempTextAlign by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("text-align")) }
        var tempFontWeight by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("font-weight")) }
        var tempFontFamily by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("font-family")) }
        var tempDisplay by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("display")) }
        var tempOpacity by remember(selectedNode.nodeId) { mutableStateOf(selectedNode.getStyle("opacity")) }

        val fontPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { fontUri ->
            if (fontUri != null) {
                scope.launch(Dispatchers.IO) {
                    val importedFontName = repository.importFontToProject(projectId, fontUri, context)
                    tempFontFamily = "'$importedFontName', sans-serif"
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showPropertiesSheet = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Properties: <$tempTag>", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            showPropertiesSheet = false
                            onJumpToCodeRequested("index.html", lineInHtml)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Code (Line $lineInHtml)", fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = tempTag,
                        onValueChange = { tempTag = it },
                        label = { Text("Tag Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Contextual Attributes
                    if (isMediaElement) {
                        Button(
                            onClick = {
                                onRequestPickMedia { relativePath ->
                                    tempSrc = relativePath
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📷 Elegir/Agregar MEDIA del dispositivo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = tempSrc,
                            onValueChange = { tempSrc = it },
                            label = { Text("Source URL / Asset (src)") },
                            placeholder = { Text("image.png or video.mp4") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (tagLower == "img") {
                        OutlinedTextField(
                            value = tempAlt,
                            onValueChange = { tempAlt = it },
                            label = { Text("Alt Text / Fallback (alt)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isLinkElement) {
                        OutlinedTextField(
                            value = tempHref,
                            onValueChange = { tempHref = it },
                            label = { Text("Link Destination URL (href)") },
                            placeholder = { Text("https://example.com or page.html") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tempTarget == "_blank", onCheckedChange = { checked -> tempTarget = if (checked) "_blank" else "" })
                            Text("Open link in new tab (target='_blank')", fontSize = 12.sp)
                        }
                    }

                    if (tagLower == "video" || tagLower == "audio") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tempControls, onCheckedChange = { tempControls = it })
                            Text("Show Playback Controls", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tempAutoplay, onCheckedChange = { tempAutoplay = it })
                            Text("Autoplay Media", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tempLoop, onCheckedChange = { tempLoop = it })
                            Text("Loop Media", fontSize = 12.sp)
                        }
                    }

                    if (!CANNOT_HAVE_CHILDREN.contains(tagLower) && !isMediaElement) {
                        OutlinedTextField(
                            value = tempText,
                            onValueChange = { tempText = it },
                            label = { Text("Text Content") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = tempId,
                        onValueChange = { tempId = it },
                        label = { Text("Element ID (#id)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tempClass,
                        onValueChange = { tempClass = it },
                        label = { Text("Classes (.class)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider()

                    Text("Styles & Geometry", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)

                    // Numeric Steppers for Width and Height
                    NumericStepperField(
                        value = tempWidth,
                        label = "Width",
                        unit = "px",
                        step = 10,
                        onValueChange = { tempWidth = it }
                    )

                    NumericStepperField(
                        value = tempHeight,
                        label = "Height",
                        unit = "px",
                        step = 10,
                        onValueChange = { tempHeight = it }
                    )

                    NumericStepperField(
                        value = tempMargin,
                        label = "Margin",
                        unit = "px",
                        step = 2,
                        onValueChange = { tempMargin = it }
                    )

                    NumericStepperField(
                        value = tempPadding,
                        label = "Padding",
                        unit = "px",
                        step = 2,
                        onValueChange = { tempPadding = it }
                    )

                    // Font Family & Size Controls (Only shown for text-bearing tags)
                    if (isTextElement) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Font Family:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { fontPickerLauncher.launch("*/*") },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("➕ Import Font (.ttf/.otf)", fontSize = 11.sp)
                                }
                            }
                            val importedFontList = repository.getImportedFontNames(projectId)
                            val availableFonts = listOf("system-ui", "Georgia", "serif", "sans-serif", "monospace", "cursive") + importedFontList

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                availableFonts.forEach { font ->
                                    val fontClean = font.trim('\'', '"')
                                    val isSel = tempFontFamily.contains(fontClean, ignoreCase = true)

                                    fun selectFont() {
                                        if (importedFontList.contains(fontClean)) {
                                            val matchCss = Regex("href=[\"']([^\"']+\\.css)[\"']").find(doc.headHtml)
                                            val targetCss = matchCss?.groupValues?.get(1) ?: "style.css"
                                            scope.launch(Dispatchers.IO) {
                                                repository.ensureFontFaceInCss(projectId, targetCss, fontClean)
                                            }
                                        }
                                        tempFontFamily = "'$fontClean', sans-serif"
                                    }

                                    if (isSel) {
                                        Button(onClick = { selectFont() }, shape = RoundedCornerShape(8.dp)) {
                                            Text(fontClean, fontSize = 11.sp)
                                        }
                                    } else {
                                        OutlinedButton(onClick = { selectFont() }, shape = RoundedCornerShape(8.dp)) {
                                            Text(fontClean, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        NumericStepperField(
                            value = tempFontSize,
                            label = "Font Size",
                            unit = "px",
                            step = 1,
                            onValueChange = { tempFontSize = it }
                        )

                        // Text Color Swatch Picker
                        Column {
                            Text("Text Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            ColorSwatchRow(currentColor = tempColor, onColorSelected = { tempColor = it })
                            OutlinedTextField(
                                value = tempColor,
                                onValueChange = { tempColor = it },
                                label = { Text("Text Color (Hex / Name)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Text Alignment Selector Chips
                        Column {
                            Text("Text Align:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("left" to "◄ Left", "center" to "≡ Center", "right" to "► Right", "justify" to "≣ Justify").forEach { (alignKey, label) ->
                                    val isSel = tempTextAlign.equals(alignKey, ignoreCase = true)
                                    if (isSel) {
                                        Button(onClick = { tempTextAlign = alignKey }, shape = RoundedCornerShape(8.dp)) {
                                            Text(label, fontSize = 11.sp)
                                        }
                                    } else {
                                        OutlinedButton(onClick = { tempTextAlign = alignKey }, shape = RoundedCornerShape(8.dp)) {
                                            Text(label, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Font Weight Selector Chips
                        Column {
                            Text("Font Weight:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("normal", "bold", "300", "500", "600", "800").forEach { weight ->
                                    val isSel = tempFontWeight.equals(weight, ignoreCase = true)
                                    if (isSel) {
                                        Button(onClick = { tempFontWeight = weight }, shape = RoundedCornerShape(8.dp)) {
                                            Text(weight, fontSize = 11.sp)
                                        }
                                    } else {
                                        OutlinedButton(onClick = { tempFontWeight = weight }, shape = RoundedCornerShape(8.dp)) {
                                            Text(weight, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Background Color Swatch Picker
                    Column {
                        Text("Background Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        ColorSwatchRow(currentColor = tempBgColor, onColorSelected = { tempBgColor = it })
                        OutlinedTextField(
                            value = tempBgColor,
                            onValueChange = { tempBgColor = it },
                            label = { Text("Background Color (Hex / Name)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Display Mode Selector Chips
                    Column {
                        Text("Display:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("block", "flex", "grid", "inline-block", "none").forEach { disp ->
                                val isSel = tempDisplay.equals(disp, ignoreCase = true)
                                if (isSel) {
                                    Button(onClick = { tempDisplay = disp }, shape = RoundedCornerShape(8.dp)) {
                                        Text(disp, fontSize = 11.sp)
                                    }
                                } else {
                                    OutlinedButton(onClick = { tempDisplay = disp }, shape = RoundedCornerShape(8.dp)) {
                                        Text(disp, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Opacity / Transparency Controls (for overlays)
                    Column {
                        Text("Opacity / Transparency:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("1" to "100% Solid", "0.7" to "70%", "0.3" to "30%", "0" to "0% Invisible").forEach { (opVal, label) ->
                                val isSel = tempOpacity == opVal
                                if (isSel) {
                                    Button(onClick = { tempOpacity = opVal }, shape = RoundedCornerShape(8.dp)) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                } else {
                                    OutlinedButton(onClick = { tempOpacity = opVal }, shape = RoundedCornerShape(8.dp)) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempTransform,
                        onValueChange = { tempTransform = it },
                        label = { Text("Transform / Rotation (e.g. rotate(45deg))") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanTag = tempTag.lowercase().trim()
                        selectedNode.tagName = cleanTag
                        selectedNode.textContent = tempText

                        if (tempId.isBlank()) selectedNode.attributes.remove("id") else selectedNode.attributes["id"] = tempId.trim()
                        if (tempClass.isBlank()) selectedNode.attributes.remove("class") else selectedNode.attributes["class"] = tempClass.trim()

                        if (cleanTag == "img" || cleanTag == "video" || cleanTag == "audio") {
                            if (tempSrc.isBlank()) selectedNode.attributes.remove("src") else selectedNode.attributes["src"] = tempSrc.trim()
                        }
                        if (cleanTag == "img") {
                            if (tempAlt.isBlank()) selectedNode.attributes.remove("alt") else selectedNode.attributes["alt"] = tempAlt.trim()
                        }
                        if (cleanTag == "a") {
                            if (tempHref.isBlank()) selectedNode.attributes.remove("href") else selectedNode.attributes["href"] = tempHref.trim()
                            if (tempTarget.isBlank()) selectedNode.attributes.remove("target") else selectedNode.attributes["target"] = tempTarget.trim()

                            // Auto-set display: inline-block for <a> links when dimensions or opacity are specified
                            if (tempWidth.isNotBlank() || tempHeight.isNotBlank() || tempOpacity.isNotBlank() || tempMargin.isNotBlank() || tempPadding.isNotBlank()) {
                                if (tempDisplay.isBlank() || tempDisplay == "inline") {
                                    tempDisplay = "inline-block"
                                }
                            }
                        }
                        if (cleanTag == "video" || cleanTag == "audio") {
                            if (tempControls) selectedNode.attributes["controls"] = "" else selectedNode.attributes.remove("controls")
                            if (tempAutoplay) selectedNode.attributes["autoplay"] = "" else selectedNode.attributes.remove("autoplay")
                            if (tempLoop) selectedNode.attributes["loop"] = "" else selectedNode.attributes.remove("loop")
                        }

                        selectedNode.setStyle("width", tempWidth)
                        selectedNode.setStyle("height", tempHeight)
                        selectedNode.setStyle("transform", tempTransform)
                        selectedNode.setStyle("color", tempColor)
                        selectedNode.setStyle("background-color", tempBgColor)
                        selectedNode.setStyle("font-size", tempFontSize)
                        selectedNode.setStyle("font-family", tempFontFamily)
                        selectedNode.setStyle("margin", tempMargin)
                        selectedNode.setStyle("padding", tempPadding)
                        selectedNode.setStyle("text-align", tempTextAlign)
                        selectedNode.setStyle("font-weight", tempFontWeight)
                        selectedNode.setStyle("display", tempDisplay)
                        selectedNode.setStyle("opacity", tempOpacity)

                        updateDocumentAndEmit()
                        showPropertiesSheet = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("✓ Finish / Apply Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPropertiesSheet = false }) {
                    Text("Cancel", fontSize = 15.sp)
                }
            }
        )
    }

    // Modal 3: NEW CHILD Selector with Contextual Suggestions & Search
    if (showNewChildDialog && selectedNode != null) {
        val parentTag = selectedNode.tagName.lowercase()
        val suggestions = getContextualSuggestions(parentTag)
        val filteredChildList = ALL_HTML_ELEMENTS.filter {
            childSearchQuery.isBlank() || it.contains(childSearchQuery.trim().lowercase())
        }
        val customChildQuery = childSearchQuery.trim().lowercase()

        AlertDialog(
            onDismissRequest = { showNewChildDialog = false },
            title = { Text("Add Child to <${selectedNode.tagName}>", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = childSearchQuery,
                        onValueChange = { childSearchQuery = it },
                        placeholder = { Text("Search HTML element (e.g. li, video, div)...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (childSearchQuery.isBlank()) {
                        Text(
                            text = "Suggested for <$parentTag>:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            suggestions.forEach { tag ->
                                OutlinedButton(
                                    onClick = {
                                        val newText = if (tag.startsWith("h") || tag == "p" || tag == "button" || tag == "a" || tag == "li" || tag == "td" || tag == "th") "Child $tag" else ""
                                        val childNode = HtmlDomParser.createNode(tag, textContent = newText)
                                        selectedNode.children.add(childNode)
                                        selectedNodeId = childNode.nodeId
                                        updateDocumentAndEmit()
                                        showNewChildDialog = false
                                        childSearchQuery = ""
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("<$tag>", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "All HTML Tags:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        if (customChildQuery.isNotEmpty() && !ALL_HTML_ELEMENTS.contains(customChildQuery)) {
                            item {
                                Card(
                                    onClick = {
                                        val childNode = HtmlDomParser.createNode(customChildQuery)
                                        selectedNode.children.add(childNode)
                                        selectedNodeId = childNode.nodeId
                                        updateDocumentAndEmit()
                                        showNewChildDialog = false
                                        childSearchQuery = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("<$customChildQuery>", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Add Custom Child", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        items(filteredChildList) { tag ->
                            Card(
                                onClick = {
                                    val newText = if (tag.startsWith("h") || tag == "p" || tag == "button" || tag == "a" || tag == "li" || tag == "td" || tag == "th") "Child $tag" else ""
                                    val childNode = HtmlDomParser.createNode(tag, textContent = newText)
                                    selectedNode.children.add(childNode)
                                    selectedNodeId = childNode.nodeId
                                    updateDocumentAndEmit()
                                    showNewChildDialog = false
                                    childSearchQuery = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("<$tag>", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Tap to insert", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChildDialog = false }) {
                    Text("Cancel", fontSize = 15.sp)
                }
            }
        )
    }
}