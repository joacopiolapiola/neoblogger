package com.neoblogger.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoblogger.app.data.ProjectRepository
import com.neoblogger.app.storage.FtpConfig
import com.neoblogger.app.storage.NeocitiesConfig
import com.neoblogger.app.storage.PublishPreferences
import com.neoblogger.app.web.FtpPublisher
import com.neoblogger.app.web.NeocitiesPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PublishTab { FTP, NEOCITIES }

@Composable
fun PublishDialog(
    projectId: String,
    repository: ProjectRepository,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val publishPrefs = remember { PublishPreferences(context) }

    var selectedTab by remember { mutableStateOf(PublishTab.FTP) }

    var ftpHost by remember { mutableStateOf(publishPrefs.getFtpConfig(projectId).host) }
    var ftpPort by remember { mutableStateOf(publishPrefs.getFtpConfig(projectId).port.toString()) }
    var ftpUser by remember { mutableStateOf(publishPrefs.getFtpConfig(projectId).user) }
    var ftpPass by remember { mutableStateOf(publishPrefs.getFtpConfig(projectId).pass) }
    var ftpPath by remember { mutableStateOf(publishPrefs.getFtpConfig(projectId).path) }

    var neocitiesToken by remember { mutableStateOf(publishPrefs.getNeocitiesConfig(projectId).apiToken) }
    var neocitiesSite by remember { mutableStateOf(publishPrefs.getNeocitiesConfig(projectId).siteName) }

    var isPublishing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun saveConfigs() {
        val portInt = ftpPort.trim().toIntOrNull() ?: 21
        publishPrefs.saveFtpConfig(projectId, FtpConfig(ftpHost, portInt, ftpUser, ftpPass, ftpPath))
        publishPrefs.saveNeocitiesConfig(projectId, NeocitiesConfig(neocitiesToken, neocitiesSite))
    }

    fun deployFtp() {
        saveConfigs()
        isPublishing = true
        statusMessage = "Starting FTP Upload..."

        scope.launch(Dispatchers.IO) {
            runCatching {
                val projDir = repository.projectRoot(projectId)
                val config = publishPrefs.getFtpConfig(projectId)
                FtpPublisher.uploadProject(projDir, config) { progress ->
                    scope.launch(Dispatchers.Main) { statusMessage = progress }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    statusMessage = "✅ FTP Publish Successful!"
                    isPublishing = false
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    statusMessage = "❌ FTP Error: ${error.message}"
                    isPublishing = false
                }
            }
        }
    }

    fun deployNeocities() {
        saveConfigs()
        isPublishing = true
        statusMessage = "Starting Neocities Upload..."

        scope.launch(Dispatchers.IO) {
            runCatching {
                val projDir = repository.projectRoot(projectId)
                val config = publishPrefs.getNeocitiesConfig(projectId)
                NeocitiesPublisher.uploadProject(projDir, config) { progress ->
                    scope.launch(Dispatchers.Main) { statusMessage = progress }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    statusMessage = "✅ Neocities Publish Successful!"
                    isPublishing = false
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    statusMessage = "❌ Neocities Error: ${error.message}"
                    isPublishing = false
                }
            }
        }
    }

    fun deployBoth() {
        saveConfigs()
        isPublishing = true
        statusMessage = "Starting Dual Upload (FTP & Neocities)..."

        scope.launch(Dispatchers.IO) {
            val projDir = repository.projectRoot(projectId)
            val ftpConf = publishPrefs.getFtpConfig(projectId)
            val neocitiesConf = publishPrefs.getNeocitiesConfig(projectId)

            var ftpSuccess = false
            var neocitiesSuccess = false

            runCatching {
                if (ftpConf.isValid) {
                    FtpPublisher.uploadProject(projDir, ftpConf) { progress ->
                        scope.launch(Dispatchers.Main) { statusMessage = "[FTP] $progress" }
                    }
                    ftpSuccess = true
                }
            }.onFailure { err ->
                scope.launch(Dispatchers.Main) { statusMessage = "❌ FTP Error: ${err.message}" }
            }

            runCatching {
                if (neocitiesConf.isValid) {
                    NeocitiesPublisher.uploadProject(projDir, neocitiesConf) { progress ->
                        scope.launch(Dispatchers.Main) { statusMessage = "[Neocities] $progress" }
                    }
                    neocitiesSuccess = true
                }
            }.onFailure { err ->
                scope.launch(Dispatchers.Main) { statusMessage = "❌ Neocities Error: ${err.message}" }
            }

            withContext(Dispatchers.Main) {
                statusMessage = when {
                    ftpSuccess && neocitiesSuccess -> "🎉 Successfully published to BOTH FTP & Neocities!"
                    ftpSuccess -> "✅ Published to FTP! (Neocities skipped/failed)"
                    neocitiesSuccess -> "✅ Published to Neocities! (FTP skipped/failed)"
                    else -> "❌ Dual publish failed."
                }
                isPublishing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isPublishing) onDismissRequest() },
        title = {
            Text("🚀 Publish / Deploy Site", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tab Switcher (FTP vs Neocities)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTab == PublishTab.FTP) {
                        Button(onClick = {}, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Text("📁 FTP Server", fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(onClick = { selectedTab = PublishTab.FTP }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Text("📁 FTP Server", fontSize = 13.sp)
                        }
                    }

                    if (selectedTab == PublishTab.NEOCITIES) {
                        Button(onClick = {}, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Text("🐱 Neocities", fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(onClick = { selectedTab = PublishTab.NEOCITIES }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Text("🐱 Neocities", fontSize = 13.sp)
                        }
                    }
                }

                HorizontalDivider()

                when (selectedTab) {
                    PublishTab.FTP -> {
                        Text("Custom FTP Connection Settings:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = ftpHost,
                            onValueChange = { ftpHost = it },
                            label = { Text("FTP Host / IP") },
                            placeholder = { Text("e.g. ftp.example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ftpPort,
                                onValueChange = { ftpPort = it },
                                label = { Text("Port") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.4f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = ftpPath,
                                onValueChange = { ftpPath = it },
                                label = { Text("Remote Path") },
                                placeholder = { Text("/public_html/") },
                                modifier = Modifier.weight(0.6f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = ftpUser,
                            onValueChange = { ftpUser = it },
                            label = { Text("FTP Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ftpPass,
                            onValueChange = { ftpPass = it },
                            label = { Text("FTP Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    PublishTab.NEOCITIES -> {
                        Text("Neocities API Credentials:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)

                        OutlinedTextField(
                            value = neocitiesSite,
                            onValueChange = { neocitiesSite = it },
                            label = { Text("Neocities Site Name") },
                            placeholder = { Text("e.g. my-awesome-site") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = neocitiesToken,
                            onValueChange = { neocitiesToken = it },
                            label = { Text("API Key / Bearer Token") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                statusMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { deployBoth() },
                    enabled = !isPublishing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🚀 Publish to Both (FTP & Neocities)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { deployFtp() },
                        enabled = !isPublishing && ftpHost.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Publish FTP", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { deployNeocities() },
                        enabled = !isPublishing && (neocitiesToken.isNotBlank() || neocitiesSite.isNotBlank()),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Publish Neocities", fontSize = 12.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    saveConfigs()
                    onDismissRequest()
                },
                enabled = !isPublishing
            ) {
                Text("Close", fontSize = 14.sp)
            }
        }
    )
}