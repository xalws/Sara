package com.example.sara

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import java.util.Locale

class AccessibilityEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val handler = Handler(Looper.getMainLooper())

    private var audioTrack: AudioTrack? = null
    private val sampleRate = 44100

    private val minFreq = 220.0  // Tono grave para valores bajos
    private val maxFreq = 880.0  // Tono agudo para valores altos

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "MX")
        }
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Tono basado en la coordenada Y (-10 a +10)
    fun playToneForY(yVal: Double) {
        playToneNormalized(yVal, -10.0, 10.0, 50)
    }

    // Tono para un valor dentro del rango [min, max] (sirve para cualquier función)
    fun playToneNormalized(value: Double, min: Double, max: Double, durationMs: Int = 50) {
        val t = if (max > min) ((value - min) / (max - min)).coerceIn(0.0, 1.0) else 0.5
        generateTone(minFreq + t * (maxFreq - minFreq), durationMs)
    }

    // Reproduce la gráfica como una secuencia de tonos (izquierda a derecha).
    // Los valores null (no definidos) se reproducen como silencio.
    fun playSweep(values: List<Double?>, stepMs: Long = 45L, startDelayMs: Long = 0L) {
        stopSweep()

        val finite = values.filterNotNull()
        if (finite.isEmpty()) {
            speak("No hay valores para reproducir")
            return
        }
        val min = finite.min()
        val max = finite.max()

        values.forEachIndexed { i, v ->
            if (v != null) {
                handler.postDelayed({
                    playToneNormalized(v, min, max, stepMs.toInt())
                }, startDelayMs + i * stepMs)
            }
        }
    }

    fun stopSweep() {
        handler.removeCallbacksAndMessages(null)
        audioTrack?.stop()
    }

    private fun generateTone(freqHz: Double, durationMs: Int) {
        val numSamples = (durationMs * sampleRate) / 1000
        val generatedSnd = ByteArray(2 * numSamples)

        var idx = 0
        for (i in 0 until numSamples) {
            val dVal = Math.sin(2.0 * Math.PI * i.toDouble() / (sampleRate / freqHz))
            val valShort = (dVal * 32767).toInt().toShort()
            generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
            generatedSnd[idx++] = (valShort.toInt() and 0xff00).shr(8).toByte()
        }

        audioTrack?.stop()
        audioTrack?.release()

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(generatedSnd.size, minBuffer))
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
        handler.removeCallbacksAndMessages(null)
        tts.stop()
        tts.shutdown()
        audioTrack?.release()
    }
}
