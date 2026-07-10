package com.example.markdownpdfeditor.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.markdownpdfeditor.data.MarkdownParser
import com.example.markdownpdfeditor.data.TtsManager
import com.example.markdownpdfeditor.ui.EditorViewModel
import com.example.markdownpdfeditor.ui.theme.AccentTeal
import com.example.markdownpdfeditor.ui.screens.EditorScreen
import com.example.markdownpdfeditor.ui.screens.PdfViewerScreen
import com.example.markdownpdfeditor.data.MarkdownRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Editor", "Preview", "PDF Viewer")

    val markdownText by viewModel.markdownText.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val pdfName by viewModel.pdfName.collectAsState()
    
    // File Picker Launchers
    val openMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadMarkdownFromFile(it, context) }
    }

    val saveMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let { viewModel.saveMarkdownToFile(it, context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedTab == 2 && pdfName.isNotEmpty()) "PDF Viewer" else "Markdown Editor",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (selectedTab == 2 && pdfName.isNotEmpty()) pdfName else fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (selectedTab != 2) {
                        // Undo & Redo for Markdown Editor
                        IconButton(onClick = { viewModel.undo() }) {
                            Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = { viewModel.redo() }) {
                            Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo")
                        }
                        // Open File
                        IconButton(onClick = { openMarkdownLauncher.launch("text/*") }) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Open File")
                        }
                        // Save File
                        IconButton(onClick = { saveMarkdownLauncher.launch(fileName) }) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save File")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            )
        },
        bottomBar = {
            Column {
                // Text-to-Speech Control Panel
                TtsControlPanel(viewModel = viewModel, isPdfTab = selectedTab == 2)
                
                // Navigation tabs
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.EditNote
                                        1 -> Icons.Default.Preview
                                        else -> Icons.Default.PictureAsPdf
                                    },
                                    contentDescription = title
                                )
                            },
                            label = { Text(text = title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> EditorScreen(viewModel = viewModel)
                1 -> {
                    val elements = remember(markdownText) { MarkdownParser.parse(markdownText) }
                    val highlightIndex by viewModel.ttsCurrentSegmentIndex.collectAsState()
                    val scrollState = rememberScrollState()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        MarkdownRenderer(
                            elements = elements,
                            highlightIndex = if (viewModel.ttsPlaybackState.collectAsState().value == TtsManager.PlaybackState.SPEAKING) highlightIndex else -1
                        )
                    }
                }
                2 -> PdfViewerScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TtsControlPanel(
    viewModel: EditorViewModel,
    isPdfTab: Boolean
) {
    val ttsPlaybackState by viewModel.ttsPlaybackState.collectAsState()
    val speed by viewModel.ttsSpeed.collectAsState()
    val pitch by viewModel.ttsPitch.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    // TTS Panel is shown only when in a ready, speaking, or paused state
    if (ttsPlaybackState != TtsManager.PlaybackState.IDLE) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Play / Pause Button
                        IconButton(
                            onClick = { viewModel.startTts(isPdfTab) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = if (ttsPlaybackState == TtsManager.PlaybackState.SPEAKING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (ttsPlaybackState == TtsManager.PlaybackState.SPEAKING) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Stop Button
                        if (ttsPlaybackState == TtsManager.PlaybackState.SPEAKING || ttsPlaybackState == TtsManager.PlaybackState.PAUSED) {
                            IconButton(
                                onClick = { viewModel.stopTts() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Status Info
                        Column {
                            val statusText = when (ttsPlaybackState) {
                                TtsManager.PlaybackState.SPEAKING -> "Speaking..."
                                TtsManager.PlaybackState.PAUSED -> "Playback Paused"
                                else -> "Text-to-Speech Ready"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (ttsPlaybackState == TtsManager.PlaybackState.SPEAKING) {
                                val currentWordHighlight by viewModel.ttsCurrentSegmentIndex.collectAsState()
                                Text(
                                    text = if (isPdfTab) "Reading PDF page ${currentWordHighlight + 1}" else "Reading paragraph ${currentWordHighlight + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Settings Toggle Button
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = "TTS settings",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Expandable Speach Controls
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        // Speed Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Speech Speed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.2fx", speed),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = speed,
                                onValueChange = { viewModel.setTtsSpeed(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Pitch Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Voice Pitch",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.2fx", pitch),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = pitch,
                                onValueChange = { viewModel.setTtsPitch(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
