package com.neoblogger.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoblogger.app.data.ProjectRepository
import com.neoblogger.app.data.WebsiteProject
import com.neoblogger.app.web.WebSiteCloner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CloneSiteDialog(
    repository: ProjectRepository,
    onProjectCloned: (WebsiteProject) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var urlInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isCloning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun startCloning() {
        if (urlInput.isBlank()) return
        isCloning = true
        statusMessage = "Connecting to ${urlInput.trim()}..."

        scope.launch(Dispatchers.IO) {
            runCatching {
                WebSiteCloner.cloneWebSiteFromUrl(
                    targetUrlInput = urlInput,
                    suggestedName = nameInput,
                    repository = repository
                ) { progress ->
                    scope.launch(Dispatchers.Main) {
                        statusMessage = progress
                    }
                }
            }.onSuccess { clonedProject ->
                withContext(Dispatchers.Main) {
                    statusMessage = "✅ Site cloned successfully!"
                    isCloning = false
                    onProjectCloned(clonedProject)
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    statusMessage = "❌ Cloning Failed: ${err.message}"
                    isCloning = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isCloning) onDismissRequest() },
        title = {
            Text("🌐 Import Site from URL / Neocities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Download and clone any public website or Neocities site directly into an editable project:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Site URL or Neocities Name") },
                    placeholder = { Text("e.g. my-site.neocities.org or https://example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Custom Project Name (Optional)") },
                    placeholder = { Text("e.g. My Cloned Site") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
            Button(
                onClick = { startCloning() },
                enabled = !isCloning && urlInput.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🌐 Download & Create Project", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isCloning
            ) {
                Text("Cancel", fontSize = 14.sp)
            }
        }
    )
}