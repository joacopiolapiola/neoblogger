package com.neoblogger.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoblogger.app.R
import com.neoblogger.app.data.WebsiteProject

@Composable
fun DashboardScreen(
    projects: List<WebsiteProject>,
    onCreateProject: (String) -> Unit,
    onImportFolderClick: () -> Unit,
    onImportZipClick: () -> Unit,
    onCloneSiteClick: () -> Unit,
    onOpenProject: (WebsiteProject) -> Unit,
    onDeleteProject: (WebsiteProject) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<WebsiteProject?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.projects_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Large action buttons for primary project workflows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(stringResource(R.string.new_project), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onImportFolderClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(stringResource(R.string.import_folder), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onImportZipClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text(stringResource(R.string.import_zip), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onCloneSiteClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("🌐 Importar / Clonar Sitio desde URL (Neocities)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (projects.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = stringResource(R.string.empty_projects),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(projects, key = { it.id }) { project ->
                    Card(
                        onClick = { onOpenProject(project) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "index.html • style.css • script.js",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(
                                onClick = { projectToDelete = project }
                            ) {
                                Text(
                                    text = stringResource(R.string.delete),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.new_project), style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(R.string.project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (projectName.isNotBlank()) {
                            onCreateProject(projectName)
                            projectName = ""
                            showCreateDialog = false
                        }
                    },
                    enabled = projectName.isNotBlank()
                ) {
                    Text(stringResource(R.string.create), fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.cancel), fontSize = 14.sp)
                }
            }
        )
    }

    projectToDelete?.let { targetProject ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text(stringResource(R.string.delete_project), style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(stringResource(R.string.delete_project_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(targetProject)
                        projectToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text(stringResource(R.string.cancel), fontSize = 14.sp)
                }
            }
        )
    }
}