package com.example.markdownpdfeditor.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.markdownpdfeditor.ui.EditorViewModel
import com.example.markdownpdfeditor.ui.theme.EditorTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val markdownText by viewModel.markdownText.collectAsState()
    
    // Maintain TextFieldValue locally to keep track of selection/cursor
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(markdownText))
    }
    
    // Sync outer updates (like load file / undo / redo)
    LaunchedEffect(markdownText) {
        if (textFieldValue.text != markdownText) {
            textFieldValue = textFieldValue.copy(text = markdownText)
        }
    }

    // Stats
    val words by viewModel.wordCount.collectAsState()
    val chars by viewModel.charCount.collectAsState()
    val lines by viewModel.lineCount.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Formatting toolbar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatButton(icon = Icons.Default.FormatBold, contentDescription = "Bold") {
                    textFieldValue = applyFormat(textFieldValue, "**", "**")
                    viewModel.updateMarkdown(textFieldValue.text)
                }
                FormatButton(icon = Icons.Default.FormatItalic, contentDescription = "Italic") {
                    textFieldValue = applyFormat(textFieldValue, "*", "*")
                    viewModel.updateMarkdown(textFieldValue.text)
                }
                FormatButton(icon = Icons.Default.Title, contentDescription = "Heading") {
                    textFieldValue = applyFormat(textFieldValue, "\n## ", "")
                    viewModel.updateMarkdown(textFieldValue.text)
                }
                FormatButton(icon = Icons.Default.Code, contentDescription = "Code") {
                    textFieldValue = applyFormat(textFieldValue, "`", "`")
                    viewModel.updateMarkdown(textFieldValue.text)
                }
                FormatButton(icon = Icons.Default.FormatListBulleted, contentDescription = "Bullet List") {
                    textFieldValue = applyFormat(textFieldValue, "\n- ", "")
                    viewModel.updateMarkdown(textFieldValue.text)
                }
                FormatButton(icon = Icons.Default.Link, contentDescription = "Link") {
                    textFieldValue = applyFormat(textFieldValue, "[", "](https://)")
                    viewModel.updateMarkdown(textFieldValue.text)
                }
                FormatButton(icon = Icons.Default.DeleteSweep, contentDescription = "Clear") {
                    textFieldValue = TextFieldValue("")
                    viewModel.updateMarkdown("")
                }
            }
        }

        // Text editor field
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                viewModel.updateMarkdown(it.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
            placeholder = { Text(text = "Start typing markdown here...", style = EditorTextStyle) },
            textStyle = EditorTextStyle,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // Statistics row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Words: $words",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Chars: $chars",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Lines: $lines",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FormatButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Helper to apply selection wrapping formatting
private fun applyFormat(textFieldValue: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val text = textFieldValue.text
    val selection = textFieldValue.selection
    val start = selection.start
    val end = selection.end
    
    val selectedText = text.substring(start, end)
    val newText = text.replaceRange(start, end, "$prefix$selectedText$suffix")
    val newSelectionStart = start + prefix.length
    val newSelectionEnd = newSelectionStart + selectedText.length
    
    return TextFieldValue(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd)
    )
}
