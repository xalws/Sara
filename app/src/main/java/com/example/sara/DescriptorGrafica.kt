package com.example.sara

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sign


object DescriptorGrafica {

    private class Suceso(val x: Double, val texto: String, val tipo: Int)

    private const val CRUCE_X = 1
    private const val CRUCE_Y = 2
    private const val OTRO = 0

    fun describir(
        f: (Double) -> Double,
        xMin: Double,
        xMax: Double,
        yMin: Double,
        yMax: Double
    ): String {
        val n = 800
        val pasoX = (xMax - xMin) / n
        val tolerancia = (yMax - yMin) * 1e-6
        val sucesos = mutableListOf<Suceso>()

        var inicio: String? = null
        var aparece: String? = null // en caso de que no exista a la izquierda
        var ultimoY = Double.NaN
        var prevX = Double.NaN
        var prevY = Double.NaN
        var direccion = 0

        for (i in 0..n) {
            val x = xMin + pasoX * i
            val y = f(x)
            if (!y.isFinite()) {
                if (prevY.isFinite()) {
                    sucesos += Suceso(x, "Deja de existir en x igual a ${numero(borde(f, prevX, x))}.", OTRO)
                }
                prevX = x
                prevY = Double.NaN
                continue
            }

            if (inicio == null) {
                if (i > 0) {
                    val xBorde = numero(borde(f, prevX, x))
                    aparece = "No existe a la izquierda de x igual a $xBorde. Ahí aparece"
                }
                inicio = posicion(y, yMin, yMax, tolerancia)
            } else if (prevY.isFinite()) {
                // Cruce con el eje Y
                if (prevX < 0.0 && x >= 0.0) {
                    val y0 = f(0.0)
                    if (y0.isFinite()) {
                        sucesos += Suceso(0.0, "Cruza el eje Y en y igual a ${numero(y0)}.", CRUCE_Y)
                    }
                }
                // Cruce con el eje X
                if (i < n && ((prevY < 0.0 && y >= 0.0) || (prevY > 0.0 && y <= 0.0))) {
                    val raiz = biseccion(f, prevX, x)
                    sucesos += Suceso(raiz, "Cruza el eje X en x igual a ${numero(raiz)}.", CRUCE_X)
                }
                // Cambios de dirección
                val cambio = if (abs(y - prevY) < tolerancia) 0 else sign(y - prevY).toInt()
                if (cambio != 0) {
                    if (direccion != 0 && cambio != direccion) {
                        val esMaximo = direccion > 0
                        val xExtremo = extremo(f, x - 2 * pasoX, x, esMaximo)
                        val yExtremo = f(xExtremo)
                        sucesos += Suceso(
                            xExtremo,
                            if (esMaximo) {
                                "Llega a su punto más alto en x igual a ${numero(xExtremo)}, y igual a ${numero(yExtremo)}, y empieza a bajar."
                            } else {
                                "Llega a su punto más bajo en x igual a ${numero(xExtremo)}, y igual a ${numero(yExtremo)}, y empieza a subir."
                            },
                            OTRO
                        )
                    }
                    if (direccion == 0) {
                        inicio += if (cambio > 0) ", y va subiendo" else ", y va bajando"
                    }
                    direccion = cambio
                }
            }
            ultimoY = y
            prevX = x
            prevY = y
        }

        if (inicio == null) return "En esta parte del plano la gráfica no existe."

        // Si cruza ambos ejes en (0, 0) se dice una sola vez
        val enOrigenX = sucesos.firstOrNull { it.tipo == CRUCE_X && abs(it.x) < pasoX }
        val enOrigenY = sucesos.firstOrNull { it.tipo == CRUCE_Y && abs(f(0.0)) < tolerancia * 1000 }
        if (enOrigenX != null && enOrigenY != null) {
            sucesos.remove(enOrigenX)
            sucesos.remove(enOrigenY)
            sucesos += Suceso(0.0, "Pasa por el origen.", OTRO)
        }

        val texto = StringBuilder(
            if (aparece != null) "$aparece, $inicio." else "Empieza a la izquierda, $inicio."
        )
        sucesos.sortedBy { it.x }.forEach { texto.append(' ').append(it.texto) }
        if (ultimoY.isFinite()) {
            texto.append(" Termina a la derecha, ").append(posicion(ultimoY, yMin, yMax, tolerancia)).append('.')
        }
        return texto.toString()
    }

    private fun posicion(y: Double, yMin: Double, yMax: Double, tolerancia: Double): String = when {
        y < yMin -> "muy abajo, fuera del plano"
        y > yMax -> "muy arriba, fuera del plano"
        abs(y) <= tolerancia * 1000 -> "sobre el eje X"
        y < 0 -> "por debajo del eje X"
        else -> "por encima del eje X"
    }

    /** Punto entre izquierda y derecha donde la función pasa de existir a no existir */
    private fun borde(f: (Double) -> Double, izquierdaInicial: Double, derechaInicial: Double): Double {
        var izquierda = izquierdaInicial
        var derecha = derechaInicial
        val existeIzquierda = f(izquierda).isFinite()
        repeat(50) {
            val medio = (izquierda + derecha) / 2
            if (f(medio).isFinite() == existeIzquierda) izquierda = medio else derecha = medio
        }
        return (izquierda + derecha) / 2
    }

    private fun biseccion(f: (Double) -> Double, izquierdaInicial: Double, derechaInicial: Double): Double {
        var izquierda = izquierdaInicial
        var derecha = derechaInicial
        val fInicial = f(izquierda)
        repeat(50) {
            val medio = (izquierda + derecha) / 2
            if (sign(f(medio)) == sign(fInicial)) izquierda = medio else derecha = medio
        }
        return (izquierda + derecha) / 2
    }

    /** Búsqueda ternaria del punto más alto o mas bajo entre izquierda y derecha. */
    private fun extremo(f: (Double) -> Double, izquierdaInicial: Double, derechaInicial: Double, maximo: Boolean): Double {
        var izquierda = izquierdaInicial
        var derecha = derechaInicial
        repeat(60) {
            val tercio1 = izquierda + (derecha - izquierda) / 3
            val tercio2 = derecha - (derecha - izquierda) / 3
            val mejorIzq = if (maximo) f(tercio1) > f(tercio2) else f(tercio1) < f(tercio2)
            if (mejorIzq) derecha = tercio2 else izquierda = tercio1
        }
        return (izquierda + derecha) / 2
    }

    fun numero(valor: Double): String {
        val redondeado = (valor * 100).roundToLong() / 100.0
        val texto = if (redondeado == redondeado.roundToLong().toDouble()) {
            redondeado.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.2f", redondeado).trimEnd('0')
        }
        return if (texto.startsWith("-")) {
            if (texto == "-0") "0" else "menos " + texto.drop(1)
        } else {
            texto
        }
    }
}
