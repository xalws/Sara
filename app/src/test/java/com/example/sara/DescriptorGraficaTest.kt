package com.example.sara

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

class DescriptorGraficaTest {

    private fun describir(xMin: Double, xMax: Double, yMin: Double, yMax: Double, f: (Double) -> Double) =
        DescriptorGrafica.describir(f, xMin, xMax, yMin, yMax).also { println(it) }

    @Test
    fun lineal() {
        val resumen = describir(-4.5, 4.5, -10.0, 10.0) { 2 * it + 1 }
        assertTrue(resumen.contains("por debajo del eje X, y va subiendo"))
        assertTrue(resumen.contains("Cruza el eje X en x igual a menos 0.5."))
        assertTrue(resumen.contains("Cruza el eje Y en y igual a 1."))
    }

    @Test
    fun cuadratica() {
        val resumen = describir(-1.0, 5.0, -2.0, 8.0) { it * it - 4 * it + 3 }
        assertTrue(resumen.contains("punto más bajo en x igual a 2, y igual a menos 1"))
        assertTrue(resumen.contains("Cruza el eje X en x igual a 1."))
        assertTrue(resumen.contains("Cruza el eje X en x igual a 3."))
    }

    @Test
    fun cubica() {
        val resumen = describir(-2.5, 2.5, -9.0, 9.0) { it * it * it - 3 * it }
        assertTrue(resumen.contains("Cruza el eje X en x igual a menos 1.73."))
        assertTrue(resumen.contains("Pasa por el origen."))
        assertTrue(resumen.contains("punto más alto en x igual a menos 1, y igual a 2"))
    }

    @Test
    fun valorAbsoluto() {
        val resumen = describir(-3.0, 7.0, -2.0, 8.0) { abs(it - 2) + 1 }
        assertTrue(resumen.contains("punto más bajo en x igual a 2, y igual a 1"))
        assertTrue(!resumen.contains("Cruza el eje X"))
    }

    @Test
    fun exponencial() {
        val resumen = describir(-4.0, 4.0, -2.0, 17.0) { 2.0.pow(it) }
        assertTrue(resumen.contains("Cruza el eje Y en y igual a 1."))
    }

    @Test
    fun logaritmica() {
        val resumen = describir(-1.0, 8.0, -4.0, 4.0) { ln(it) / ln(2.0) }
        assertTrue(resumen.contains("No existe a la izquierda de x igual a 0. Ahí aparece, muy abajo"))
        assertTrue(resumen.contains("Cruza el eje X en x igual a 1."))
    }

    @Test
    fun seno() {
        val resumen = describir(-2 * PI, 2 * PI, -3.0, 3.0) { 2 * sin(it) }
        assertTrue(resumen.contains("Pasa por el origen."))
        assertTrue(resumen.contains("punto más alto en x igual a 1.57, y igual a 2"))
        assertTrue(!resumen.contains("6.28"))
    }

    @Test
    fun numeros() {
        assertEquals("menos 0.5", DescriptorGrafica.numero(-0.5))
        assertEquals("2", DescriptorGrafica.numero(1.9999))
        assertEquals("0", DescriptorGrafica.numero(-0.0001))
    }
}
