package com.example.sara

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin


class TonoContinuo {

    /** Alturas de 0 (abajo) a 1 (arriba), ordenadas de menor a mayor. Vacío = silencio. */
    @Volatile private var objetivoAlturas = DoubleArray(0)
    @Volatile private var objetivoBalance = 0.5
    @Volatile private var generacion = 0
    private val tics = AtomicInteger(0)
    private val bips = AtomicInteger(0)

    fun iniciar() {
        generacion++
        val mi = generacion
        objetivoAlturas = DoubleArray(0)
        tics.set(0)
        bips.set(0)
        Thread { bucle(mi) }.start()
    }

    /** [altura] de 0 (abajo) a 1 (arriba), o null si ahí no hay gráfica. [balance] de 0 (izq.) a 1 (der.). */
    fun mover(altura: Double?, balance: Double) {
        mover(if (altura == null) emptyList() else listOf(altura), balance)
    }

    /** Una altura por rama de la curva (lista vacía = no hay grafica). */
    fun mover(alturas: List<Double>, balance: Double) {
        objetivoBalance = balance
        objetivoAlturas = alturas.take(MAX_VOCES).sorted().toDoubleArray()
    }

    fun marcarEjeY() { tics.incrementAndGet() }

    fun marcarEjeX() { bips.incrementAndGet() }

    fun detener() { generacion++ }

    private fun bucle(mi: Int) {
        val pista = try {
            crearPista()
        } catch (excepcion: Exception) {
            return
        }
        pista.play()

        val cuadros = 256
        val bufer = ShortArray(cuadros * 2)
        val suavizadoFrecuencia = 1.0 - exp(-1.0 / (0.010 * TASA_MUESTREO))
        val suavizadoAmplitud = 1.0 - exp(-1.0 / (0.006 * TASA_MUESTREO))
        val frecuencia = DoubleArray(MAX_VOCES) { frecuenciaParaAltura(0.5) }
        val amplitud = DoubleArray(MAX_VOCES)
        val fase = DoubleArray(MAX_VOCES)
        val frecuenciaObjetivo = DoubleArray(MAX_VOCES)
        val amplitudObjetivo = DoubleArray(MAX_VOCES)
        var balance = objetivoBalance
        var faseMarca = 0.0
        var ticRestante = 0
        var bipRestante = 0
        val largoTic = TASA_MUESTREO * 25 / 1000
        val largoBip = TASA_MUESTREO * 70 / 1000

        try {
            while (true) {
                val vivo = mi == generacion
                if (!vivo && amplitud.all { it < 0.0005 } && ticRestante == 0 && bipRestante == 0) break
                if (tics.getAndSet(0) > 0) ticRestante = largoTic
                if (bips.getAndSet(0) > 0) bipRestante = largoBip

                val alturas = objetivoAlturas
                val presentes = if (vivo) alturas.size else 0
                for (voz in 0 until MAX_VOCES) {
                    // Una voz que se apaga conserva su tono mientras se desvanece
                    frecuenciaObjetivo[voz] =
                        if (voz < alturas.size) frecuenciaParaAltura(alturas[voz]) else frecuencia[voz]
                    amplitudObjetivo[voz] = if (voz < presentes) AMPLITUD_GRAFICA / presentes else 0.0
                    // Al reaparecer, la voz entra directo en su tono (sin glissando desde el anterior)
                    if (voz < presentes && amplitud[voz] < 0.001) frecuencia[voz] = frecuenciaObjetivo[voz]
                }
                val balanceObjetivo = objetivoBalance

                for (n in 0 until cuadros) {
                    balance += (balanceObjetivo - balance) * suavizadoAmplitud
                    var muestra = 0.0
                    for (voz in 0 until MAX_VOCES) {
                        frecuencia[voz] += (frecuenciaObjetivo[voz] - frecuencia[voz]) * suavizadoFrecuencia
                        amplitud[voz] += (amplitudObjetivo[voz] - amplitud[voz]) * suavizadoAmplitud
                        fase[voz] += 2.0 * PI * frecuencia[voz] / TASA_MUESTREO
                        if (fase[voz] > 2.0 * PI) fase[voz] -= 2.0 * PI
                        muestra += amplitud[voz] * sin(fase[voz])
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

                    bufer[2 * n] = aMuestra(muestra * cos(balance * PI / 2))
                    bufer[2 * n + 1] = aMuestra(muestra * sin(balance * PI / 2))
                }
                pista.write(bufer, 0, bufer.size)
            }
        } finally {
            try {
                pista.stop()
            } catch (excepcion: IllegalStateException) {
            }
            pista.release()
        }
    }

    private fun crearPista(): AudioTrack {
        val formato = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(TASA_MUESTREO)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val minimo = AudioTrack.getMinBufferSize(
            TASA_MUESTREO, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )
        val constructorPista = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(formato)
            .setBufferSizeInBytes(maxOf(minimo, 256 * 4 * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            constructorPista.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }
        return constructorPista.build()
    }

    companion object {
        //para los que se pueden escuchar a la vez
        const val MAX_VOCES = 4
    }
}
