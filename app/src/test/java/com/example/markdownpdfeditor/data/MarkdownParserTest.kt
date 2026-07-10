package com.example.markdownpdfeditor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testParseHeaders() {
        val markdown = "# Heading 1\n## Heading 2\n### Heading 3"
        val elements = MarkdownParser.parse(markdown)

        assertEquals(3, elements.size)
        assertTrue(elements[0] is MarkdownElement.Heading)
        assertTrue(elements[1] is MarkdownElement.Heading)
        assertTrue(elements[2] is MarkdownElement.Heading)

        val h1 = elements[0] as MarkdownElement.Heading
        assertEquals(1, h1.level)
        assertEquals("Heading 1", h1.text)

        val h2 = elements[1] as MarkdownElement.Heading
        assertEquals(2, h2.level)
        assertEquals("Heading 2", h2.text)
    }

    @Test
    fun testParseBulletItems() {
        val markdown = "- Item 1\n* Item 2\n+ Item 3"
        val elements = MarkdownParser.parse(markdown)

        assertEquals(3, elements.size)
        assertTrue(elements[0] is MarkdownElement.BulletItem)
        assertTrue(elements[1] is MarkdownElement.BulletItem)
        assertTrue(elements[2] is MarkdownElement.BulletItem)

        assertEquals("Item 1", (elements[0] as MarkdownElement.BulletItem).text)
        assertEquals("Item 2", (elements[1] as MarkdownElement.BulletItem).text)
        assertEquals("Item 3", (elements[2] as MarkdownElement.BulletItem).text)
    }

    @Test
    fun testParseCodeBlocks() {
        val markdown = "```kotlin\nfun main() {\n    println(\"Hello\")\n}\n```"
        val elements = MarkdownParser.parse(markdown)

        assertEquals(1, elements.size)
        assertTrue(elements[0] is MarkdownElement.CodeBlock)

        val codeBlock = elements[0] as MarkdownElement.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertEquals("fun main() {\n    println(\"Hello\")\n}", codeBlock.code)
    }

    @Test
    fun testParseBlockquote() {
        val markdown = "> This is a quote"
        val elements = MarkdownParser.parse(markdown)

        assertEquals(1, elements.size)
        assertTrue(elements[0] is MarkdownElement.Blockquote)
        assertEquals("This is a quote", (elements[0] as MarkdownElement.Blockquote).text)
    }

    @Test
    fun testParseHorizontalRules() {
        val markdown = "---\n***"
        val elements = MarkdownParser.parse(markdown)

        assertEquals(2, elements.size)
        assertTrue(elements[0] is MarkdownElement.HorizontalRule)
        assertTrue(elements[1] is MarkdownElement.HorizontalRule)
    }

    @Test
    fun testParseParagraphs() {
        val markdown = "This is paragraph 1.\n\nThis is paragraph 2."
        val elements = MarkdownParser.parse(markdown)

        assertEquals(2, elements.size)
        assertTrue(elements[0] is MarkdownElement.Paragraph)
        assertTrue(elements[1] is MarkdownElement.Paragraph)
        assertEquals("This is paragraph 1.", (elements[0] as MarkdownElement.Paragraph).text)
        assertEquals("This is paragraph 2.", (elements[1] as MarkdownElement.Paragraph).text)
    }
}
