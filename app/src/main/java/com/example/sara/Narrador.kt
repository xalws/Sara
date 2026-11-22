package com.example.sara

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
Voz de la app para guiar la escucha de una gráfica.
A diferencia de TalkBack, avisa cuándo termina de hablar, así se puede
encadenar: hablar -> tono -> hablar -> recorrido.
 */
class Narrador(contexto: Context) : TextToSpeech.OnInitListener {

    private val manejador = Handler(Looper.getMainLooper())
    private var motorVoz: TextToSpeech? = TextToSpeech(contexto.applicationContext, this)
    private var estado = INICIANDO
    private var pendiente: Pair<String, (() -> Unit)?>? = null
    private val alTerminar = ConcurrentHashMap<String, () -> Unit>()
    private var contador = 0

    override fun onInit(estadoInicio: Int) {
        val motor = motorVoz
        if (motor == null || estadoInicio != TextToSpeech.SUCCESS) {
            estado = NO_DISPONIBLE
            pendiente?.second?.let { manejador.post(it) }
            pendiente = null
            return
        }
        val resultado = motor.setLanguage(Locale.forLanguageTag("es-MX"))
        if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
            motor.setLanguage(Locale.forLanguageTag("es"))
        }
        motor.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(idFrase: String) = Unit
            override fun onDone(idFrase: String) = avisar(idFrase)

            @Deprecated("Requerido por la API")
            override fun onError(idFrase: String) = avisar(idFrase)
        })
        estado = LISTO
        val pendienteActual = pendiente
        pendiente = null
        if (pendienteActual != null) decir(pendienteActual.first, pendienteActual.second)
    }

    /** Dice [texto] (interrumpe lo anterior) y al terminar llama a [luego]. */
    fun decir(texto: String, luego: (() -> Unit)? = null) {
        when (estado) {
            LISTO -> {
                val idFrase = "sara${contador++}"
                alTerminar.clear()
                if (luego != null) alTerminar[idFrase] = luego
                motorVoz?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, idFrase)
            }
            INICIANDO -> pendiente = texto to luego
            else -> if (luego != null) manejador.post(luego)
        }
    }

    fun callar() {
        alTerminar.clear()
        pendiente = null
        motorVoz?.stop()
    }

    fun cerrar() {
        callar()
        motorVoz?.shutdown()
        motorVoz = null
    }

    private fun avisar(idFrase: String) {
        alTerminar.remove(idFrase)?.let { manejador.post(it) }
    }

    private companion object {
        const val INICIANDO = 0
        const val LISTO = 1
        const val NO_DISPONIBLE = -1
    }
}
