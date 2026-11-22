package com.example.sara

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button


class ControladorInicio(actividad: Activity, raiz: View, alComenzar: () -> Unit) {

    private val btnEjemplo: Button = raiz.findViewById(R.id.btnEjemploSonido)
    private val textoEscuchar = actividad.getString(R.string.escucharejemplo)
    private val textoDetener = actividad.getString(R.string.detenerejemplo)

    private val sonido = SonidoGrafica()
    private val vibracion = Vibracion(actividad)
    private var narrador: Narrador? = Narrador(actividad)
    private val accesibilidad = actividad.getSystemService(AccessibilityManager::class.java)
    private val manejador = Handler(Looper.getMainLooper())

    /** Cada acción nueva incrementa el turno los antiguos se ignoran */
    private var turno = 0
    private var sonando = false

    private sealed class Paso {
        class Decir(val texto: String) : Paso()
        class Tono(val altura: Double) : Paso()
        object Recorrido : Paso()
    }

    private val pasos = listOf(
        Paso.Decir(
            "Así se escucha una gráfica en Sara. El tono indica la altura. " +
                "Así suena la parte más baja del plano."
        ),
        Paso.Tono(0.0),
        Paso.Decir("Así suena la parte más alta."),
        Paso.Tono(1.0),
        Paso.Decir("Y así suena la altura del eje X, cuando está a la mitad del plano."),
        Paso.Tono(0.5),
        Paso.Decir(
            "El sonido se mueve del oído izquierdo al derecho, igual que la gráfica avanza de izquierda a derecha. " +
                "Un bip indica que la gráfica cruza el eje X, y un tic, que cruza el eje Y. " +
                "Escucha una recta que sube: primero cruza el eje X y después el eje Y."
        ),
        Paso.Recorrido,
        Paso.Decir("Cuando hay silencio, la gráfica no existe en esa parte o se sale del plano. Fin del ejemplo.")
    )

    init {
        btnEjemplo.setOnClickListener { if (sonando) detener() else reproducir() }
        raiz.findViewById<Button>(R.id.btnComenzar).setOnClickListener { alComenzar() }
    }

    private fun reproducir() {
        cancelar()
        sonando = true
        // Se cambia el texto antes de esperar así TalkBack lo anuncia antes del sonido
        btnEjemplo.text = textoDetener
        val mi = turno
        // se espera a que talkback termine
        val espera = if (accesibilidad?.isTouchExplorationEnabled == true) 1500L else 100L
        manejador.postDelayed({ ejecutar(mi, pasos) }, espera)
    }

    fun detener() {
        cancelar()
        sonando = false
        btnEjemplo.text = textoEscuchar
    }

    private fun cancelar() {
        turno++
        manejador.removeCallbacksAndMessages(null)
        sonido.detener()
        narrador?.callar()
    }

    private fun ejecutar(mi: Int, pendientes: List<Paso>) {
        if (mi != turno) return
        if (pendientes.isEmpty()) {
            detener()
            return
        }
        val resto = pendientes.drop(1)
        when (val paso = pendientes.first()) {
            is Paso.Decir -> {
                val voz = narrador ?: return
                voz.decir(paso.texto) { ejecutar(mi, resto) }
            }
            is Paso.Tono -> {
                sonido.reproducirTono(paso.altura, DURACION_TONO_MS)
                manejador.postDelayed({ ejecutar(mi, resto) }, DURACION_TONO_MS + 250L)
            }
            Paso.Recorrido -> recorrer(mi, resto)
        }
    }

    private fun recorrer(mi: Int, resto: List<Paso>) {
        sonido.reproducir({ x -> 0.6 * x + 1.5 }, -5.0, 5.0, -5.0, 5.0, DURACION_RECORRIDO_MS) { eventos ->
            manejador.post {
                if (mi == turno) {
                    for (eventoEje in eventos) {
                        manejador.postDelayed({
                            if (mi == turno) {
                                if (eventoEje.tipo == TipoEje.EJE_X) vibracion.ejeX() else vibracion.ejeY()
                            }
                        }, (eventoEje.progreso * DURACION_RECORRIDO_MS).toLong())
                    }
                    manejador.postDelayed({ ejecutar(mi, resto) }, DURACION_RECORRIDO_MS + 300L)
                }
            }
        }
    }

    fun destruir() {
        cancelar()
        sonando = false
        narrador?.cerrar()
        narrador = null
    }

    private companion object {
        const val DURACION_RECORRIDO_MS = 6000
        const val DURACION_TONO_MS = 900
    }
}
