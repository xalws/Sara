package com.example.sara

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

/**
 * Teclado matemático propio (estilo GeoGebra) para escribir funciones.
 * Tres páginas: 123 (números y operadores), f(x) (funciones) y abc (letras).
 * Cada tecla tiene descripción para TalkBack.
 */
class TecladoMatematico(
    private val activity: Activity,
    private val campo: EditText,
    private val alAceptar: () -> Unit
) {

    private enum class Accion { NINGUNA, BORRAR, IZQUIERDA, DERECHA, ACEPTAR }

    private class Tecla(
        val etiqueta: String,
        val insertar: String,
        val voz: String,
        val atras: Int,
        val tipo: Int,
        val accion: Accion
    )

    val vista: LinearLayout = LinearLayout(activity)
    private val contenedorTeclas = LinearLayout(activity)
    private val filaPestanas = LinearLayout(activity)
    private val pestanas = mutableListOf<Button>()
    private var pagina = 0

    init {
        vista.orientation = LinearLayout.VERTICAL
        vista.visibility = View.GONE
        vista.setPadding(0, dp(15), 0, 0)

        contenedorTeclas.orientation = LinearLayout.VERTICAL
        filaPestanas.orientation = LinearLayout.HORIZONTAL

        val nombres = listOf("123", "f(x)", "abc")
        val voces = listOf("Números y operadores", "Funciones", "Letras")
        nombres.forEachIndexed { i, nombre ->
            val b = crearBoton(nombre, 18f, voces[i])
            b.layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            b.setOnClickListener { mostrarPagina(i) }
            pestanas.add(b)
            filaPestanas.addView(b)
        }

        vista.addView(filaPestanas)
        vista.addView(contenedorTeclas)
        mostrarPagina(0)
    }

    // ---------- Mostrar / ocultar ----------

    fun mostrar() {
        ocultarSistema()
        if (vista.visibility != View.VISIBLE) {
            vista.visibility = View.VISIBLE
            vista.post {
                vista.requestRectangleOnScreen(Rect(0, 0, vista.width, vista.height), false)
            }
        }
    }

    fun ocultar() {
        vista.visibility = View.GONE
        ocultarSistema()
    }



    private fun ocultarSistema() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(campo.windowToken, 0)
    }

    // ---------- Páginas ----------

    private fun mostrarPagina(i: Int) {
        pagina = i
        pestanas.forEachIndexed { n, b ->
            b.setBackgroundResource(if (n == i) R.drawable.btn_amarillo else R.drawable.btn_morado)
        }
        contenedorTeclas.removeAllViews()
        val filas = when (i) {
            0 -> paginaNumeros()
            1 -> paginaFunciones()
            else -> paginaLetras()
        }
        filas.forEach { fila ->
            val linea = LinearLayout(activity)
            linea.orientation = LinearLayout.HORIZONTAL
            fila.forEach { tecla -> linea.addView(crearTecla(tecla)) }
            contenedorTeclas.addView(
                linea,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun num(e: String, ins: String = e, voz: String = e): Tecla =
        Tecla(e, ins, voz, 0, 0, Accion.NINGUNA)

    private fun op(e: String, ins: String, voz: String): Tecla =
        Tecla(e, ins, voz, 0, 1, Accion.NINGUNA)

    private fun fn(e: String, nombre: String, voz: String): Tecla =
        Tecla(e, nombre + "()", voz, 1, 1, Accion.NINGUNA)

    private fun esp(e: String, accion: Accion, voz: String): Tecla =
        Tecla(e, "", voz, 0, 2, accion)

    private fun paginaNumeros(): List<List<Tecla>> = listOf(
        listOf(num("x"), num("y"), num("7"), num("8"), num("9"), op("÷", "/", "dividido entre")),
        listOf(num("a"), num("b"), num("4"), num("5"), num("6"), op("×", "*", "por")),
        listOf(
            op("x²", "^2", "al cuadrado"), op("^", "^", "elevado a"),
            num("1"), num("2"), num("3"), op("−", "-", "menos")
        ),
        listOf(
            op("(", "(", "abre paréntesis"), op(")", ")", "cierra paréntesis"),
            num("0"), num(".", ".", "punto"), num("π", "pi", "pi"), op("+", "+", "más")
        ),
        listOf(
            op("=", "=", "igual a"), fn("√", "sqrt", "raíz cuadrada"),
            esp("←", Accion.IZQUIERDA, "mover cursor a la izquierda"),
            esp("→", Accion.DERECHA, "mover cursor a la derecha"),
            esp("⌫", Accion.BORRAR, "borrar"),
            esp("OK", Accion.ACEPTAR, "graficar y cerrar el teclado")
        )
    )

    private fun paginaFunciones(): List<List<Tecla>> = listOf(
        listOf(
            fn("sen", "sen", "seno"), fn("cos", "cos", "coseno"), fn("tan", "tan", "tangente"),
            fn("sen⁻¹", "arcsen", "arco seno"), fn("cos⁻¹", "arccos", "arco coseno"),
            fn("tan⁻¹", "arctan", "arco tangente")
        ),
        listOf(
            fn("ln", "ln", "logaritmo natural"), fn("log", "log", "logaritmo base diez"),
            fn("√", "sqrt", "raíz cuadrada"), fn("|x|", "abs", "valor absoluto"),
            fn("eˣ", "exp", "e elevado a"), num("π", "pi", "pi")
        ),
        listOf(
            op("(", "(", "abre paréntesis"), op(")", ")", "cierra paréntesis"),
            op("^", "^", "elevado a"), op("÷", "/", "dividido entre"),
            op("×", "*", "por"), esp("⌫", Accion.BORRAR, "borrar")
        ),
        listOf(
            num("x"), num("y"), op("=", "=", "igual a"),
            esp("←", Accion.IZQUIERDA, "mover cursor a la izquierda"),
            esp("→", Accion.DERECHA, "mover cursor a la derecha"),
            esp("OK", Accion.ACEPTAR, "graficar y cerrar el teclado")
        )
    )

    private fun paginaLetras(): List<List<Tecla>> = listOf(
        "abcdefg".map { num(it.toString()) },
        "hijklmn".map { num(it.toString()) },
        "opqrstu".map { num(it.toString()) },
        "vwxyz".map { num(it.toString()) } + listOf(
            esp("⌫", Accion.BORRAR, "borrar"),
            esp("OK", Accion.ACEPTAR, "graficar y cerrar el teclado")
        )
    )

    // ---------- Vistas ----------

    private fun crearBoton(texto: String, tamano: Float, voz: String): Button {
        val b = Button(activity)
        b.text = texto
        b.textSize = tamano
        b.isAllCaps = false
        b.minWidth = 0
        b.minHeight = 0
        b.minimumWidth = 0
        b.minimumHeight = 0
        b.setPadding(0, 0, 0, 0)
        b.setTextColor(Color.BLACK)
        b.contentDescription = voz
        return b
    }

    private fun crearTecla(t: Tecla): Button {
        val grande = t.etiqueta.length <= 2
        val b = crearBoton(t.etiqueta, if (grande) 20f else 16f, t.voz)

        val fondo = GradientDrawable()
        fondo.cornerRadius = dp(12).toFloat()
        when (t.tipo) {
            0 -> {
                fondo.setColor(Color.parseColor("#4f4f4f"))
                b.setTextColor(Color.WHITE)
            }
            1 -> fondo.setColor(Color.parseColor("#ee03ff"))
            else -> fondo.setColor(Color.parseColor("#ffb905"))
        }
        b.background = fondo
        b.layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply {
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }

        b.setOnClickListener {
            b.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            pulsar(t)
        }
        if (t.accion == Accion.BORRAR) {
            b.setOnLongClickListener {
                campo.text.clear()
                true
            }
        }
        return b
    }

    // ---------- Edición del texto ----------

    private fun pulsar(t: Tecla) {
        when (t.accion) {
            Accion.BORRAR -> borrar()
            Accion.IZQUIERDA -> mover(-1)
            Accion.DERECHA -> mover(1)
            Accion.ACEPTAR -> {
                ocultar()
                alAceptar()
            }
            Accion.NINGUNA -> insertar(t.insertar, t.atras)
        }
    }

    private fun insertar(texto: String, atras: Int) {
        val ini = maxOf(campo.selectionStart, 0)
        val fin = maxOf(campo.selectionEnd, 0)
        val desde = minOf(ini, fin)
        val hasta = maxOf(ini, fin)
        campo.text.replace(desde, hasta, texto)
        campo.setSelection(desde + texto.length - atras)
    }

    private fun borrar() {
        val ini = maxOf(campo.selectionStart, 0)
        val fin = maxOf(campo.selectionEnd, 0)
        val desde = minOf(ini, fin)
        val hasta = maxOf(ini, fin)
        if (desde != hasta) {
            campo.text.delete(desde, hasta)
        } else if (desde > 0) {
            campo.text.delete(desde - 1, desde)
        }
    }

    private fun mover(paso: Int) {
        val pos = (campo.selectionStart + paso).coerceIn(0, campo.text.length)
        campo.setSelection(pos)
    }

    private fun dp(v: Int): Int = (v * activity.resources.displayMetrics.density).toInt()
}
