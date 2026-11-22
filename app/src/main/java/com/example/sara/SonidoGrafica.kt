package com.example.sara

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

// ---------- Reglas de sonificación compartidas (recorrido, exploración y referencias) ----------

internal const val TASA_MUESTREO = 44100
internal const val FRECUENCIA_MINIMA = 220.0 // parte baja del plano
internal const val OCTAVAS_GRAFICA = 2.0     // parte alta del plano: 880 Hz
internal const val AMPLITUD_GRAFICA = 0.5
internal const val FRECUENCIA_TIC = 1200.0   // cruce del eje Y
internal const val FRECUENCIA_BIP = 1760.0   // cruce del eje X

/** altura = 0 abajo del plano, altura = 1 arriba. */
internal fun frecuenciaParaAltura(altura: Double): Double =
    FRECUENCIA_MINIMA * 2.0.pow(altura.coerceIn(0.0, 1.0) * OCTAVAS_GRAFICA)

/** avance = 0 borde izquierdo, avance = 1 borde derecho. Nunca suena 100% en un solo oído. */
internal fun panoramaParaAvance(avance: Double): Double = 0.08 + 0.84 * avance

internal fun aMuestra(valor: Double): Short = (valor.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

enum class TipoEje { EJE_X, EJE_Y }

/** Momento del recorrido (0 a 1) en que la gráfica cruza un eje. */
data class EventoEje(val progreso: Float, val tipo: TipoEje)

/**
 * Convierte el recorrido de una gráfica en sonido estéreo:
 *  - Eje X = tiempo y posición: el sonido viaja del oído izquierdo al derecho.
 *  - Eje Y = tono: más alto en la gráfica, más agudo.
 *  - "Tic" al cruzar el eje Y (x = 0) y "bip" al cruzar o tocar el eje X (y = 0).
 *  - Silencio donde la función no existe o se sale de la ventana visible.
 */
class SonidoGrafica {

    private val candado = Any()
    private var version = 0
    private var pista: AudioTrack? = null
    private var totalMuestras = 0

    /** Progreso de 0 a 1 de la reproducción actual, o null si no está sonando. */
    fun progreso(): Float? {
        synchronized(candado) {
            val pistaActual = pista ?: return null
            if (totalMuestras == 0) return null
            val cabeza = try {
                pistaActual.playbackHeadPosition
            } catch (excepcion: IllegalStateException) {
                return null
            }
            if (cabeza >= totalMuestras) return null
            return cabeza.toFloat() / totalMuestras
        }
    }

    /** Recorre la gráfica; [alIniciar] recibe los cruces con los ejes (en un hilo de fondo). */
    fun reproducir(
        f: (Double) -> Double,
        xMin: Double,
        xMax: Double,
        yMin: Double,
        yMax: Double,
        duracionMs: Int = 6000,
        alIniciar: (List<EventoEje>) -> Unit = {}
    ) {
        reproducir(listOf(f), xMin, xMax, yMin, yMax, duracionMs, alIniciar)
    }

    /**
     * Recorre varias curvas a la vez, una voz por curva (por ejemplo, la mitad de arriba
     * y la de abajo de un círculo): en cada x suenan juntos los tonos de todas.
     */
    fun reproducir(
        curvas: List<(Double) -> Double>,
        xMin: Double,
        xMax: Double,
        yMin: Double,
        yMax: Double,
        duracionMs: Int = 6000,
        alIniciar: (List<EventoEje>) -> Unit = {}
    ) {
        lanzar(alIniciar) {
            val eventos = mutableListOf<EventoEje>()
            sintetizarRecorrido(curvas, xMin, xMax, yMin, yMax, duracionMs, eventos) to eventos
        }
    }

    /** Tono fijo al centro, para las referencias (abajo, arriba, eje X). */
    fun reproducirTono(altura: Double, duracionMs: Int) {
        lanzar({}) { sintetizarTono(altura, duracionMs) to emptyList() }
    }

    fun detener() {
        synchronized(candado) {
            version++
            liberar()
        }
    }

    private fun lanzar(
        alIniciar: (List<EventoEje>) -> Unit,
        sintetizar: () -> Pair<ShortArray, List<EventoEje>>
    ) {
        val mi: Int
        synchronized(candado) {
            version++
            mi = version
            liberar()
        }
        Thread {
            val (muestrasAudio, eventos) = sintetizar()
            val nueva = try {
                crearPista(muestrasAudio)
            } catch (excepcion: Exception) {
                null
            } ?: return@Thread
            synchronized(candado) {
                if (mi != version) {
                    nueva.release()
                } else {
                    pista = nueva
                    totalMuestras = muestrasAudio.size / 2
                    nueva.play()
                    alIniciar(eventos)
                }
            }
        }.start()
    }

    private fun sintetizarRecorrido(
        curvas: List<(Double) -> Double>,
        xMin: Double,
        xMax: Double,
        yMin: Double,
        yMax: Double,
        duracionMs: Int,
        eventos: MutableList<EventoEje>
    ): ShortArray {
        val n = TASA_MUESTREO * duracionMs / 1000
        val muestrasAudio = ShortArray(n * 2) // estéreo intercalado: L, R, L, R...
        val rangoY = yMax - yMin
        val saltoMaximo = rangoY * 0.02 // cambio brusco entre muestras = asíntota -> silencio

        val suavizadoFrecuencia = 1.0 - exp(-1.0 / (0.004 * TASA_MUESTREO))
        val suavizadoAmplitud = 1.0 - exp(-1.0 / (0.004 * TASA_MUESTREO))
        // Una voz por curva, cada una con su propio tono, volumen y fase
        val voces = curvas.size
        val frecuencia = DoubleArray(voces) { FRECUENCIA_MINIMA }
        val amplitud = DoubleArray(voces)
        val fase = DoubleArray(voces)
        val yAnterior = DoubleArray(voces) { Double.NaN }
        val visible = BooleanArray(voces)
        val y = DoubleArray(voces)

        var ticRestante = 0
        var bipRestante = 0
        var faseMarca = 0.0
        val largoTic = TASA_MUESTREO * 25 / 1000
        val largoBip = TASA_MUESTREO * 70 / 1000
        // Varias curvas pueden cruzar el eje X en el mismo punto (los extremos de un círculo):
        // solo se registra un evento para que el teléfono no vibre dos veces
        val separacionMinima = largoBip.toFloat() / n
        var ultimoBip = -1f

        var xAnterior = xMin

        for (i in 0 until n) {
            val avance = i.toDouble() / (n - 1)
            val x = xMin + (xMax - xMin) * avance

            if (xAnterior < 0.0 && x >= 0.0) {
                ticRestante = largoTic
                eventos += EventoEje(avance.toFloat(), TipoEje.EJE_Y)
            }

            var presentes = 0
            for (voz in 0 until voces) {
                y[voz] = curvas[voz](x)
                visible[voz] = y[voz].isFinite() && y[voz] >= yMin && y[voz] <= yMax &&
                    (yAnterior[voz].isNaN() || abs(y[voz] - yAnterior[voz]) < saltoMaximo)
                if (visible[voz]) presentes++

                if (visible[voz] && yAnterior[voz].isFinite() &&
                    ((yAnterior[voz] < 0.0 && y[voz] >= 0.0) || (yAnterior[voz] > 0.0 && y[voz] <= 0.0))
                ) {
                    bipRestante = largoBip
                    if (avance.toFloat() - ultimoBip > separacionMinima) {
                        eventos += EventoEje(avance.toFloat(), TipoEje.EJE_X)
                        ultimoBip = avance.toFloat()
                    }
                }
            }

            var muestra = 0.0
            for (voz in 0 until voces) {
                if (visible[voz]) {
                    val objetivo = frecuenciaParaAltura((y[voz] - yMin) / rangoY)
                    if (amplitud[voz] < 0.001) frecuencia[voz] = objetivo // al reaparecer, entra directo en su tono
                    frecuencia[voz] += (objetivo - frecuencia[voz]) * suavizadoFrecuencia
                }
                // El volumen se reparte entre las voces que suenan, para no saturar
                val amplitudObjetivo = if (visible[voz]) AMPLITUD_GRAFICA / presentes else 0.0
                amplitud[voz] += (amplitudObjetivo - amplitud[voz]) * suavizadoAmplitud
                fase[voz] += 2.0 * PI * frecuencia[voz] / TASA_MUESTREO
                if (fase[voz] > 2.0 * PI) fase[voz] -= 2.0 * PI
                muestra += amplitud[voz] * sin(fase[voz])
                yAnterior[voz] = if (y[voz].isFinite()) y[voz] else Double.NaN
            }

            if (ticRestante > 0) {
                val envolvente = ticRestante.toDouble() / largoTic
                faseMarca += 2.0 * PI * FRECUENCIA_TIC / TASA_MUESTREO
                muestra += 0.45 * envolvente * envolvente * sin(faseMarca)
                ticRestante--
            } else if (bipRestante > 0) {
                val envolvente = sin(PI * bipRestante / largoBip)
                faseMarca += 2.0 * PI * FRECUENCIA_BIP / TASA_MUESTREO
                muestra += 0.30 * envolvente * sin(faseMarca)
                bipRestante--
            }

            val balance = panoramaParaAvance(avance)
            muestrasAudio[2 * i] = aMuestra(muestra * cos(balance * PI / 2))
            muestrasAudio[2 * i + 1] = aMuestra(muestra * sin(balance * PI / 2))

            xAnterior = x
        }
        fundidoFinal(muestrasAudio, n)
        return muestrasAudio
    }

    private fun sintetizarTono(altura: Double, duracionMs: Int): ShortArray {
        val n = TASA_MUESTREO * duracionMs / 1000
        val muestrasAudio = ShortArray(n * 2)
        val frecuencia = frecuenciaParaAltura(altura)
        val entrada = TASA_MUESTREO / 50
        for (i in 0 until n) {
            val envolvente = minOf(1.0, i.toDouble() / entrada)
            val valor = AMPLITUD_GRAFICA * envolvente * sin(2.0 * PI * frecuencia * i / TASA_MUESTREO)
            val muestraTono = aMuestra(valor * cos(PI / 4))
            muestrasAudio[2 * i] = muestraTono
            muestrasAudio[2 * i + 1] = muestraTono
        }
        fundidoFinal(muestrasAudio, n)
        return muestrasAudio
    }

    private fun fundidoFinal(muestrasAudio: ShortArray, n: Int) {
        val fundido = minOf(n, TASA_MUESTREO / 50)
        for (k in 0 until fundido) {
            val indice = n - 1 - k
            val ganancia = k.toDouble() / fundido
            muestrasAudio[2 * indice] = (muestrasAudio[2 * indice] * ganancia).toInt().toShort()
            muestrasAudio[2 * indice + 1] = (muestrasAudio[2 * indice + 1] * ganancia).toInt().toShort()
        }
    }

    private fun crearPista(muestrasAudio: ShortArray): AudioTrack {
        val nueva = AudioTrack.Builder()
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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(muestrasAudio.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        nueva.write(muestrasAudio, 0, muestrasAudio.size)
        return nueva
    }

    private fun liberar() {
        val pistaActual = pista ?: return
        try {
            pistaActual.stop()
        } catch (excepcion: IllegalStateException) {
            // ya estaba detenida
        }
        pistaActual.release()
        pista = null
        totalMuestras = 0
    }
}
