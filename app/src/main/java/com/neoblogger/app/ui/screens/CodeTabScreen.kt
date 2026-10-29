package com.neoblogger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoblogger.app.R
import com.neoblogger.app.data.ProjectFile
import com.neoblogger.app.editor.CodeAutocompleter
import com.neoblogger.app.editor.CodeEditorState
import com.neoblogger.app.editor.EmmetExpander

private fun getEmmetAbbreviationAtCursor(text: String, selectionStart: Int): String {
    if (selectionStart <= 0 || selectionStart > text.length) return ""
    val sub = text.substring(0, selectionStart)
    val lastIndex = sub.indexOfLast { it.isWhitespace() || it == '\n' || it == ';' || it == '<' }
    val word = if (lastIndex == -1) sub else sub.substring(lastIndex + 1)
    return word.trim()
}

private fun isCursorOnMediaTag(text: String, selectionStart: Int): Boolean {
    if (selectionStart < 0 || selectionStart > text.length) return false
    val lineStart = text.lastIndexOf('\n', (selectionStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it }
    val lineEnd = text.indexOf('\n', selectionStart).let { if (it == -1) text.length else it }
    val lineText = text.substring(lineStart, lineEnd).lowercase()
    return lineText.contains("<img") || lineText.contains("<video") || lineText.contains("<audio") || lineText.contains("<source")
}

private fun moveCursorHorizontal(editorState: CodeEditorState, delta: Int) {
    val sel = editorState.textFieldValue.selection.start
    val newPos = (sel + delta).coerceIn(0, editorState.currentText.length)
    editorState.updateTextFieldValue(
        editorState.textFieldValue.copy(selection = TextRange(newPos))
    )
}

private fun moveCursorVertical(editorState: CodeEditorState, lineDelta: Int) {
    val text = editorState.currentText
    val lines = text.lines()
    if (lines.isEmpty()) return

    val sel = editorState.textFieldValue.selection.start
    var currentLine = 0
    var accumulated = 0
    var charInLine = 0
    for ((i, line) in lines.withIndex()) {
        val lineEnd = accumulated + line.length
        if (sel in accumulated..lineEnd) {
            currentLine = i
            charInLine = sel - accumulated
            break
        }
        accumulated += line.length + 1
    }

    val targetLine = (currentLine + lineDelta).coerceIn(0, lines.size - 1)
    var targetOffset = 0
    for (i in 0 until targetLine) {
        targetOffset += lines[i].length + 1
    }
    val targetCharInLine = charInLine.coerceAtMost(lines[targetLine].length)
    val finalPos = targetOffset + targetCharInLine

    editorState.updateTextFieldValue(
        editorState.textFieldValue.copy(selection = TextRange(finalPos))
    )
}

private fun insertSymbolAtCursor(editorState: CodeEditorState, symbol: String, onSourceChange: (String) -> Unit) {
    val text = editorState.currentText
    val sel = editorState.textFieldValue.selection.start
    val newText = text.substring(0, sel) + symbol + text.substring(sel)
    val newCursor = sel + symbol.length
    editorState.updateTextFieldValue(
        TextFieldValue(text = newText, selection = TextRange(newCursor))
    )
    onSourceChange(newText)
}

@Composable
fun CodeTabScreen(
    currentFileName: String,
    editorState: CodeEditorState,
    savedSource: String,
    saveStatus: String?,
    projectFiles: List<ProjectFile>,
    onSelectFile: (String) -> Unit,
    onSourceChange: (String) -> Unit,
    onSave: () -> Unit,
    onCreateNewFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRequestPickMedia: ((String) -> Unit) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showFileExplorer by remember { mutableStateOf(false) }
    var showFindReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var emmetMessage by remember { mutableStateOf<String?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }

    val lineListState = rememberLazyListState()
    val isCssFile = currentFileName.endsWith(".css")

    val selectionStart = editorState.textFieldValue.selection.start
    val currentWord = getEmmetAbbreviationAtCursor(editorState.currentText, selectionStart)
    val detectedEmmetExpansion = if (currentWord.isNotEmpty()) EmmetExpander.expand(currentWord, isCss = isCssFile) else null
    val completions = CodeAutocompleter.getCompletions(currentWord, currentFileName)
    val isOnMediaTag = isCursorOnMediaTag(editorState.currentText, selectionStart)

    fun applyCompletion(wordToReplace: String, completion: String) {
        val text = editorState.currentText
        val sub = text.substring(0, selectionStart)
        val wordStart = sub.lastIndexOf(wordToReplace)
        if (wordStart != -1) {
            val newText = text.substring(0, wordStart) + completion + text.substring(selectionStart)
            val newCursor = wordStart + completion.length
            editorState.updateTextFieldValue(
                TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursor)
                )
            )
            onSourceChange(newText)
        }
    }

    fun expandEmmetAbbr(abbr: String, expanded: String) {
        val text = editorState.currentText
        val sub = text.substring(0, selectionStart)
        val abbrStart = sub.lastIndexOf(abbr)
        if (abbrStart != -1) {
            val newText = text.substring(0, abbrStart) + expanded + text.substring(selectionStart)
            val newCursor = abbrStart + expanded.length
            editorState.updateTextFieldValue(
                TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursor)
                )
            )
            onSourceChange(newText)
            emmetMessage = "Expanded '$abbr'"
        }
    }

    val lines = editorState.currentText.lines()
    val lineCount = lines.size

    var currentLineIndex = 0
    var accumulatedLength = 0
    for ((index, line) in lines.withIndex()) {
        accumulatedLength += line.length + 1
        if (selectionStart < accumulatedLength) {
            currentLineIndex = index
            break
        }
    }

    LaunchedEffect(currentLineIndex) {
        if (lineCount > 0) {
            lineListState.animateScrollToItem((currentLineIndex - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Top Toolbar Row 1: Dynamic File Switcher Tabs
        val textFiles = projectFiles.filter { !it.isDirectory && it.isTextFile }.map { it.relativePath }.distinct()
        val tabFiles = if (textFiles.contains(currentFileName)) textFiles else (listOf(currentFileName) + textFiles).distinct()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabFiles.forEach { file ->
                if (file == currentFileName) {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(file, fontSize = 14.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSelectFile(file) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(file, fontSize = 14.sp)
                    }
                }
            }

            OutlinedButton(
                onClick = { showFileExplorer = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.file_explorer), fontSize = 14.sp)
            }
        }

        // Top Toolbar Row 2: Action Buttons (Find, Undo, Redo, Save)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showFindReplace = !showFindReplace },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.find), fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (editorState.undo()) onSourceChange(editorState.currentText)
                    },
                    enabled = editorState.canUndo,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.undo), fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (editorState.redo()) onSourceChange(editorState.currentText)
                    },
                    enabled = editorState.canRedo,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.redo), fontSize = 13.sp)
                }
            }

            Button(
                onClick = onSave,
                enabled = editorState.currentText != savedSource,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.save), fontSize = 14.sp)
            }
        }

        if (showFindReplace) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = { findQuery = it },
                            label = { Text(stringResource(R.string.find)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            label = { Text(stringResource(R.string.replace)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            val count = editorState.findAndReplace(findQuery, replaceQuery)
                            if (count > 0) onSourceChange(editorState.currentText)
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.replace_all), fontSize = 13.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$currentFileName (Line ${currentLineIndex + 1})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = saveStatus ?: if (editorState.currentText == savedSource) stringResource(R.string.saved) else stringResource(R.string.unsaved),
                style = MaterialTheme.typography.labelMedium,
                color = if (editorState.currentText == savedSource) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Middle Code Editor Area (Takes remaining height and scrolls to cursor)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lineListState,
                    modifier = Modifier
                        .width(42.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    items(lineCount) { i ->
                        val isCurrent = i == currentLineIndex
                        Text(
                            text = "${i + 1}",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(end = 6.dp, top = 1.dp, bottom = 1.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = editorState.textFieldValue,
                    onValueChange = { newValue ->
                        editorState.updateTextFieldValue(newValue)
                        onSourceChange(newValue.text)
                        emmetMessage = null
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 2.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Floating Banner for Media Tag File Selection
        if (isOnMediaTag) {
            Card(
                onClick = {
                    onRequestPickMedia { relativePath ->
                        val text = editorState.currentText
                        val lineStart = text.lastIndexOf('\n', (selectionStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it }
                        val lineEnd = text.indexOf('\n', selectionStart).let { if (it == -1) text.length else it }
                        val lineText = text.substring(lineStart, lineEnd)

                        val updatedLine = if (lineText.contains("src=\"")) {
                            lineText.replace(Regex("src=\"[^\"]*\""), "src=\"$relativePath\"")
                        } else if (lineText.contains("src='")) {
                            lineText.replace(Regex("src='[^']*'"), "src='$relativePath'")
                        } else {
                            lineText.replace(">", " src=\"$relativePath\">")
                        }

                        val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
                        editorState.updateText(newText)
                        onSourceChange(newText)
                        emmetMessage = "Inserted src=\"$relativePath\""
                    }
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🖼️ Seleccionar archivo multimedia (Copiar al proyecto)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Button(
                        onClick = {
                            onRequestPickMedia { relativePath ->
                                val text = editorState.currentText
                                val lineStart = text.lastIndexOf('\n', (selectionStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it }
                                val lineEnd = text.indexOf('\n', selectionStart).let { if (it == -1) text.length else it }
                                val lineText = text.substring(lineStart, lineEnd)

                                val updatedLine = if (lineText.contains("src=\"")) {
                                    lineText.replace(Regex("src=\"[^\"]*\""), "src=\"$relativePath\"")
                                } else if (lineText.contains("src='")) {
                                    lineText.replace(Regex("src='[^']*'"), "src='$relativePath'")
                                } else {
                                    lineText.replace(">", " src=\"$relativePath\">")
                                }

                                val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
                                editorState.updateText(newText)
                                onSourceChange(newText)
                                emmetMessage = "Inserted src=\"$relativePath\""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Elegir 📁", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Floating Autocompletion Chips Bar
        if (completions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡 Auto:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                completions.forEach { completion ->
                    OutlinedButton(
                        onClick = { applyCompletion(currentWord, completion) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(completion, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        emmetMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        // BOTTOM ANCHORED SOFT KEYBOARD ACCESSORY BAR (Floats directly above system soft keyboard)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emmet Expansion Button (Green when available, Grey when unavailable)
                if (detectedEmmetExpansion != null) {
                    Button(
                        onClick = { expandEmmetAbbr(currentWord, detectedEmmetExpansion) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Bright Green
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("⚡ Expand '$currentWord'", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = Color.Gray)
                    ) {
                        Text("⚡ Expand Emmet", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // Cursor Arrow Navigation
                OutlinedButton(
                    onClick = { moveCursorHorizontal(editorState, -1) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(36.dp)
                ) { Text("◄", fontSize = 12.sp) }

                OutlinedButton(
                    onClick = { moveCursorHorizontal(editorState, 1) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(36.dp)
                ) { Text("►", fontSize = 12.sp) }

                OutlinedButton(
                    onClick = { moveCursorVertical(editorState, -1) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(36.dp)
                ) { Text("▲", fontSize = 12.sp) }

                OutlinedButton(
                    onClick = { moveCursorVertical(editorState, 1) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(36.dp)
                ) { Text("▼", fontSize = 12.sp) }

                // Symbol Quick Insertion Keys
                listOf(">", "<", "=", "\"", ".", ":", ";", "{", "}", "(", ")", "#", "/", "+", "*").forEach { sym ->
                    OutlinedButton(
                        onClick = { insertSymbolAtCursor(editorState, sym, onSourceChange) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(sym, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    if (showFileExplorer) {
        AlertDialog(
            onDismissRequest = { showFileExplorer = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.file_explorer), style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { showNewFileDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ ${stringResource(R.string.new_file)}", fontSize = 13.sp)
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    items(projectFiles, key = { it.relativePath }) { file ->
                        Card(
                            onClick = {
                                if (!file.isDirectory && file.isTextFile) {
                                    onSelectFile(file.relativePath)
                                    showFileExplorer = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (file.isDirectory) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (file.isDirectory) "📁 " else "📄 ",
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = file.relativePath,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (file.relativePath !in listOf("index.html", "style.css", "script.js")) {
                                    TextButton(onClick = { onDeleteFile(file.relativePath) }) {
                                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFileExplorer = false }) {
                    Text(stringResource(R.string.cancel), fontSize = 14.sp)
                }
            }
        )
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text(stringResource(R.string.new_file), style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = newFileNameInput,
                    onValueChange = { newFileNameInput = it },
                    label = { Text(stringResource(R.string.file_name)) },
                    placeholder = { Text("about.html or custom.css") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            onCreateNewFile(newFileNameInput.trim())
                            newFileNameInput = ""
                            showNewFileDialog = false
                        }
                    },
                    enabled = newFileNameInput.isNotBlank()
                ) {
                    Text(stringResource(R.string.create), fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text(stringResource(R.string.cancel), fontSize = 14.sp)
                }
            }
        )
    }
}