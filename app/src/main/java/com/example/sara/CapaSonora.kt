package com.example.sara

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import kotlin.math.abs
import kotlin.math.roundToInt

/**
capa transparente para evaluar la grafica con el tacto cuando se usa geogebra
 */
class CapaSonora @JvmOverloads constructor(
    contexto: Context,
    atributos: AttributeSet? = null
) : View(contexto, atributos) {

    /** columnas[i] = valores de y visibles (0 a 4 ramas) en la iesima x, de izquierda a derecha. */
    private class Mapa(
        val xMin: Double, val xMax: Double,
        val yMin: Double, val yMax: Double,
        val columnas: List<DoubleArray>
    ) {
        fun x(columna: Int): Double = xMin + (xMax - xMin) * columna / (columnas.size - 1)
    }

    private var mapa: Mapa? = null

    /** Sin TalkBack: true = un dedo explora con sonido, false = el dedo mueve la gráfica. */
    var exploracionActiva = false

    /** comienza al usar talkback o explorar esto para detener el resto de los sonidos (aveces no cumple su funcion) */
    var alEmpezar: (() -> Unit)? = null

    /** Con TalkBack: tocar dos veces la gráfica. */
    var alTocarDosVeces: (() -> Unit)? = null

    private val tono = TonoContinuo()
    private val vibracion = Vibracion(contexto)
    private var narrador: Narrador? = null
    private val accesibilidad = contexto.getSystemService(AccessibilityManager::class.java)

    private var explorando = false
    private var columnaPrevia = -1
    private var columnaActual = -1

    private val densidad = resources.displayMetrics.density
    private val pincelLinea = Paint().apply { color = Color.parseColor("#99FFFFFF"); strokeWidth = 2f * densidad }
    private val pincelPunto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD600") }
    private val pincelBordePunto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f * densidad
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = true
        isFocusable = true
        actualizarDescripcion()
    }



    /** mapa de zona visible de geo*/
    fun fijarMapa(xMin: Double, xMax: Double, yMin: Double, yMax: Double, columnas: List<DoubleArray>) {
        mapa = if (columnas.size >= 2 && xMax > xMin && yMax > yMin) {
            Mapa(xMin, xMax, yMin, yMax, columnas.map { it.sortedArray() })
        } else {
            null
        }
        columnaPrevia = -1
        actualizarDescripcion()
        invalidate()
    }


    fun borrarMapa() {
        mapa = null
        if (explorando) terminarExploracion(anunciar = false)
        actualizarDescripcion()
        invalidate()
    }

    private fun actualizarDescripcion() {
        contentDescription = if (mapa == null) {
            "Gráfica. Escribe una función y pulsa Graficar para poder explorarla."
        } else {
            "Gráfica de la función. Desliza un dedo de izquierda a derecha para escucharla. " +
                "Toca dos veces para escuchar el recorrido completo."
        }
    }

//toque

    override fun performClick(): Boolean {
        super.performClick()
        alTocarDosVeces?.invoke()
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(evento: MotionEvent): Boolean {
        // Devolver false deja pasar el toque a GeoGebra (mover y hacer zoom)
        if (!exploracionActiva) return false
        when (evento.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true) // dejar quieto para que el scroll no se mueva
                empezarExploracion()
                explorar(evento.x)
            }
            MotionEvent.ACTION_MOVE -> explorar(evento.x)
            MotionEvent.ACTION_UP -> {
                terminarExploracion(anunciar = true)
                if (mapa == null) voz().decir("Primero grafica una función.")
            }
            MotionEvent.ACTION_CANCEL -> terminarExploracion(anunciar = false)
        }
        return true
    }

    /** Con TalkBack, deslizar un dedo llega como eventos hover (exploracion de tacto) */
    override fun onHoverEvent(evento: MotionEvent): Boolean {
        if (!talkBackActivo()) return super.onHoverEvent(evento)
        super.onHoverEvent(evento) // TalkBack enfoca y lee la capa
        when (evento.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER -> {
                empezarExploracion()
                explorar(evento.x)
            }
            MotionEvent.ACTION_HOVER_MOVE -> explorar(evento.x)
            MotionEvent.ACTION_HOVER_EXIT -> terminarExploracion(anunciar = true)
        }
        return true
    }

    private fun empezarExploracion() {
        if (explorando) return
        explorando = true
        columnaPrevia = -1
        columnaActual = -1
        alEmpezar?.invoke()
        narrador?.callar()
        if (mapa != null) tono.iniciar()
    }

    private fun explorar(xPantalla: Float) {
        val datos = mapa ?: return
        if (!explorando || width <= 0) return
        val avance = (xPantalla / width).toDouble().coerceIn(0.0, 1.0)
        val columna = (avance * (datos.columnas.size - 1)).roundToInt()
        val rango = datos.yMax - datos.yMin

        tono.mover(datos.columnas[columna].map { (it - datos.yMin) / rango }, panoramaParaAvance(avance))
        if (columnaPrevia >= 0 && columna != columnaPrevia) revisarCruces(datos, columnaPrevia, columna)

        columnaPrevia = columna
        columnaActual = columna
        invalidate()
    }

    /** revisa las columnas cuando se mueve muy rapido el dedo */
    private fun revisarCruces(datos: Mapa, desde: Int, hasta: Int) {
        if ((datos.x(desde) < 0.0) != (datos.x(hasta) < 0.0)) {
            tono.marcarEjeY()
            vibracion.ejeY()
        }
        val saltoMaximo = (datos.yMax - datos.yMin) * 0.2 // salto grande = asíntota, no cruce
        val inicio = minOf(desde, hasta)
        val fin = maxOf(desde, hasta)
        for (i in inicio until fin) {
            val antes = datos.columnas[i]
            val despues = datos.columnas[i + 1]
            if (antes.size != despues.size) continue
            val cruza = antes.indices.any { rama ->
                val y0 = antes[rama]
                val y1 = despues[rama]
                abs(y1 - y0) < saltoMaximo && ((y0 < 0.0 && y1 >= 0.0) || (y0 > 0.0 && y1 <= 0.0))
            }
            if (cruza) {
                tono.marcarEjeX()
                vibracion.ejeX()
                return
            }
        }
    }

    private fun terminarExploracion(anunciar: Boolean) {
        if (!explorando) return
        explorando = false
        tono.detener()
        val columna = columnaActual
        columnaActual = -1
        invalidate()
        val datos = mapa
        if (anunciar && datos != null && columna >= 0) {
            voz().decir(textoCoordenadas(datos.x(columna), datos.columnas[columna]))
        }
    }

    private fun textoCoordenadas(x: Double, valoresY: DoubleArray): String {
        val textoX = DescriptorGrafica.numero(x)
        if (valoresY.isEmpty()) return "En x igual a $textoX no hay gráfica en la zona visible"
        // De arriba hacia abajo: "y igual a 4 y menos 4"
        val textosY = valoresY.sortedArrayDescending().map { DescriptorGrafica.numero(it) }
        val listaY = if (textosY.size == 1) textosY[0]
        else textosY.dropLast(1).joinToString(", ") + " y " + textosY.last()
        return "x igual a $textoX, y igual a $listaY"
    }

    // Dibujo: línea y puntos bajo el dedo

    override fun onDraw(lienzo: Canvas) {
        super.onDraw(lienzo)
        val datos = mapa ?: return
        val columna = columnaActual
        if (columna < 0) return
        val xPantalla = width * columna.toFloat() / (datos.columnas.size - 1)
        lienzo.drawLine(xPantalla, 0f, xPantalla, height.toFloat(), pincelLinea)
        for (y in datos.columnas[columna]) {
            val yPantalla = (height - (y - datos.yMin) / (datos.yMax - datos.yMin) * height).toFloat()
            lienzo.drawCircle(xPantalla, yPantalla, 8f * densidad, pincelPunto)
            lienzo.drawCircle(xPantalla, yPantalla, 8f * densidad, pincelBordePunto)
        }
    }

    // ciclo de voz

    private fun voz(): Narrador = narrador ?: Narrador(context).also { narrador = it }

    private fun talkBackActivo(): Boolean = accesibilidad?.isTouchExplorationEnabled == true

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        voz() // inicia el motor de voz desde antes
    }

    override fun onDetachedFromWindow() {
        tono.detener()
        explorando = false
        narrador?.cerrar()
        narrador = null
        super.onDetachedFromWindow()
    }
}
