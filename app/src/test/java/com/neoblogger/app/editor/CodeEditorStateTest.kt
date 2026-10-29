package com.neoblogger.app.editor

import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeEditorStateTest {

    @Test
    fun updateTextFieldValue_preservesTextAndSupportsUndo() {
        val state = CodeEditorState("hello")
        assertEquals("hello", state.currentText)

        state.updateTextFieldValue(TextFieldValue("hello world"))
        assertEquals("hello world", state.currentText)
        assertTrue(state.canUndo)

        state.undo()
        assertEquals("hello", state.currentText)
    }

    @Test
    fun moveCursorToLine_calculatesCorrectSelectionOffset() {
        val state = CodeEditorState("line 1\nline 2\nline 3")
        state.moveCursorToLine(2)

        val sel = state.textFieldValue.selection
        assertEquals(7, sel.start) // start of "line 2"
        assertEquals(13, sel.end)  // end of "line 2"
    }
}