package com.example.markdownpdfeditor.data

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownElement {
    data class Heading(val level: Int, val text: String) : MarkdownElement()
    data class Paragraph(val text: String) : MarkdownElement()
    data class BulletItem(val text: String) : MarkdownElement()
    data class Blockquote(val text: String) : MarkdownElement()
    data class CodeBlock(val language: String, val code: String) : MarkdownElement()
    object HorizontalRule : MarkdownElement()
}

object MarkdownParser {

    fun parse(text: String): List<MarkdownElement> {
        val lines = text.lines()
        val elements = mutableListOf<MarkdownElement>()
        var currentCodeBlock: StringBuilder? = null
        var currentCodeLang = ""
        var currentParagraph: StringBuilder? = null

        fun commitParagraph() {
            currentParagraph?.let {
                val pText = it.toString().trim()
                if (pText.isNotEmpty()) {
                    elements.add(MarkdownElement.Paragraph(pText))
                }
                currentParagraph = null
            }
        }

        for (line in lines) {
            val trimmedLine = line.trim()
            
            // Check if we are inside a code block
            if (currentCodeBlock != null) {
                if (trimmedLine.startsWith("```")) {
                    elements.add(MarkdownElement.CodeBlock(currentCodeLang, currentCodeBlock.toString().trimEnd()))
                    currentCodeBlock = null
                    currentCodeLang = ""
                } else {
                    currentCodeBlock.append(line).append("\n")
                }
                continue
            }

            // Start of a code block
            if (trimmedLine.startsWith("```")) {
                commitParagraph()
                currentCodeBlock = StringBuilder()
                currentCodeLang = trimmedLine.substring(3).trim()
                continue
            }

            // Headings (# Header)
            if (trimmedLine.startsWith("#")) {
                commitParagraph()
                val hashCount = trimmedLine.takeWhile { it == '#' }.length
                if (hashCount in 1..6 && trimmedLine.length > hashCount && trimmedLine[hashCount] == ' ') {
                    elements.add(MarkdownElement.Heading(hashCount, trimmedLine.substring(hashCount + 1).trim()))
                    continue
                }
            }

            // Blockquotes (> Blockquote)
            if (trimmedLine.startsWith(">")) {
                commitParagraph()
                elements.add(MarkdownElement.Blockquote(trimmedLine.substring(1).trim()))
                continue
            }

            // Bullet list items (- list)
            if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("+ ")) {
                commitParagraph()
                elements.add(MarkdownElement.BulletItem(trimmedLine.substring(2).trim()))
                continue
            }

            // Horizontal Rules (---)
            if (trimmedLine == "---" || trimmedLine == "***" || trimmedLine == "___") {
                commitParagraph()
                elements.add(MarkdownElement.HorizontalRule)
                continue
            }

            // Empty lines split paragraphs
            if (trimmedLine.isEmpty()) {
                commitParagraph()
            } else {
                if (currentParagraph == null) {
                    currentParagraph = StringBuilder()
                } else {
                    currentParagraph!!.append(" ")
                }
                currentParagraph!!.append(trimmedLine)
            }
        }
        commitParagraph()
        return elements
    }

    fun parseInlineStyles(text: String, primaryColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                // Bold: **text**
                if (text.startsWith("**", i)) {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                        continue
                    }
                }
                // Italic: *text*
                if (text.startsWith("*", i)) {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                        continue
                    }
                }
                // Inline Code: `code`
                if (text.startsWith("`", i)) {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = primaryColor.copy(alpha = 0.12f),
                            color = primaryColor,
                            fontSize = 13.sp
                        ))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                        continue
                    }
                }
                // Links: [label](url)
                if (text.startsWith("[", i)) {
                    val labelEnd = text.indexOf("]", i + 1)
                    if (labelEnd != -1 && text.startsWith("(", labelEnd + 1)) {
                        val urlEnd = text.indexOf(")", labelEnd + 2)
                        if (urlEnd != -1) {
                            val label = text.substring(i + 1, labelEnd)
                            pushStyle(SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            ))
                            append(label)
                            pop()
                            i = urlEnd + 1
                            continue
                        }
                    }
                }
                append(text[i])
                i++
            }
        }
    }
}

@Composable
fun MarkdownRenderer(
    elements: List<MarkdownElement>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1 // For TTS highlighted paragraph/block
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        elements.forEachIndexed { index, element ->
            val isHighlighted = index == highlightIndex
            val itemModifier = if (isHighlighted) {
                Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            } else {
                Modifier
            }

            Box(modifier = itemModifier.fillMaxWidth()) {
                when (element) {
                    is MarkdownElement.Heading -> {
                        val style = when (element.level) {
                            1 -> MaterialTheme.typography.displayMedium
                            2 -> MaterialTheme.typography.headlineLarge
                            3 -> MaterialTheme.typography.headlineMedium
                            else -> MaterialTheme.typography.titleLarge
                        }
                        Text(
                            text = element.text,
                            style = style,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    is MarkdownElement.Paragraph -> {
                        Text(
                            text = MarkdownParser.parseInlineStyles(element.text, MaterialTheme.colorScheme.primary),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    is MarkdownElement.BulletItem -> {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "•  ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = MarkdownParser.parseInlineStyles(element.text, MaterialTheme.colorScheme.primary),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    is MarkdownElement.Blockquote -> {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = 4.dp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = element.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    is MarkdownElement.CodeBlock -> {
                        val clipboardManager = LocalClipboardManager.current
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (element.language.isNotEmpty()) {
                                    Text(
                                        text = element.language.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "CODE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Button(
                                    onClick = { clipboardManager.setText(AnnotatedString(element.code)) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(text = "Copy", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = element.code,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                    is MarkdownElement.HorizontalRule -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}
