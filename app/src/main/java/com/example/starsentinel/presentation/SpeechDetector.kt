package com.example.starsentinel.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.abs

// A class to detect speech activity using audio input and extract audio features

class SpeechDetector(private val context: Context) {
    private val tag = "SpeechDetector"
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val _isSpeechDetected = MutableStateFlow(false)
    val isSpeechDetected: StateFlow<Boolean> = _isSpeechDetected.asStateFlow()

    // Create feature extractor
    private val audioFeatureExtractor = AudioFeatureExtractor()
    val mfccValues = audioFeatureExtractor.mfccValues
    val pitchMean = audioFeatureExtractor.pitchMean
    val intensityVar = audioFeatureExtractor.intensityVar

    private val amplitudeThreshold = 1500 // Adjust this threshold based on testing
    private val speechTimeoutMS = 1000 // Time without speech before considering it ended

    fun startListening(): Boolean {
        if (isRecording) return true

        // Explicitly check for permission before accessing the microphone
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {
            Log.e(tag, "Record audio permission not granted")
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord not initialized")
                return false
            }

            isRecording = true
            audioRecord?.startRecording()

            CoroutineScope(Dispatchers.IO).launch {
                processAudio()
            }
            return true
        } catch (securityException: SecurityException) {
            Log.e(tag, "Security exception when starting microphone: ${securityException.message}")
        } catch (ioException: IOException) {
            Log.e(tag, "IO exception when starting microphone: ${ioException.message}")
        } catch (exception: Exception) {
            Log.e(tag, "Error starting speech detection: ${exception.message}")
        }

        return false
    }

    fun stopListening() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            audioFeatureExtractor.reset()
        } catch (exception: Exception) {
            Log.e(tag, "Error stopping speech detection: ${exception.message}")
        }
    }

    private fun processAudio() {
        val buffer = ShortArray(bufferSize)
        var lastSpeechTime = System.currentTimeMillis()

        while (isRecording) {
            try {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    // Calculate average amplitude
                    var sum = 0.0
                    for (i in 0 until readSize) {
                        sum += abs(buffer[i].toDouble())
                    }
                    val average = sum / readSize

                    val currentTime = System.currentTimeMillis()
                    val speechDetected = average > amplitudeThreshold

                    if (speechDetected) {
                        lastSpeechTime = currentTime
                        if (!_isSpeechDetected.value) {
                            _isSpeechDetected.value = true
                        }

                        // Process audio features when speech is detected
                        audioFeatureExtractor.processAudioBuffer(buffer, sampleRate)
                    } else if (_isSpeechDetected.value && (currentTime - lastSpeechTime > speechTimeoutMS)) {
                        _isSpeechDetected.value = false
                    }
                }
            } catch (securityException: SecurityException) {
                Log.e(tag, "Security exception while processing audio: ${securityException.message}")
                break
            } catch (exception: Exception) {
                Log.e(tag, "Error processing audio: ${exception.message}")
                break
            }

            try {
                // Small delay to prevent CPU overuse
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}