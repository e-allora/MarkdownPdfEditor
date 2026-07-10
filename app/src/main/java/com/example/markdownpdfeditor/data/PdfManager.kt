package com.example.markdownpdfeditor.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class PdfPageInfo(val pageIndex: Int, val bitmap: Bitmap, val width: Int, val height: Int)
    data class PdfDocState(val name: String, val pages: List<PdfPageInfo>, val textSegments: List<String>)

    suspend fun loadPdf(uri: Uri): PdfDocState = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        
        // Get PDF filename
        var filename = "Document.pdf"
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        filename = it.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to default name
        }

        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Cannot open PDF: $uri")

        val pagesList = mutableListOf<PdfPageInfo>()
        val textList = mutableListOf<String>()

        PdfRenderer(parcelFileDescriptor).use { renderer ->
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val width = page.width
                    val height = page.height
                    
                    // Render page to bitmap (resize if too large to prevent OOM)
                    val maxDimension = 1200
                    val scale = if (width > maxDimension || height > maxDimension) {
                        maxDimension.toFloat() / maxOf(width, height)
                    } else {
                        1.0f
                    }
                    val bitmapWidth = (width * scale).toInt()
                    val bitmapHeight = (height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pagesList.add(PdfPageInfo(i, bitmap, width, height))

                    // Text extraction on API 35 (Android 15)+
                    if (Build.VERSION.SDK_INT >= 35) {
                        try {
                            val getTextContentsMethod = page.javaClass.getMethod("getTextContents")
                            val contentsList = getTextContentsMethod.invoke(page) as? List<*>
                            if (contentsList != null && contentsList.isNotEmpty()) {
                                val sb = StringBuilder()
                                for (content in contentsList) {
                                    if (content != null) {
                                        val getTextMethod = content.javaClass.getMethod("getText")
                                        val textVal = getTextMethod.invoke(content) as? String
                                        if (!textVal.isNullOrBlank()) {
                                            sb.append(textVal).append(" ")
                                        }
                                    }
                                }
                                val extracted = sb.toString().trim()
                                if (extracted.isNotEmpty()) {
                                    textList.add(extracted)
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback if API 35 class reflection fails
                        }
                    }
                }
            }
        }
        
        if (textList.isEmpty()) {
            // Text-to-speech information/fallback message for PDF
            textList.add("TTS for PDF is supported on Android 15 (API 35) or higher. For older versions, please use the Markdown Editor for full text-to-speech experience.")
        }

        PdfDocState(filename, pagesList, textList)
    }
}
