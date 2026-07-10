package com.example.markdownpdfeditor.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class PlaybackState { IDLE, INITIALIZING, READY, SPEAKING, PAUSED }

    private var tts: TextToSpeech? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _currentSegmentIndex = MutableStateFlow(-1)
    val currentSegmentIndex: StateFlow<Int> = _currentSegmentIndex.asStateFlow()

    private var segments: List<String> = emptyList()
    private var speedRate = 1.0f
    private var pitchRate = 1.0f

    fun init(onInitResult: (Boolean) -> Unit = {}) {
        if (tts != null) {
            onInitResult(true)
            return
        }
        _playbackState.value = PlaybackState.INITIALIZING
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(speedRate)
                tts?.setPitch(pitchRate)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val index = utteranceId?.toIntOrNull() ?: -1
                        _currentSegmentIndex.value = index
                        _playbackState.value = PlaybackState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        val index = utteranceId?.toIntOrNull() ?: -1
                        if (index >= 0 && index < segments.size - 1) {
                            speakSegment(index + 1)
                        } else {
                            _playbackState.value = PlaybackState.READY
                            _currentSegmentIndex.value = -1
                            _currentText.value = ""
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _playbackState.value = PlaybackState.READY
                        _currentSegmentIndex.value = -1
                        _currentText.value = ""
                    }
                })
                _playbackState.value = PlaybackState.READY
                onInitResult(true)
            } else {
                _playbackState.value = PlaybackState.IDLE
                onInitResult(false)
            }
        }
    }

    fun speak(textList: List<String>, startFromIndex: Int = 0) {
        if (tts == null) {
            init { ok ->
                if (ok) {
                    startSpeaking(textList, startFromIndex)
                }
            }
        } else {
            startSpeaking(textList, startFromIndex)
        }
    }

    private fun startSpeaking(textList: List<String>, startFromIndex: Int) {
        this.segments = textList
        if (textList.isEmpty()) return
        speakSegment(startFromIndex)
    }

    private fun speakSegment(index: Int) {
        if (index !in segments.indices) return
        val text = segments[index]
        _currentText.value = text
        _currentSegmentIndex.value = index
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun pause() {
        if (_playbackState.value == PlaybackState.SPEAKING) {
            tts?.stop()
            _playbackState.value = PlaybackState.PAUSED
        }
    }

    fun resume() {
        if (_playbackState.value == PlaybackState.PAUSED) {
            speakSegment(_currentSegmentIndex.value)
        }
    }

    fun stop() {
        tts?.stop()
        segments = emptyList()
        _currentSegmentIndex.value = -1
        _currentText.value = ""
        _playbackState.value = PlaybackState.READY
    }

    fun setSpeed(rate: Float) {
        speedRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speedRate)
    }

    fun setPitch(pitch: Float) {
        pitchRate = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(pitchRate)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _playbackState.value = PlaybackState.IDLE
    }
}
