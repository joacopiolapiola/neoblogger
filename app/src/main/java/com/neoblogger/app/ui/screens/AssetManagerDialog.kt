package com.neoblogger.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoblogger.app.R
import com.neoblogger.app.data.ProjectFile

@Composable
fun AssetManagerDialog(
    projectFiles: List<ProjectFile>,
    onAddAssetClick: () -> Unit,
    onDeleteAsset: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val assets = projectFiles.filter { !it.isDirectory && (it.extension in ProjectFile.IMAGE_EXTENSIONS || it.relativePath.startsWith("assets/")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.assets), style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = onAddAssetClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ " + stringResource(R.string.add_asset), fontSize = 13.sp)
                }
            }
        },
        text = {
            if (assets.isEmpty()) {
                Text("No assets found in project.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    items(assets) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.relativePath, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${file.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { onDeleteAsset(file.relativePath) }) {
                                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), fontSize = 15.sp)
            }
        }
    )
}