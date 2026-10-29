package com.neoblogger.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

    private var activeProjectIdForExport: String? = null
    private var onImportFolderSelected: ((Uri) -> Unit)? = null
    private var onImportZipSelected: ((Uri) -> Unit)? = null
    private var onExportZipSelected: ((Uri) -> Unit)? = null
    private var onExportFolderSelected: ((Uri) -> Unit)? = null
    private var onAddAssetSelected: ((Uri) -> Unit)? = null

    private val importFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { onImportFolderSelected?.invoke(it) }
    }

    private val importZipLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImportZipSelected?.invoke(it) }
    }

    private val exportZipLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { onExportZipSelected?.invoke(it) }
    }

    private val exportFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { onExportFolderSelected?.invoke(it) }
    }

    private val addAssetLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onAddAssetSelected?.invoke(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        repository = ProjectRepository(File(filesDir, "projects"))
        adPreferences = AdPreferences(this)

        setContent {
            NeobloggerTheme {
                NeobloggerMainApp(
                    repository = repository,
                    adPreferences = adPreferences,
                    onRequestImportFolder = { onSelected ->
                        onImportFolderSelected = onSelected
                        importFolderLauncher.launch(null)
                    },
                    onRequestImportZip = { onSelected ->
                        onImportZipSelected = onSelected
                        importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                    },
                    onRequestExportZip = { projectId, defaultName, onSelected ->
                        activeProjectIdForExport = projectId
                        onExportZipSelected = onSelected
                        exportZipLauncher.launch("$defaultName.zip")
                    },
                    onRequestExportFolder = { projectId, onSelected ->
                        activeProjectIdForExport = projectId
                        onExportFolderSelected = onSelected
                        exportFolderLauncher.launch(null)
                    },
                    onRequestAddAsset = { onSelected ->
                        onAddAssetSelected = onSelected
                        addAssetLauncher.launch(arrayOf("image/*"))
                    }
                )
            }
        }
    }
}

@Composable
private fun NeobloggerMainApp(
    repository: ProjectRepository,
    adPreferences: AdPreferences,
    onRequestImportFolder: ((Uri) -> Unit) -> Unit,
    onRequestImportZip: ((Uri) -> Unit) -> Unit,
    onRequestExportZip: (String, String, (Uri) -> Unit) -> Unit,
    onRequestExportFolder: (String, (Uri) -> Unit) -> Unit,
    onRequestAddAsset: ((Uri) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var projects by remember { mutableStateOf<List<WebsiteProject>>(emptyList()) }
    var selectedProject by remember { mutableStateOf<WebsiteProject?>(null) }
    var selectedFile by remember { mutableStateOf("index.html") }
    var projectFiles by remember { mutableStateOf<List<ProjectFile>>(emptyList()) }

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

    fun handleImportZip(uri: Uri) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        repository.importProjectFromZip(inputStream, "Imported Site")
                    } ?: throw IllegalStateException("Cannot open zip stream.")
                }
            }.onSuccess { importedProj ->
                refreshProjects()
                selectedProject = importedProj
                selectedFile = importedProj.entryPage
                refreshProjectFiles(importedProj.id)
                loadFile(importedProj.id, importedProj.entryPage)
                snackbarHostState.showSnackbar("Website imported successfully.")
            }.onFailure {
                snackbarHostState.showSnackbar("Import failed: ${it.localizedMessage}")
            }
        }
    }

    fun handleExportZip(projectId: String, targetUri: Uri) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                        repository.exportProjectToZip(projectId, outputStream)
                    } ?: throw IllegalStateException("Cannot write export stream.")
                }
            }.onSuccess {
                snackbarHostState.showSnackbar("Website exported as ZIP.")
            }.onFailure {
                snackbarHostState.showSnackbar("Export failed: ${it.localizedMessage}")
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

    fun addAssetToProject(uri: Uri) {
        val proj = selectedProject ?: return
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

    LaunchedEffect(Unit) {
        refreshProjects()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val currentProject = selectedProject
        if (currentProject == null) {
            DashboardScreen(
                projects = projects,
                onCreateProject = ::createProject,
                onImportFolderClick = {
                    onRequestImportFolder { uri ->
                        Toast.makeText(context, "Folder selected: $uri", Toast.LENGTH_SHORT).show()
                    }
                },
                onImportZipClick = {
                    onRequestImportZip { uri -> handleImportZip(uri) }
                },
                onOpenProject = { project ->
                    selectedProject = project
                    selectedFile = project.entryPage
                    refreshProjectFiles(project.id)
                    loadFile(project.id, project.entryPage)
                },
                onDeleteProject = ::deleteProject
            )
        } else {
            WorkspaceScreen(
                repository = repository,
                project = currentProject,
                selectedFile = selectedFile,
                editorState = editorState,
                savedSource = savedSource,
                saveStatus = saveStatus,
                projectFiles = projectFiles,
                onBackToProjectsRequested = {
                    selectedProject = null
                    refreshProjects()
                },
                onSelectFile = { fileName ->
                    saveCurrentFile()
                    loadFile(currentProject.id, fileName)
                },
                onSourceChange = { newSource ->
                    editorState.updateText(newSource)
                    saveStatus = null
                },
                onSaveCurrentFile = ::saveCurrentFile,
                onCreateNewFile = ::createNewFileInProject,
                onDeleteFile = ::deleteFileInProject,
                onExportZipRequested = {
                    onRequestExportZip(currentProject.id, currentProject.name) { uri ->
                        handleExportZip(currentProject.id, uri)
                    }
                },
                onExportFolderRequested = {
                    onRequestExportFolder(currentProject.id) { uri ->
                        Toast.makeText(context, "Export directory selected: $uri", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenAssetManager = { showAssetManager = true }
            )
        }
    }

    if (showLaunchAd) {
        LaunchAdDialog(
            onDismiss = {
                adPreferences.markAdSkipped()
                showLaunchAd = false
            }
        )
    }

    if (showAssetManager) {
        AssetManagerDialog(
            projectFiles = projectFiles,
            onAddAssetClick = {
                onRequestAddAsset { uri -> addAssetToProject(uri) }
            },
            onDeleteAsset = { relativePath ->
                deleteFileInProject(relativePath)
            },
            onDismiss = { showAssetManager = false }
        )
    }
}