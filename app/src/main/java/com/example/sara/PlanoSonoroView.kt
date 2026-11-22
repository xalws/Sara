package com.example.sara

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.ceil


class PlanoSonoroView @JvmOverloads constructor(
    contexto: Context,
    atributos: AttributeSet? = null
) : View(contexto, atributos) {

    private var funcion: (Double) -> Double = { it }
    /** Segunda curva que suena y se dibuja junto con la función, por ejemplo la mitad de abajo de una circunferencia. */
    private var complemento: ((Double) -> Double)? = null
    /** Si es verdadero, la unidad mide tanto x como y al mismo tiempo */
    private var escalaIgual = false
    private var descripcion = ""
    private var resumen = ""
    private var xMin = -5.0
    private var xMax = 5.0
    private var yMin = -5.0
    private var yMax = 5.0

    private val sonido = SonidoGrafica()
    private val tono = TonoContinuo()
    private val vibracion = Vibracion(contexto)
    private var narrador: Narrador? = null
    private val accesibilidad = contexto.getSystemService(AccessibilityManager::class.java)
    private val manejador = Handler(Looper.getMainLooper())

    private var botonReproducir: Button? = null
    private var textoReproducir: CharSequence = ""
    private var iconoReproducir: Drawable? = null
    private val iconoDetener: Drawable? = ContextCompat.getDrawable(contexto, R.drawable.ico_detener)

    /** Cada acción nueva incrementa el turno, cuando hay turnos viejos estos son ignorados */
    private var turno = 0
    private var sonando = false

    // Recorrido
    private var progresoActual: Float? = null
    private var progresoPrevio = 0f
    private var eventos: List<EventoEje> = emptyList()
    private var recorridoIniciado = false

    // Exploración con el dedo
    private var explorando = false
    private var xExplorado: Double? = null
    private var xPrevExp = Double.NaN
    /** Valor anterior de cada curva al explorar. */
    private var yPrevExp: List<Double> = emptyList()

    private val densidad = resources.displayMetrics.density
    private val marco = RectF()
    private val trazoCurva = Path()
    private val trazoRecorrido = Path()
    private val trazoComplemento = Path()

    private val pincelFondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C1C1C") }
    private val pincelCuadricula = Paint().apply { color = Color.parseColor("#333333"); strokeWidth = 1f * densidad }
    private val pincelEje = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#BDBDBD"); strokeWidth = 2f * densidad }
    private val pincelTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9E9E9E"); textSize = 12f * densidad }
    private val pincelCurva = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD600"); style = Paint.Style.STROKE
        strokeWidth = 3f * densidad; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val pincelRecorrido = Paint(pincelCurva).apply { color = Color.WHITE; strokeWidth = 5f * densidad }
    private val pincelPunto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD600") }
    private val pincelBordePunto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f * densidad
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = true
        isFocusable = true
    }

    // Configuración

    /**
     * Define la función de ejemplo y la ventana del plano que se muestra.
     * [complemento] es una segunda curva, sirve para curvas que no son funciones
     * (circunferencia, elipse, hipérbola): la mitad de arriba y la de abajo suenan y se dibujan a la vez.
     * [escalaIgual] ajusta el alto del plano para que una unidad mida lo mismo en ambos ejes.
     */
    fun configurar(
        descripcion: String,
        xMin: Double, xMax: Double,
        yMin: Double, yMax: Double,
        complemento: ((Double) -> Double)? = null,
        escalaIgual: Boolean = false,
        f: (Double) -> Double
    ) {
        funcion = f
        this.complemento = complemento
        this.escalaIgual = escalaIgual
        this.descripcion = descripcion
        this.xMin = xMin; this.xMax = xMax
        this.yMin = yMin; this.yMax = yMax
        requestLayout()
        resumen = DescriptorGrafica.describir(f, xMin, xMax, yMin, yMax)
        if (complemento != null) {
            resumen = "Mitad de arriba: $resumen La mitad de abajo es su reflejo respecto al eje X: " +
                "cada punto de arriba tiene uno abajo, a la misma distancia del eje."
        }
        contentDescription = "Plano con la gráfica de $descripcion. " +
            "Desliza un dedo de izquierda a derecha para explorarla. " +
            "Toca dos veces para escuchar el recorrido completo."
        invalidate()
    }

    /** [reproducir] inicia o detiene el recorrido. */
    fun vincularBotones(reproducir: Button) {
        botonReproducir = reproducir
        textoReproducir = reproducir.text
        iconoReproducir = reproducir.compoundDrawablesRelative[2] // drawableEnd del XML
        reproducir.setOnClickListener { if (sonando) detener() else reproducir() }
    }

    // recorrido

    fun reproducir() {
        val mi = empezarTurno()
        despuesDeTalkBack(mi) { presentarYRecorrer(mi) }
    }

    fun detener() {
        cancelarTodo()
        terminar()
    }

    private fun empezarTurno(): Int {
        // se detiene un plano en caso de que se escuche otro para no encimarse
        planoActivo?.get()?.takeIf { it !== this }?.detener()
        planoActivo = WeakReference(this)
        cancelarTodo()
        sonando = true
        // Se cambia el texto antes de esperar, así TalkBack lo anuncia antes del sonido
        botonReproducir?.text = "Detener sonido "
        botonReproducir?.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, iconoDetener, null)
        return turno
    }

    private fun cancelarTodo() {
        turno++
        manejador.removeCallbacksAndMessages(null)
        sonido.detener()
        tono.detener()
        narrador?.callar()
        progresoActual = null
        recorridoIniciado = false
    }

    private fun terminar() {
        sonando = false
        progresoActual = null
        botonReproducir?.text = textoReproducir
        botonReproducir?.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, iconoReproducir, null)
        invalidate()
    }

    /** Si TalkBack está activo, espera a que termine de anunciar el botón antes de sonar. */
    private fun despuesDeTalkBack(mi: Int, accion: () -> Unit) {
        val espera = if (talkBackActivo()) 1500L else 100L
        manejador.postDelayed({ if (mi == turno) accion() }, espera)
    }

    private fun presentarYRecorrer(mi: Int) {
        voz().decir("Gráfica de $descripcion. De izquierda a derecha.") {
            if (mi == turno) iniciarRecorrido(mi)
        }
    }

    private fun iniciarRecorrido(mi: Int) {
        progresoPrevio = 0f
        recorridoIniciado = false
        sonido.reproducir(curvas(), xMin, xMax, yMin, yMax, DURACION_RECORRIDO_MS) { lista ->
            manejador.post {
                if (mi == turno) {
                    eventos = lista
                    recorridoIniciado = true
                    seguirRecorrido(mi)
                }
            }
        }
    }

    /** Sigue el audio cada 16 ms, mueve el punto y vibra al cruzar los ejes. */
    private fun seguirRecorrido(mi: Int) {
        if (mi != turno) return
        val avance = sonido.progreso()
        if (avance == null) {
            progresoActual = null
            invalidate()
            voz().decir("Resumen. $resumen") { if (mi == turno) terminar() }
            return
        }
        for (eventoEje in eventos) {
            if (eventoEje.progreso > progresoPrevio && eventoEje.progreso <= avance) {
                if (eventoEje.tipo == TipoEje.EJE_X) vibracion.ejeX() else vibracion.ejeY()
            }
        }
        progresoPrevio = avance
        progresoActual = avance
        invalidate()
        manejador.postDelayed({ seguirRecorrido(mi) }, 16)
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (sonando) detener() else reproducir()
        return true
    }

    // exploracion con dedo

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(evento: MotionEvent): Boolean {
        when (evento.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true) // que el ScrollView no se mueva
                empezarExploracion()
                explorar(evento.x)
            }
            MotionEvent.ACTION_MOVE -> explorar(evento.x)
            MotionEvent.ACTION_UP -> terminarExploracion(anunciar = true)
            MotionEvent.ACTION_CANCEL -> terminarExploracion(anunciar = false)
        }
        return true
    }

    /** Con TalkBack cuando deslizas un dedo este llega como eventos hover (exploración táctil) */
    override fun onHoverEvent(evento: MotionEvent): Boolean {
        if (!talkBackActivo()) return super.onHoverEvent(evento)
        super.onHoverEvent(evento) // TalkBack enfoca y lee el plano
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
        cancelarTodo()
        terminar()
        explorando = true
        xPrevExp = Double.NaN
        yPrevExp = emptyList()
        tono.iniciar()
    }

    private fun curvas(): List<(Double) -> Double> = listOfNotNull(funcion, complemento)

    private fun explorar(xPantalla: Float) {
        if (!explorando || marco.width() <= 0f) return
        val avance = ((xPantalla - marco.left) / marco.width()).toDouble().coerceIn(0.0, 1.0)
        val x = xMin + (xMax - xMin) * avance
        // Un valor por curva: en un círculo suenan a la vez el punto de arriba y el de abajo
        val valores = curvas().map { it(x) }
        val visibles = valores.filter { it.isFinite() && it in yMin..yMax }

        tono.mover(visibles.map { (it - yMin) / (yMax - yMin) }, panoramaParaAvance(avance))

        if (xPrevExp.isFinite() && (xPrevExp < 0.0) != (x < 0.0)) {
            tono.marcarEjeY()
            vibracion.ejeY()
        }
        val cruzaEjeX = valores.indices.any { i ->
            val y = valores[i]
            val yPrev = yPrevExp.getOrElse(i) { Double.NaN }
            y.isFinite() && y in yMin..yMax && yPrev.isFinite() &&
                ((yPrev < 0.0 && y >= 0.0) || (yPrev > 0.0 && y <= 0.0))
        }
        if (cruzaEjeX) {
            tono.marcarEjeX()
            vibracion.ejeX()
        }
        xPrevExp = x
        yPrevExp = valores.map { if (it.isFinite() && it in yMin..yMax) it else Double.NaN }
        xExplorado = x
        invalidate()
    }

    private fun terminarExploracion(anunciar: Boolean) {
        if (!explorando) return
        explorando = false
        tono.detener()
        val x = xExplorado
        xExplorado = null
        invalidate()
        if (anunciar && x != null) {
            val textoX = DescriptorGrafica.numero(x)
            val existentes = curvas().map { it(x) }.filter { it.isFinite() }
                .map { DescriptorGrafica.numero(it) }.distinct()
            voz().decir(
                when (existentes.size) {
                    0 -> "En x igual a $textoX la gráfica no existe"
                    1 -> "x igual a $textoX, y igual a ${existentes[0]}"
                    else -> "x igual a $textoX. Arriba, y igual a ${existentes[0]}. " +
                        "Abajo, y igual a ${existentes[1]}"
                }
            )
        }
    }

    // ciclo

    private fun voz(): Narrador = narrador ?: Narrador(context).also { narrador = it }

    private fun talkBackActivo(): Boolean = accesibilidad?.isTouchExplorationEnabled == true

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        voz() //inicia el motor para que se escuche antes y sea mas rapido
    }

    override fun onDetachedFromWindow() {
        cancelarTodo()
        narrador?.cerrar()
        narrador = null
        super.onDetachedFromWindow()
    }

    // dibujo

    private fun aPantallaX(x: Double) = (marco.left + (x - xMin) / (xMax - xMin) * marco.width()).toFloat()
    private fun aPantallaY(y: Double) = (marco.bottom - (y - yMin) / (yMax - yMin) * marco.height()).toFloat()

    override fun onMeasure(anchoMedida: Int, altoMedida: Int) {
        super.onMeasure(anchoMedida, altoMedida)
        if (!escalaIgual) return
        val margen = 20f * densidad
        val alto = ((measuredWidth - margen) * (yMax - yMin) / (xMax - xMin) + margen).toInt()
        setMeasuredDimension(measuredWidth, alto)
    }

    override fun onDraw(lienzo: Canvas) {
        super.onDraw(lienzo)
        marco.set(0f, 0f, width.toFloat(), height.toFloat())
        lienzo.drawRoundRect(marco, 16f * densidad, 16f * densidad, pincelFondo)
        marco.inset(10f * densidad, 10f * densidad)

        dibujarCuadricula(lienzo)

        lienzo.save()
        lienzo.clipRect(marco)
        construirCurva(trazoCurva, xMax)
        lienzo.drawPath(trazoCurva, pincelCurva)
        complemento?.let {
            construirCurva(trazoComplemento, xMax, it)
            lienzo.drawPath(trazoComplemento, pincelCurva)
        }

        val progreso = progresoActual
        val xPunto = xExplorado ?: progreso?.let { xMin + (xMax - xMin) * it }
        if (progreso != null && xPunto != null) {
            for (curva in curvas()) {
                construirCurva(trazoRecorrido, xPunto, curva)
                lienzo.drawPath(trazoRecorrido, pincelRecorrido)
            }
        }
        if (xPunto != null) {
            // Un punto por cada curva que está sonando
            val xPantalla = aPantallaX(xPunto)
            lienzo.drawLine(xPantalla, marco.top, xPantalla, marco.bottom, pincelCuadricula)
            for (curva in curvas()) {
                val y = curva(xPunto)
                if (y.isFinite() && y in yMin..yMax) {
                    val yPantalla = aPantallaY(y)
                    lienzo.drawCircle(xPantalla, yPantalla, 8f * densidad, pincelPunto)
                    lienzo.drawCircle(xPantalla, yPantalla, 8f * densidad, pincelBordePunto)
                }
            }
        }
        lienzo.restore()
    }

    private fun dibujarCuadricula(lienzo: Canvas) {
        val paso = pasoCuadricula(maxOf(xMax - xMin, yMax - yMin))
        var x = ceil(xMin / paso) * paso
        while (x <= xMax) {
            val xPantalla = aPantallaX(x)
            lienzo.drawLine(xPantalla, marco.top, xPantalla, marco.bottom, pincelCuadricula)
            x += paso
        }
        var y = ceil(yMin / paso) * paso
        while (y <= yMax) {
            val yPantalla = aPantallaY(y)
            lienzo.drawLine(marco.left, yPantalla, marco.right, yPantalla, pincelCuadricula)
            y += paso
        }
        if (0.0 in yMin..yMax) {
            val yPantalla = aPantallaY(0.0)
            lienzo.drawLine(marco.left, yPantalla, marco.right, yPantalla, pincelEje)
            lienzo.drawText("x", marco.right - 10f * densidad, yPantalla - 6f * densidad, pincelTexto)
        }
        if (0.0 in xMin..xMax) {
            val xPantalla = aPantallaX(0.0)
            lienzo.drawLine(xPantalla, marco.top, xPantalla, marco.bottom, pincelEje)
            lienzo.drawText("y", xPantalla + 6f * densidad, marco.top + 12f * densidad, pincelTexto)
        }
    }

    private fun pasoCuadricula(rango: Double): Double = when {
        rango <= 12 -> 1.0
        rango <= 25 -> 2.0
        else -> 5.0
    }

    /** Traza la curva desde xMin hasta xHasta, cortándola donde no existe o hay saltos. */
    private fun construirCurva(trazo: Path, xHasta: Double, f: (Double) -> Double = funcion) {
        trazo.reset()
        val muestras = 400
        val saltoMaximo = (yMax - yMin) * 0.5
        var dibujando = false
        var yPrev = Double.NaN
        for (i in 0..muestras) {
            val x = xMin + (xMax - xMin) * i / muestras
            if (x > xHasta) break
            val y = f(x)
            val valido = y.isFinite() && y >= yMin - (yMax - yMin) && y <= yMax + (yMax - yMin) &&
                (yPrev.isNaN() || abs(y - yPrev) < saltoMaximo)
            if (valido) {
                val xPantalla = aPantallaX(x)
                val yPantalla = aPantallaY(y.coerceIn(yMin - 1, yMax + 1))
                if (dibujando) trazo.lineTo(xPantalla, yPantalla) else trazo.moveTo(xPantalla, yPantalla)
                dibujando = true
            } else {
                dibujando = false
            }
            yPrev = if (y.isFinite()) y else Double.NaN
        }
    }

    private companion object {
        const val DURACION_RECORRIDO_MS = 6000

        var planoActivo: WeakReference<PlanoSonoroView>? = null
    }
}
