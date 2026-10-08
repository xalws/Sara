package com.example.sara

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import java.util.Locale

class AccessibilityEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var audioTrack: AudioTrack? = null
    private val sampleRate = 44100
    private var isPlaying = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "MX")
        }
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Método para reproducir tono basado en la coordenada Y (-10 a +10)
    fun playToneForY(yVal: Double) {
        val minFreq = 220.0  // Tono grave para Y negativo
        val maxFreq = 880.0  // Tono agudo para Y positivo

        // Mapeo lineal simple de Y (-10 a 10) a frecuencias audibles
        val clampedY = yVal.coerceIn(-10.0, 10.0)
        val frequency = minFreq + ((clampedY + 10) / 20.0) * (maxFreq - minFreq)

        generateTone(frequency)
    }

    private fun generateTone(freqHz: Double) {
        val durationMs = 50
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = DoubleArray(numSamples)
        val generatedSnd = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            sample[i] = Math.sin(2.0 * Math.PI * i.toDouble() / (sampleRate / freqHz))
        }

        var idx = 0
        for (dVal in sample) {
            val valShort = (dVal * 32767).toInt().toShort()
            generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
            generatedSnd[idx++] = (valShort.toInt() and 0xff00).shr(8).toByte()
        }

        audioTrack?.stop()
        audioTrack?.release()

        audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(generatedSnd.size)
            .build()

        audioTrack?.write(generatedSnd, 0, generatedSnd.size)
        audioTrack?.play()
    }

    // Vibración al pasar sobre la curva o puntos clave
    fun triggerHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        audioTrack?.release()
    }
}