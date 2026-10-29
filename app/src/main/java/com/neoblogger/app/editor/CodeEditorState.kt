package com.neoblogger.app.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class CodeEditorState(initialText: String = "") {

    var textFieldValue by mutableStateOf(TextFieldValue(text = initialText))
        private set

    val currentText: String
        get() = textFieldValue.text

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    fun updateTextFieldValue(newValue: TextFieldValue) {
        val oldText = textFieldValue.text
        if (newValue.text != oldText) {
            undoStack.addLast(oldText)
            if (undoStack.size > MAX_STACK_SIZE) undoStack.removeFirst()
            redoStack.clear()
        }
        textFieldValue = newValue
    }

    fun updateText(newText: String) {
        val oldText = textFieldValue.text
        if (newText != oldText) {
            undoStack.addLast(oldText)
            if (undoStack.size > MAX_STACK_SIZE) undoStack.removeFirst()
            redoStack.clear()
            textFieldValue = TextFieldValue(
                text = newText,
                selection = TextRange(newText.length)
            )
        }
    }

    fun moveCursorToLine(targetLine: Int) {
        val lines = currentText.lines()
        if (lines.isEmpty()) return

        val clampedLine = targetLine.coerceIn(1, lines.size)
        var charOffset = 0
        for (i in 0 until (clampedLine - 1)) {
            charOffset += lines[i].length + 1 // +1 for newline
        }

        val lineLength = lines[clampedLine - 1].length
        val endOffset = charOffset + lineLength

        textFieldValue = TextFieldValue(
            text = currentText,
            selection = TextRange(charOffset, endOffset)
        )
    }

    fun undo(): Boolean {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(currentText)
            val restored = undoStack.removeLast()
            textFieldValue = TextFieldValue(
                text = restored,
                selection = TextRange(restored.length)
            )
            return true
        }
        return false
    }

    fun redo(): Boolean {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(currentText)
            val restored = redoStack.removeLast()
            textFieldValue = TextFieldValue(
                text = restored,
                selection = TextRange(restored.length)
            )
            return true
        }
        return false
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun reset(text: String) {
        textFieldValue = TextFieldValue(
            text = text,
            selection = TextRange(text.length)
        )
        undoStack.clear()
        redoStack.clear()
    }

    fun findAndReplace(findTerm: String, replaceTerm: String): Int {
        if (findTerm.isEmpty()) return 0
        val count = currentText.split(findTerm).size - 1
        if (count > 0) {
            updateText(currentText.replace(findTerm, replaceTerm))
        }
        return count
    }

    companion object {
        private const val MAX_STACK_SIZE = 50
    }
}