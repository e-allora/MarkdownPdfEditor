package com.example.markdownpdfeditor.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdownpdfeditor.data.MarkdownElement
import com.example.markdownpdfeditor.data.MarkdownParser
import com.example.markdownpdfeditor.data.PdfManager
import com.example.markdownpdfeditor.data.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Stack
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsManager: TtsManager,
    private val pdfManager: PdfManager
) : ViewModel() {

    // --- MARKDOWN STATE ---
    private val _markdownText = MutableStateFlow(
        """# Markdown & PDF Reader with TTS

This is a premium Markdown editor built in **Android Jetpack Compose**. It features:
- Live Markdown Preview rendering
- Dynamic Material 3 styling
- Native PDF viewing (via Android's `PdfRenderer`)
- Paragraph-by-paragraph **Text-to-Speech (TTS)** with visual word highlights!

## Text-to-Speech Controls
Press the Play button at the bottom of the screen to read this text aloud. 
You can adjust the speed and pitch in real-time.

> "Clean architecture is not a rule, it's a way of writing software that respects changes."

Try adding lists, code blocks, or loading a PDF file!
"""
    )
    val markdownText: StateFlow<String> = _markdownText.asStateFlow()

    private val _fileName = MutableStateFlow("Untitled.md")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    // Undo/Redo Stacks
    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var isUpdatingUndo = false

    // Stats
    val wordCount: StateFlow<Int> = MutableStateFlow(0)
    val charCount: StateFlow<Int> = MutableStateFlow(0)
    val lineCount: StateFlow<Int> = MutableStateFlow(0)

    // --- PDF STATE ---
    private val _pdfPages = MutableStateFlow<List<PdfManager.PdfPageInfo>>(emptyList())
    val pdfPages: StateFlow<List<PdfManager.PdfPageInfo>> = _pdfPages.asStateFlow()

    private val _pdfName = MutableStateFlow("")
    val pdfName: StateFlow<String> = _pdfName.asStateFlow()

    private val _pdfLoading = MutableStateFlow(false)
    val pdfLoading: StateFlow<Boolean> = _pdfLoading.asStateFlow()

    private val _pdfError = MutableStateFlow<String?>(null)
    val pdfError: StateFlow<String?> = _pdfError.asStateFlow()

    private var pdfTextSegments = emptyList<String>()

    // --- TTS STATE ---
    val ttsPlaybackState = ttsManager.playbackState
    val ttsCurrentSegmentIndex = ttsManager.currentSegmentIndex
    
    private val _ttsSpeed = MutableStateFlow(1.0f)
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _ttsPitch = MutableStateFlow(1.0f)
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    init {
        updateStats(_markdownText.value)
        ttsManager.init()
    }

    // Update Markdown text and handle Undo/Redo history
    fun updateMarkdown(newText: String) {
        if (!isUpdatingUndo) {
            val currentVal = _markdownText.value
            if (undoStack.isEmpty() || undoStack.peek() != currentVal) {
                undoStack.push(currentVal)
                redoStack.clear()
            }
        }
        _markdownText.value = newText
        updateStats(newText)
    }

    private fun updateStats(text: String) {
        viewModelScope.launch {
            val chars = text.length
            val lines = if (text.isEmpty()) 0 else text.lines().size
            val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            
            (wordCount as MutableStateFlow).value = words
            (charCount as MutableStateFlow).value = chars
            (lineCount as MutableStateFlow).value = lines
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(_markdownText.value)
            isUpdatingUndo = true
            _markdownText.value = undoStack.pop()
            isUpdatingUndo = false
            updateStats(_markdownText.value)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_markdownText.value)
            isUpdatingUndo = true
            _markdownText.value = redoStack.pop()
            isUpdatingUndo = false
            updateStats(_markdownText.value)
        }
    }

    // Save Markdown to File
    fun saveMarkdownToFile(uri: Uri, context: Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(_markdownText.value)
                    }
                }
                // Extract filename
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            _fileName.value = it.getString(index)
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                // Handle file write errors
            }
        }
    }

    // Load Markdown from File
    fun loadMarkdownFromFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val content = reader.readText()
                        undoStack.clear()
                        redoStack.clear()
                        _markdownText.value = content
                        updateStats(content)
                    }
                }
                // Extract filename
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            _fileName.value = it.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle file read errors
            }
        }
    }

    // Load PDF Document
    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _pdfLoading.value = true
            _pdfError.value = null
            try {
                val state = pdfManager.loadPdf(uri)
                _pdfPages.value = state.pages
                _pdfName.value = state.name
                pdfTextSegments = state.textSegments
            } catch (e: Exception) {
                _pdfError.value = "Failed to load PDF: ${e.localizedMessage}"
            } finally {
                _pdfLoading.value = false
            }
        }
    }

    // Close PDF Document
    fun closePdf() {
        _pdfPages.value = emptyList()
        _pdfName.value = ""
        _pdfError.value = null
        pdfTextSegments = emptyList()
        if (ttsPlaybackState.value == TtsManager.PlaybackState.SPEAKING) {
            stopTts()
        }
    }

    // --- TTS CONTROLS ---
    fun startTts(isPdfTab: Boolean) {
        if (ttsPlaybackState.value == TtsManager.PlaybackState.SPEAKING) {
            ttsManager.pause()
            return
        }
        if (ttsPlaybackState.value == TtsManager.PlaybackState.PAUSED) {
            ttsManager.resume()
            return
        }

        val textToSpeak = if (isPdfTab) {
            pdfTextSegments
        } else {
            // Parse markdown and speak paragraph elements
            val elements = MarkdownParser.parse(_markdownText.value)
            elements.mapNotNull { element ->
                when (element) {
                    is MarkdownElement.Paragraph -> element.text
                    is MarkdownElement.Heading -> element.text
                    is MarkdownElement.BulletItem -> element.text
                    is MarkdownElement.Blockquote -> element.text
                    is MarkdownElement.CodeBlock -> "Code block: " + element.language
                    else -> null
                }
            }.filter { it.isNotBlank() }
        }

        if (textToSpeak.isNotEmpty()) {
            ttsManager.speak(textToSpeak)
        }
    }

    fun pauseTts() {
        ttsManager.pause()
    }

    fun stopTts() {
        ttsManager.stop()
    }

    fun setTtsSpeed(speed: Float) {
        _ttsSpeed.value = speed
        ttsManager.setSpeed(speed)
    }

    fun setTtsPitch(pitch: Float) {
        _ttsPitch.value = pitch
        ttsManager.setPitch(pitch)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
