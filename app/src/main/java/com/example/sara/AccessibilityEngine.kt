package com.example.sara

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Voz (TTS), sonificación de gráficas y vibración.
 * Sonificación: eje X = tiempo (izquierda -> derecha), altura de la curva = tono (grave -> agudo).
 * Si la curva tiene varias ramas en un mismo x (p. ej. un círculo), suenan varias voces a la vez.
 */
class AccessibilityEngine(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val lock = Any()
    private val hilo = Executors.newSingleThreadExecutor()
    private var version = 0

    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ttsListo = false
    private var textoPendiente: String? = null

    private val vibrator: Vibrator? = obtenerVibrator()
    private var audioTrack: AudioTrack? = null

    // ---------- Voz ----------

    override fun onInit(status: Int) {
        val motor = tts ?: return
        if (status != TextToSpeech.SUCCESS) return
        val r = motor.setLanguage(Locale.forLanguageTag("es-MX"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            motor.setLanguage(Locale.forLanguageTag("es"))
        }
        ttsListo = true
        val pendiente = textoPendiente
        if (pendiente != null) {
            motor.speak(pendiente, TextToSpeech.QUEUE_FLUSH, null, ID_TTS)
        }
        textoPendiente = null
    }

    fun speak(text: String) {
        val motor = tts ?: return
        if (ttsListo) {
            motor.speak(text, TextToSpeech.QUEUE_FLUSH, null, ID_TTS)
        } else {
            textoPendiente = text // se dirá cuando el TTS termine de iniciar
        }
    }

    // ---------- Sonificación ----------

    /**
     * series[i] = valores de y (0 a 4 ramas) en el i-ésimo paso de x.
     * Se normaliza al rango real de la curva dentro de la ventana [yMin, yMax].
     */
    fun reproducirSerie(
        series: List<List<Double>>,
        yMin: Double,
        yMax: Double,
        duracionTotalMs: Int = 5000
    ) {
        val pasos = normalizar(series, yMin, yMax)
        if (pasos.isEmpty()) return
        val msPorPaso = (duracionTotalMs / pasos.size).coerceIn(30, 150)
        reproducirNormalizado(pasos, msPorPaso)
    }

    /** Tono corto para un valor de y (útil para exploración táctil). */
    fun playToneForY(yVal: Double, yMin: Double = -10.0, yMax: Double = 10.0) {
        val t = ((yVal - yMin) / (yMax - yMin)).coerceIn(0.0, 1.0)
        reproducirNormalizado(List(3) { listOf(t) }, 60)
    }

    fun detenerAudio() {
        synchronized(lock) {
            version++
            liberarPista()
        }
    }

    private fun normalizar(
        series: List<List<Double>>,
        yMin: Double,
        yMax: Double
    ): List<List<Double>> {
        val dentro = series.flatten().filter { it.isFinite() && it >= yMin && it <= yMax }
        var lo = dentro.minOrNull() ?: yMin
        var hi = dentro.maxOrNull() ?: yMax
        if (hi - lo < 1e-6 * (yMax - yMin)) { // función constante -> tono medio
            lo -= 1.0
            hi += 1.0
        }
        return series.map { paso ->
            paso.filter { it.isFinite() }.map { (it.coerceIn(lo, hi) - lo) / (hi - lo) }
        }
    }

    private fun frecuencia(t: Double): Double = F_MIN * 2.0.pow(t.coerceIn(0.0, 1.0) * OCTAVAS)

    private fun reproducirNormalizado(pasos: List<List<Double>>, msPorPaso: Int) {
        val mi: Int
        synchronized(lock) {
            version++
            mi = version
            liberarPista()
        }
        hilo.execute {
            val pcm = sintetizar(pasos, msPorPaso)
            if (pcm != null) {
                val pista = try {
                    crearPista(pcm)
                } catch (e: Exception) {
                    null
                }
                if (pista != null) {
                    synchronized(lock) {
                        if (mi != version) {
                            pista.release()
                        } else {
                            audioTrack = pista
                            pista.play()
                        }
                    }
                }
            }
        }
    }

    private fun sintetizar(pasos: List<List<Double>>, msPorPaso: Int): ShortArray? {
        val voces = (pasos.maxOfOrNull { it.size } ?: 0).coerceAtMost(MAX_VOCES)
        if (voces == 0) return null

        val muestrasPorPaso = SAMPLE_RATE * msPorPaso / 1000
        val pcm = ShortArray(muestrasPorPaso * pasos.size)
        val freq = DoubleArray(voces) { F_MIN }
        val amp = DoubleArray(voces)
        val fase = DoubleArray(voces)
        val kFreq = 1.0 - exp(-1.0 / (0.012 * SAMPLE_RATE)) // glissando suave entre pasos
        val kAmp = 1.0 - exp(-1.0 / (0.003 * SAMPLE_RATE))  // evita clics al entrar/salir una voz

        var pos = 0
        for (paso in pasos) {
            val presentes = minOf(paso.size, voces)
            val freqObj = DoubleArray(voces) { v -> if (v < presentes) frecuencia(paso[v]) else freq[v] }
            val ampObj = DoubleArray(voces) { v -> if (v < presentes) AMPLITUD / presentes else 0.0 }
            for (v in 0 until presentes) {
                if (amp[v] < 0.001) freq[v] = freqObj[v]
            }

            for (n in 0 until muestrasPorPaso) {
                var suma = 0.0
                for (v in 0 until voces) {
                    freq[v] += (freqObj[v] - freq[v]) * kFreq
                    amp[v] += (ampObj[v] - amp[v]) * kAmp
                    fase[v] += 2.0 * PI * freq[v] / SAMPLE_RATE
                    if (fase[v] > 2.0 * PI) fase[v] -= 2.0 * PI
                    suma += amp[v] * sin(fase[v])
                }
                pcm[pos] = (suma.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                pos++
            }
        }

        // Fundido final de 20 ms
        val fade = minOf(pcm.size, SAMPLE_RATE / 50)
        for (i in 0 until fade) {
            val idx = pcm.size - 1 - i
            pcm[idx] = (pcm[idx] * (i.toDouble() / fade)).toInt().toShort()
        }
        return pcm
    }

    private fun crearPista(pcm: ShortArray): AudioTrack {
        val pista = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        pista.write(pcm, 0, pcm.size)
        return pista
    }

    private fun liberarPista() {
        val pista = audioTrack
        if (pista != null) {
            try {
                pista.stop()
            } catch (e: IllegalStateException) {
                // ya estaba detenida
            }
            pista.release()
        }
        audioTrack = null
    }

    // ---------- Vibración ----------

    private fun obtenerVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun triggerHapticFeedback() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(30)
        }
    }

    // ---------- Ciclo de vida ----------

    fun shutdown() {
        detenerAudio()
        hilo.shutdownNow()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private companion object {
        const val ID_TTS = "sara"
        const val SAMPLE_RATE = 44100
        const val F_MIN = 220.0   // La3
        const val OCTAVAS = 2.0   // 220 Hz -> 880 Hz
        const val MAX_VOCES = 4
        const val AMPLITUD = 0.6
    }
}
