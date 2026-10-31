package com.neoblogger.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoblogger.app.R
import com.neoblogger.app.data.ProjectFile
import com.neoblogger.app.data.ProjectRepository
import com.neoblogger.app.data.WebsiteProject
import com.neoblogger.app.editor.CodeEditorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProjectTab { DESIGN, CODE, PREVIEW }

@Composable
fun WorkspaceScreen(
    repository: ProjectRepository,
    project: WebsiteProject,
    selectedFile: String,
    editorState: CodeEditorState,
    savedSource: String,
    saveStatus: String?,
    projectFiles: List<ProjectFile>,
    onBackToProjectsRequested: () -> Unit,
    onSelectFile: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onSaveCurrentFile: () -> Unit,
    onCreateNewFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onExportZipRequested: () -> Unit,
    onExportFolderRequested: () -> Unit,
    onOpenAssetManager: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ProjectTab.DESIGN) }
    var previewRevision by remember { mutableIntStateOf(0) }
    var showLeavePrompt by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }

    var activeHtmlSource by remember { mutableStateOf("") }
    var onMediaPickedCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    repository.copyAssetToProject(project.id, uri, context)
                }.onSuccess { relativePath ->
                    withContext(Dispatchers.Main) {
                        onMediaPickedCallback?.invoke(relativePath)
                        onMediaPickedCallback = null
                    }
                }
            }
        }
    }

    val requestPickMedia: ((String) -> Unit) -> Unit = { callback ->
        onMediaPickedCallback = callback
        mediaPickerLauncher.launch("*/*")
    }

    var activeHtmlFile by remember { mutableStateOf("index.html") }

    LaunchedEffect(selectedFile, editorState.currentText) {
        if (selectedFile.endsWith(".html")) {
            activeHtmlFile = selectedFile
            activeHtmlSource = editorState.currentText
        }
    }

    val designHtmlInput = if (selectedFile.endsWith(".html")) {
        editorState.currentText
    } else if (activeHtmlSource.isNotBlank()) {
        activeHtmlSource
    } else {
        runCatching { repository.readFile(project.id, activeHtmlFile) }.getOrDefault("<!doctype html>\n<html lang=\"en\">\n<body>\n  <main>\n    <h1>Welcome to Neoblogger</h1>\n  </main>\n</body>\n</html>")
    }

    val hasUnsavedChanges = editorState.currentText != savedSource

    BackHandler {
        if (hasUnsavedChanges) {
            showLeavePrompt = true
        } else {
            onBackToProjectsRequested()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(modifier = Modifier.height(68.dp)) {
                ProjectTab.entries.forEach { tab ->
                    val label = when (tab) {
                        ProjectTab.DESIGN -> stringResource(R.string.design)
                        ProjectTab.CODE -> stringResource(R.string.code)
                        ProjectTab.PREVIEW -> stringResource(R.string.preview)
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Text(
                                text = when (tab) {
                                    ProjectTab.DESIGN -> "🎨"
                                    ProjectTab.CODE -> "💻"
                                    ProjectTab.PREVIEW -> "👁️"
                                },
                                fontSize = 18.sp
                            )
                        },
                        label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { insets ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = insets.calculateTopPadding()),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
                // Top Workspace Header with larger touch targets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (hasUnsavedChanges) showLeavePrompt = true else onBackToProjectsRequested()
                        }
                    ) {
                        Text("← " + stringResource(R.string.back_to_projects), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row {
                        OutlinedButton(
                            onClick = { showExportMenu = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.export_project), fontSize = 13.sp)
                        }

                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_zip), fontSize = 14.sp) },
                                onClick = {
                                    showExportMenu = false
                                    onExportZipRequested()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_folder), fontSize = 14.sp) },
                                onClick = {
                                    showExportMenu = false
                                    onExportFolderRequested()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.assets), fontSize = 14.sp) },
                                onClick = {
                                    showExportMenu = false
                                    onOpenAssetManager()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🚀 Publish / Deploy", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showExportMenu = false
                                    showPublishDialog = true
                                }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    ProjectTab.DESIGN -> {
                        DesignTabScreen(
                            htmlSource = designHtmlInput,
                            projectId = project.id,
                            repository = repository,
                            onHtmlSourceChanged = { updatedHtml ->
                                activeHtmlSource = updatedHtml
                                val targetHtmlFile = if (selectedFile.endsWith(".html")) selectedFile else activeHtmlFile
                                if (selectedFile == targetHtmlFile) {
                                    onSourceChange(updatedHtml)
                                } else {
                                    scope.launch(Dispatchers.IO) {
                                        runCatching { repository.saveFile(project.id, targetHtmlFile, updatedHtml) }
                                    }
                                }
                            },
                            onJumpToCodeRequested = { targetFile, targetLine ->
                                onSelectFile(targetFile)
                                selectedTab = ProjectTab.CODE
                                editorState.moveCursorToLine(targetLine)
                            },
                            onRequestPickMedia = requestPickMedia,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ProjectTab.CODE -> {
                        CodeTabScreen(
                            currentFileName = selectedFile,
                            editorState = editorState,
                            savedSource = savedSource,
                            saveStatus = saveStatus,
                            projectFiles = projectFiles,
                            onSelectFile = onSelectFile,
                            onSourceChange = onSourceChange,
                            onSave = onSaveCurrentFile,
                            onCreateNewFile = onCreateNewFile,
                            onDeleteFile = onDeleteFile,
                            onRequestPickMedia = requestPickMedia,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ProjectTab.PREVIEW -> {
                        PreviewTabScreen(
                            repository = repository,
                            project = project,
                            revision = previewRevision,
                            hasUnsavedChanges = hasUnsavedChanges,
                            onReloadRequested = { previewRevision++ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showPublishDialog) {
        PublishDialog(
            projectId = project.id,
            repository = repository,
            onDismissRequest = { showPublishDialog = false }
        )
    }

    if (showLeavePrompt) {
        AlertDialog(
            onDismissRequest = { showLeavePrompt = false },
            title = { Text(stringResource(R.string.unsaved_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(R.string.unsaved_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeavePrompt = false
                    onBackToProjectsRequested()
                }) {
                    Text(stringResource(R.string.leave), fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeavePrompt = false }) {
                    Text(stringResource(R.string.cancel), fontSize = 14.sp)
                }
            }
        )
    }
}