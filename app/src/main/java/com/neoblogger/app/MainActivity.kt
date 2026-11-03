package com.neoblogger.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.neoblogger.app.data.ProjectFile
import com.neoblogger.app.data.ProjectRepository
import com.neoblogger.app.data.WebsiteProject
import com.neoblogger.app.editor.CodeEditorState
import com.neoblogger.app.storage.AdPreferences
import com.neoblogger.app.ui.screens.AssetManagerDialog
import com.neoblogger.app.ui.screens.DashboardScreen
import com.neoblogger.app.ui.screens.LaunchAdDialog
import com.neoblogger.app.ui.screens.WorkspaceScreen
import com.neoblogger.app.ui.theme.NeobloggerTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var repository: ProjectRepository
    private lateinit var adPreferences: AdPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        repository = ProjectRepository(File(filesDir, "projects"))
        adPreferences = AdPreferences(this)

        setContent {
            NeobloggerTheme {
                NeobloggerMainApp(
                    repository = repository,
                    adPreferences = adPreferences
                )
            }
        }
    }
}

@Composable
private fun NeobloggerMainApp(
    repository: ProjectRepository,
    adPreferences: AdPreferences,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var projects by remember { mutableStateOf<List<WebsiteProject>>(emptyList()) }
    var selectedProject by remember { mutableStateOf<WebsiteProject?>(null) }
    var selectedFile by remember { mutableStateOf("index.html") }
    var projectFiles by remember { mutableStateOf<List<ProjectFile>>(emptyList()) }

    var activeExportProjectId by remember { mutableStateOf("") }

    val editorState = remember { CodeEditorState("") }
    var savedSource by remember { mutableStateOf("") }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    var showLaunchAd by remember { mutableStateOf(adPreferences.shouldAttemptLaunchAd()) }
    var showAssetManager by remember { mutableStateOf(false) }

    fun refreshProjects() {
        scope.launch {
            projects = withContext(Dispatchers.IO) { repository.listProjects() }
        }
    }

    fun refreshProjectFiles(projectId: String) {
        scope.launch {
            projectFiles = withContext(Dispatchers.IO) { repository.listProjectFiles(projectId) }
        }
    }

    fun loadFile(projectId: String, fileName: String) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.readFile(projectId, fileName) }
            }.onSuccess { contents ->
                selectedFile = fileName
                editorState.reset(contents)
                savedSource = contents
                saveStatus = null
            }.onFailure {
                saveStatus = "Could not open file $fileName"
            }
        }
    }

    fun saveCurrentFile() {
        val proj = selectedProject ?: return
        val currentFileName = selectedFile
        val contents = editorState.currentText

        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.saveFile(proj.id, currentFileName, contents)
                }
            }.onSuccess {
                savedSource = contents
                saveStatus = "Saved"
                refreshProjectFiles(proj.id)
            }.onFailure {
                saveStatus = "Could not save file"
            }
        }
    }

    fun createProject(name: String) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.createProject(name) }
            }.onSuccess { newProject ->
                refreshProjects()
                selectedProject = newProject
                selectedFile = "index.html"
                refreshProjectFiles(newProject.id)
                loadFile(newProject.id, "index.html")
            }.onFailure {
                Toast.makeText(context, "Failed to create project", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteProject(project: WebsiteProject) {
        scope.launch {
            withContext(Dispatchers.IO) { repository.deleteProject(project.id) }
            refreshProjects()
            if (selectedProject?.id == project.id) {
                selectedProject = null
            }
        }
    }

    // SAF Activity Result Launchers
    val importFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        repository.importProjectFromFolderUri(context, uri, "Imported Folder Site")
                    }
                }.onSuccess { importedProj ->
                    refreshProjects()
                    selectedProject = importedProj
                    selectedFile = "index.html"
                    refreshProjectFiles(importedProj.id)
                    loadFile(importedProj.id, "index.html")
                    snackbarHostState.showSnackbar("Folder website imported successfully.")
                }.onFailure {
                    snackbarHostState.showSnackbar("Folder Import failed: ${it.localizedMessage}")
                }
            }
        }
    }

    val importZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        repository.importProjectFromZipUri(context, uri, "Imported Zip Site")
                    }
                }.onSuccess { importedProj ->
                    refreshProjects()
                    selectedProject = importedProj
                    selectedFile = "index.html"
                    refreshProjectFiles(importedProj.id)
                    loadFile(importedProj.id, "index.html")
                    snackbarHostState.showSnackbar("ZIP website imported successfully.")
                }.onFailure {
                    snackbarHostState.showSnackbar("ZIP Import failed: ${it.localizedMessage}")
                }
            }
        }
    }

    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && activeExportProjectId.isNotBlank()) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        repository.exportProjectToZipUri(context, activeExportProjectId, uri)
                    }
                }.onSuccess {
                    snackbarHostState.showSnackbar("Website exported as ZIP successfully.")
                }.onFailure {
                    snackbarHostState.showSnackbar("ZIP Export failed: ${it.localizedMessage}")
                }
            }
        }
    }

    val exportFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && activeExportProjectId.isNotBlank()) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        repository.exportProjectToFolderUri(context, activeExportProjectId, uri)
                    }
                }.onSuccess {
                    snackbarHostState.showSnackbar("Website exported to folder successfully.")
                }.onFailure {
                    snackbarHostState.showSnackbar("Folder Export failed: ${it.localizedMessage}")
                }
            }
        }
    }

    val addAssetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val proj = selectedProject ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "image.png"
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            repository.addProjectAsset(proj.id, fileName, stream)
                        }
                    }
                }.onSuccess {
                    refreshProjectFiles(proj.id)
                    snackbarHostState.showSnackbar("Asset added: $fileName")
                }
            }
        }
    }

    fun createNewFileInProject(fileName: String) {
        val proj = selectedProject ?: return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.createProjectFile(proj.id, fileName, "<!-- $fileName -->\n")
                }
            }.onSuccess {
                refreshProjectFiles(proj.id)
                loadFile(proj.id, fileName)
            }
        }
    }

    fun deleteFileInProject(fileName: String) {
        val proj = selectedProject ?: return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.deleteProjectFile(proj.id, fileName) }
            }.onSuccess {
                refreshProjectFiles(proj.id)
                if (selectedFile == fileName) {
                    loadFile(proj.id, "index.html")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshProjects()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val currentProject = selectedProject
            if (currentProject == null) {
                DashboardScreen(
                    projects = projects,
                    onCreateProject = { name -> createProject(name) },
                    onImportFolderClick = {
                        importFolderLauncher.launch(null)
                    },
                    onImportZipClick = {
                        importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                    },
                    onOpenProject = { project ->
                        selectedProject = project
                        selectedFile = "index.html"
                        refreshProjectFiles(project.id)
                        loadFile(project.id, "index.html")
                    },
                    onDeleteProject = { project -> deleteProject(project) }
                )
            } else {
                WorkspaceScreen(
                    project = currentProject,
                    selectedFile = selectedFile,
                    editorState = editorState,
                    savedSource = savedSource,
                    saveStatus = saveStatus,
                    projectFiles = projectFiles,
                    repository = repository,
                    onBackToProjectsRequested = { selectedProject = null },
                    onSelectFile = { fileName -> loadFile(currentProject.id, fileName) },
                    onSourceChange = { saveStatus = null },
                    onSaveCurrentFile = { saveCurrentFile() },
                    onCreateNewFile = { name -> createNewFileInProject(name) },
                    onDeleteFile = { name -> deleteFileInProject(name) },
                    onExportZipRequested = {
                        activeExportProjectId = currentProject.id
                        exportZipLauncher.launch("${currentProject.name}.zip")
                    },
                    onExportFolderRequested = {
                        activeExportProjectId = currentProject.id
                        exportFolderLauncher.launch(null)
                    },
                    onOpenAssetManager = { showAssetManager = true }
                )
            }

            if (showLaunchAd) {
                LaunchAdDialog(
                    onDismiss = { showLaunchAd = false }
                )
            }

            if (showAssetManager && currentProject != null) {
                AssetManagerDialog(
                    projectFiles = projectFiles,
                    onAddAssetClick = {
                        addAssetLauncher.launch(arrayOf("image/*"))
                    },
                    onDeleteAsset = { relativePath ->
                        deleteFileInProject(relativePath)
                    },
                    onDismiss = { showAssetManager = false }
                )
            }
        }
    }
}