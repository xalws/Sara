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
  Voz (TTS), sonificación de gráficas y vibración.
  Sonificación: eje X = tiempo (izquierda -> derecha), altura de la curva = tono (grave -> agudo).
  Si la curva tiene varias ramas en un mismo x, suenan varias voces a la vez.
 */
class MotorAccesibilidad(contexto: Context) : TextToSpeech.OnInitListener {

    private val contextoApp = contexto.applicationContext
    private val candado = Any()
    private val hilo = Executors.newSingleThreadExecutor()
    private var version = 0

    private var motorVoz: TextToSpeech? = TextToSpeech(contextoApp, this)
    private var vozLista = false
    private var textoPendiente: String? = null

    private val vibrador: Vibrator? = obtenerVibrador()
    private var pistaAudio: AudioTrack? = null

    // voz

    override fun onInit(estadoInicio: Int) {
        val motor = motorVoz ?: return
        if (estadoInicio != TextToSpeech.SUCCESS) return
        val resultado = motor.setLanguage(Locale.forLanguageTag("es-MX"))
        if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
            motor.setLanguage(Locale.forLanguageTag("es"))
        }
        vozLista = true
        val pendiente = textoPendiente
        if (pendiente != null) {
            motor.speak(pendiente, TextToSpeech.QUEUE_FLUSH, null, ID_VOZ)
        }
        textoPendiente = null
    }

    fun hablar(texto: String) {
        val motor = motorVoz ?: return
        if (vozLista) {
            motor.speak(texto, TextToSpeech.QUEUE_FLUSH, null, ID_VOZ)
        } else {
            textoPendiente = texto // aviso cuando tts se inicia
        }
    }

    // sonificacion

    /**
      series[i] = valores de y (0 a 4 ramas) en el i paso de x.
      Se normaliza al rango real de la curva dentro de la ventana [yMin, yMax].
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

    // Tono corto para un valor de y
    fun tocarTonoParaY(valorY: Double, yMin: Double = -10.0, yMax: Double = 10.0) {
        val altura = ((valorY - yMin) / (yMax - yMin)).coerceIn(0.0, 1.0)
        reproducirNormalizado(List(3) { listOf(altura) }, 60)
    }

    fun detenerAudio() {
        synchronized(candado) {
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
        var minimo = dentro.minOrNull() ?: yMin
        var maximo = dentro.maxOrNull() ?: yMax
        if (maximo - minimo < 1e-6 * (yMax - yMin)) { // función constante -> tono medio
            minimo -= 1.0
            maximo += 1.0
        }
        return series.map { paso ->
            paso.filter { it.isFinite() }.map { (it.coerceIn(minimo, maximo) - minimo) / (maximo - minimo) }
        }
    }

    private fun frecuenciaDe(altura: Double): Double = FRECUENCIA_MINIMA * 2.0.pow(altura.coerceIn(0.0, 1.0) * OCTAVAS_GRAFICA)

    private fun reproducirNormalizado(pasos: List<List<Double>>, msPorPaso: Int) {
        val mi: Int
        synchronized(candado) {
            version++
            mi = version
            liberarPista()
        }
        hilo.execute {
            val muestrasAudio = sintetizar(pasos, msPorPaso)
            if (muestrasAudio != null) {
                val pista = try {
                    crearPista(muestrasAudio)
                } catch (excepcion: Exception) {
                    null
                }
                if (pista != null) {
                    synchronized(candado) {
                        if (mi != version) {
                            pista.release()
                        } else {
                            pistaAudio = pista
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

        val muestrasPorPaso = TASA_MUESTREO * msPorPaso / 1000
        val muestrasAudio = ShortArray(muestrasPorPaso * pasos.size)
        val frecuencia = DoubleArray(voces) { FRECUENCIA_MINIMA }
        val amplitud = DoubleArray(voces)
        val fase = DoubleArray(voces)
        val suavizadoFrecuencia = 1.0 - exp(-1.0 / (0.012 * TASA_MUESTREO)) // glissando suave entre pasos
        val suavizadoAmplitud = 1.0 - exp(-1.0 / (0.003 * TASA_MUESTREO))  // evita clics al entrar/salir una voz

        var pos = 0
        for (paso in pasos) {
            val presentes = minOf(paso.size, voces)
            val frecuenciaObjetivo = DoubleArray(voces) { voz -> if (voz < presentes) frecuenciaDe(paso[voz]) else frecuencia[voz] }
            val amplitudObjetivo = DoubleArray(voces) { voz -> if (voz < presentes) AMPLITUD / presentes else 0.0 }
            for (voz in 0 until presentes) {
                if (amplitud[voz] < 0.001) frecuencia[voz] = frecuenciaObjetivo[voz]
            }

            for (n in 0 until muestrasPorPaso) {
                var suma = 0.0
                for (voz in 0 until voces) {
                    frecuencia[voz] += (frecuenciaObjetivo[voz] - frecuencia[voz]) * suavizadoFrecuencia
                    amplitud[voz] += (amplitudObjetivo[voz] - amplitud[voz]) * suavizadoAmplitud
                    fase[voz] += 2.0 * PI * frecuencia[voz] / TASA_MUESTREO
                    if (fase[voz] > 2.0 * PI) fase[voz] -= 2.0 * PI
                    suma += amplitud[voz] * sin(fase[voz])
                }
                muestrasAudio[pos] = (suma.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                pos++
            }
        }
        //20 ms de fundido
        val fundido = minOf(muestrasAudio.size, TASA_MUESTREO / 50)
        for (i in 0 until fundido) {
            val indice = muestrasAudio.size - 1 - i
            muestrasAudio[indice] = (muestrasAudio[indice] * (i.toDouble() / fundido)).toInt().toShort()
        }
        return muestrasAudio
    }

    private fun crearPista(muestrasAudio: ShortArray): AudioTrack {
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
                    .setSampleRate(TASA_MUESTREO)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(muestrasAudio.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        pista.write(muestrasAudio, 0, muestrasAudio.size)
        return pista
    }

    private fun liberarPista() {
        val pista = pistaAudio
        if (pista != null) {
            try {
                pista.stop()
            } catch (excepcion: IllegalStateException) {
                // ya estaba detenida
            }
            pista.release()
        }
        pistaAudio = null
    }

    // vibracion

    private fun obtenerVibrador(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contextoApp.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            contextoApp.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrarBreve() {
        val dispositivo = vibrador ?: return
        if (!dispositivo.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dispositivo.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            dispositivo.vibrate(30)
        }
    }

    // ---------- Ciclo de vida ----------

    fun apagar() {
        detenerAudio()
        hilo.shutdownNow()
        motorVoz?.stop()
        motorVoz?.shutdown()
        motorVoz = null
    }

    private companion object {
        const val ID_VOZ = "sara"
        const val MAX_VOCES = 4
        const val AMPLITUD = 0.6
    }
}
