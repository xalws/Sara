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


class TecladoMatematico(
    private val actividad: Activity,
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

    val vista: LinearLayout = LinearLayout(actividad)
    private val contenedorTeclas = LinearLayout(actividad)
    private val filaPestanas = LinearLayout(actividad)
    private val pestanas = mutableListOf<Button>()
    private var pagina = 0

    init {
        vista.orientation = LinearLayout.VERTICAL
        vista.visibility = View.GONE
        vista.setPadding(0, aPixeles(15), 0, 0)

        contenedorTeclas.orientation = LinearLayout.VERTICAL
        filaPestanas.orientation = LinearLayout.HORIZONTAL

        val nombres = listOf("123", "f(x)", "abc")
        val voces = listOf("Números y operadores", "Funciones", "Letras")
        nombres.forEachIndexed { i, nombre ->
            val boton = crearBoton(nombre, 18f, voces[i])
            boton.layoutParams = LinearLayout.LayoutParams(0, aPixeles(44), 1f).apply {
                setMargins(aPixeles(3), aPixeles(3), aPixeles(3), aPixeles(3))
            }
            boton.setOnClickListener { mostrarPagina(i) }
            pestanas.add(boton)
            filaPestanas.addView(boton)
        }

        vista.addView(filaPestanas)
        vista.addView(contenedorTeclas)
        mostrarPagina(0)
    }

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
        val gestorTeclado = actividad.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        gestorTeclado.hideSoftInputFromWindow(campo.windowToken, 0)
    }


    private fun mostrarPagina(i: Int) {
        pagina = i
        pestanas.forEachIndexed { indice, boton ->
            boton.setBackgroundResource(if (indice == i) R.drawable.btn_amarillo else R.drawable.btn_contorno)
            boton.setTextColor(if (indice == i) Color.BLACK else AMARILLO)
        }
        contenedorTeclas.removeAllViews()
        val filas = when (i) {
            0 -> paginaNumeros()
            1 -> paginaFunciones()
            else -> paginaLetras()
        }
        filas.forEach { fila ->
            val linea = LinearLayout(actividad)
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

    private fun num(etiqueta: String, textoInsertar: String = etiqueta, voz: String = etiqueta): Tecla =
        Tecla(etiqueta, textoInsertar, voz, 0, 0, Accion.NINGUNA)

    private fun op(etiqueta: String, textoInsertar: String, voz: String): Tecla =
        Tecla(etiqueta, textoInsertar, voz, 0, 1, Accion.NINGUNA)

    private fun funcion(etiqueta: String, nombre: String, voz: String): Tecla =
        Tecla(etiqueta, nombre + "()", voz, 1, 1, Accion.NINGUNA)

    private fun esp(etiqueta: String, accion: Accion, voz: String): Tecla =
        Tecla(etiqueta, "", voz, 0, 2, accion)

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
            op("=", "=", "igual a"), funcion("√", "sqrt", "raíz cuadrada"),
            esp("←", Accion.IZQUIERDA, "mover cursor a la izquierda"),
            esp("→", Accion.DERECHA, "mover cursor a la derecha"),
            esp("⌫", Accion.BORRAR, "borrar"),
            esp("OK", Accion.ACEPTAR, "graficar y cerrar el teclado")
        )
    )

    private fun paginaFunciones(): List<List<Tecla>> = listOf(
        listOf(
            funcion("sen", "sen", "seno"), funcion("cos", "cos", "coseno"), funcion("tan", "tan", "tangente"),
            funcion("sen⁻¹", "arcsen", "arco seno"), funcion("cos⁻¹", "arccos", "arco coseno"),
            funcion("tan⁻¹", "arctan", "arco tangente")
        ),
        listOf(
            funcion("ln", "ln", "logaritmo natural"), funcion("log", "log", "logaritmo base diez"),
            funcion("√", "sqrt", "raíz cuadrada"), funcion("|x|", "abs", "valor absoluto"),
            funcion("eˣ", "exp", "e elevado a"), num("π", "pi", "pi")
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

    // vistas
    private fun crearBoton(texto: String, tamano: Float, voz: String): Button {
        val boton = Button(actividad)
        boton.text = texto
        boton.textSize = tamano
        boton.isAllCaps = false
        boton.minWidth = 0
        boton.minHeight = 0
        boton.minimumWidth = 0
        boton.minimumHeight = 0
        boton.setPadding(0, 0, 0, 0)
        boton.setTextColor(Color.BLACK)
        boton.contentDescription = voz
        return boton
    }

    private fun crearTecla(tecla: Tecla): Button {
        val grande = tecla.etiqueta.length <= 2
        val boton = crearBoton(tecla.etiqueta, if (grande) 20f else 16f, tecla.voz)

        val fondo = GradientDrawable()
        fondo.cornerRadius = aPixeles(12).toFloat()
        // Números: borde blanco. Operadores y funciones: borde amarillo. Especiales: amarillo sólido.
        when (tecla.tipo) {
            0 -> {
                fondo.setColor(FONDO_TECLA)
                fondo.setStroke(aPixeles(2), Color.WHITE)
                boton.setTextColor(Color.WHITE)
            }
            1 -> {
                fondo.setColor(FONDO_TECLA)
                fondo.setStroke(aPixeles(2), AMARILLO)
                boton.setTextColor(Color.WHITE)
            }
            else -> fondo.setColor(AMARILLO)
        }
        boton.background = fondo
        boton.layoutParams = LinearLayout.LayoutParams(0, aPixeles(50), 1f).apply {
            setMargins(aPixeles(3), aPixeles(3), aPixeles(3), aPixeles(3))
        }

        boton.setOnClickListener {
            boton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            pulsar(tecla)
        }
        if (tecla.accion == Accion.BORRAR) {
            boton.setOnLongClickListener {
                campo.text.clear()
                true
            }
        }
        return boton
    }

    // ---------- Edición del texto ----------

    private fun pulsar(tecla: Tecla) {
        when (tecla.accion) {
            Accion.BORRAR -> borrar()
            Accion.IZQUIERDA -> mover(-1)
            Accion.DERECHA -> mover(1)
            Accion.ACEPTAR -> {
                ocultar()
                alAceptar()
            }
            Accion.NINGUNA -> insertar(tecla.insertar, tecla.atras)
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

    private fun aPixeles(valor: Int): Int = (valor * actividad.resources.displayMetrics.density).toInt()

    private companion object {
        val AMARILLO = Color.parseColor("#FFD600")
        val FONDO_TECLA = Color.parseColor("#1C1C1C")
    }
}
