package com.example.sara

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.EditorInfo
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

//maneja vistas de xml
class ControladorGrafica(private val actividad: Activity, raiz: View) {

    private val vistaWeb: WebView = raiz.findViewById(R.id.contenedorGeogebra)
    private val etFuncion: EditText = raiz.findViewById(R.id.etFuncion)
    private val btnGraficar: AppCompatButton = raiz.findViewById(R.id.btnGraficar)
    private val btnReproducir: AppCompatButton = raiz.findViewById(R.id.btnReproducirGrafica)
    private val capa: CapaSonora = raiz.findViewById(R.id.capaSonora)
    private val interruptorExplorar: SwitchCompat = raiz.findViewById(R.id.swExplorar)
    //vista abajo de los botones
    private lateinit var teclado: TecladoMatematico
    private val txtEstado: TextView
    private val txtTituloParametros: TextView
    private val contenedorParametros: LinearLayout

    private var motor: MotorAccesibilidad? = MotorAccesibilidad(actividad)
    private val manejador = Handler(Looper.getMainLooper())
    private var hablarParametro: Runnable? = null

    private var destruido = false
    private var graficaLista = false
    private var calculando = false
    private var planActual: PlanGrafica? = null
    private val valores = linkedMapOf<String, Double>()

    init {
        // Contenedor vertical de xml
        val contenido = (btnGraficar.parent as View).parent as LinearLayout

        txtEstado = TextView(actividad).apply {
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 18f
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = aPixeles(20) }
        }
        txtTituloParametros = TextView(actividad).apply {
            text = "Parámetros"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = aPixeles(25) }
        }
        contenedorParametros = LinearLayout(actividad).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
       //teclado
        teclado = TecladoMatematico(actividad, etFuncion) { graficar() }
        val pos = contenido.indexOfChild(btnGraficar.parent as View) + 1
        contenido.addView(txtEstado, pos)
        contenido.addView(teclado.vista, pos + 1)
        contenido.addView(txtTituloParametros, pos + 2)
        contenido.addView(contenedorParametros, pos + 3)

        etFuncion.hint = "Ej: x^2   a*x^2+b   x^2+y^2=25"
        etFuncion.imeOptions = EditorInfo.IME_ACTION_DONE
        etFuncion.setSingleLine(true)
        //usa teclado matematico no el del sistema
        etFuncion.setShowSoftInputOnFocus(false)
        etFuncion.setOnClickListener { teclado.mostrar() }
        etFuncion.setOnFocusChangeListener { _, tieneFoco -> if (tieneFoco) teclado.mostrar() }
        // TalkBack lee y explora la capa sonora en lugar de el de geogebra
        vistaWeb.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

        configurarWebView()
        configurarBotones()
        configurarExploracion()
        mostrar("Cargando GeoGebra...")
    }

    // webview

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun configurarWebView() {
        val ajustes = vistaWeb.settings
        ajustes.javaScriptEnabled = true
        ajustes.domStorageEnabled = true

      //modo oscuro
        vistaWeb.setBackgroundColor(Color.BLACK)
        if (Build.VERSION.SDK_INT >= 33) {
            ajustes.isAlgorithmicDarkeningAllowed = false
        } else if (Build.VERSION.SDK_INT >= 29) {
            @Suppress("DEPRECATION")
            ajustes.forceDark = WebSettings.FORCE_DARK_OFF
        }
        vistaWeb.setLayerType(View.LAYER_TYPE_SOFTWARE, null) // en caso de provocar lag, eliminar esta linea

        WebView.setWebContentsDebuggingEnabled(true)

        vistaWeb.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(mensajeConsola: ConsoleMessage): Boolean {
                Log.d("SaraWeb", "${mensajeConsola.message()} (${mensajeConsola.sourceId()}:${mensajeConsola.lineNumber()})")
                if (mensajeConsola.messageLevel() == ConsoleMessage.MessageLevel.ERROR && !graficaLista) {
                    mostrar("Error en la página: ${mensajeConsola.message()}")
                }
                return true
            }
        }
        // GeoGebra viene dentro de la app assets/geogebra
        val cargadorAssets = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(actividad))
            .build()
        vistaWeb.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                vista: WebView, solicitud: WebResourceRequest
            ): WebResourceResponse? {
                if (solicitud.url.host != WebViewAssetLoader.DEFAULT_DOMAIN) {
                    // en caso de que geogebra de error, pasamos a geogebra en red, esto requiere internet
                    Log.w("SaraWeb", "Pedido fuera de la app: ${solicitud.url}")
                }
                return cargadorAssets.shouldInterceptRequest(solicitud.url)
            }

            override fun onReceivedError(
                vista: WebView, solicitud: WebResourceRequest, error: WebResourceError
            ) {
                Log.e("SaraWeb", "Error ${error.description} en ${solicitud.url}")
                if (solicitud.isForMainFrame || solicitud.url.toString().contains("geogebra")) {
                    mostrar("No se pudo cargar GeoGebra: ${error.description}")
                }
            }
        }
        vistaWeb.addJavascriptInterface(Puente(), "Android")


        vistaWeb.setOnTouchListener { vistaTocada, _ ->
            vistaTocada.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        manejador.postDelayed({
            if (!graficaLista) mostrar("GeoGebra no terminó de cargar. Sal de la sección y vuelve a entrar.")
        }, 15000)

        vistaWeb.loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/geogebra.html")
    }

    private inner class Puente {
        @JavascriptInterface
        fun evento(tipo: String, datos: String) {
            actividad.runOnUiThread {
                if (!destruido) manejarEvento(tipo, datos)
            }
        }
    }

    private fun manejarEvento(tipo: String, datos: String) {
        when (tipo) {
            "listo" -> {
                graficaLista = true
                mostrar("GeoGebra listo. Escribe una función y pulsa Graficar.")
                planActual?.let { dibujar(it) }
            }
            "error" -> mostrar(datos)
            "grafica" -> {
                val respuesta = JSONObject(datos)
                if (respuesta.optBoolean("ok")) {
                    graficaLista = true
                    val parametrosPlan = planActual?.parametros.orEmpty()
                    anunciar(
                        if (parametrosPlan.isEmpty()) "Función graficada. Pulsa Reproducir o explórala con el dedo."
                        else "Función graficada. Ajusta ${parametrosPlan.joinToString(", ")} con los controles deslizantes."
                    )
                } else {
                    planActual = null
                    capa.borrarMapa()
                    anunciar(respuesta.optString("mensaje", "No se pudo graficar."))
                }
            }
            "muestras" -> procesarMuestras(JSONObject(datos))
            "mapa" -> procesarMapa(JSONObject(datos))
        }
    }

    private fun dibujar(plan: PlanGrafica) {
        vistaWeb.evaluateJavascript("graficar(${plan.aJson(valores)})", null)
    }

    //botones

    private fun configurarBotones() {
        btnGraficar.setOnClickListener { graficar() }

        etFuncion.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) {
                graficar()
                true
            } else {
                false
            }
        }

        btnReproducir.setOnClickListener {
            val plan = planActual
            when {
                calculando -> Unit
                plan == null || !graficaLista -> anunciar("Primero grafica una función.")
                else -> {
                    calculando = true
                    motor?.detenerAudio()
                    mostrar("Calculando el sonido...")
                    val cantidadMuestras = if (plan.modo == "implicita") 60 else 120
                    vistaWeb.evaluateJavascript("muestrear('${plan.modo}', $cantidadMuestras)", null)
                }
            }
        }
    }

    private fun graficar() {
        teclado.ocultar()
        when (val analisis = AnalizadorFuncion.analizar(etFuncion.text.toString())) {
            is Analisis.Invalido -> anunciar(analisis.mensaje)
            is Analisis.Valido -> {
                val plan = analisis.plan
                planActual = plan
                capa.borrarMapa() // hasta que llegue el mapa de la gráfica nueva
                reconstruirParametros(plan.parametros)
                // Se intenta siempre: la página responde si aún no está lista
                dibujar(plan)
            }
        }
    }

    private fun procesarMuestras(respuesta: JSONObject) {
        calculando = false
        if (!respuesta.optBoolean("ok")) {
            anunciar(respuesta.optString("mensaje", "No se pudo calcular el sonido."))
            return
        }
        val arregloSeries = respuesta.getJSONArray("series")
        val series = List(arregloSeries.length()) { i ->
            val paso = arregloSeries.getJSONArray(i)
            List(paso.length()) { j -> paso.getDouble(j) }
        }
        if (series.all { it.isEmpty() }) {
            anunciar("No hay puntos de la gráfica en la zona visible.")
            return
        }
        mostrar("Reproduciendo de izquierda a derecha.")
        motor?.reproducirSerie(series, respuesta.getDouble("yMin"), respuesta.getDouble("yMax"))
    }

    // exploracion de el dedo

    private fun configurarExploracion() {
        val amarillo = 0xFFFFD600.toInt()
        val estados = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        interruptorExplorar.thumbTintList = ColorStateList(estados, intArrayOf(amarillo, Color.parseColor("#BDBDBD")))
        interruptorExplorar.trackTintList = ColorStateList(estados, intArrayOf(0x80FFD600.toInt(), Color.parseColor("#4F4F4F")))

        interruptorExplorar.setOnCheckedChangeListener { _, encendido ->
            capa.exploracionActiva = encendido
            mostrar(
                if (encendido) "Desliza un dedo sobre la gráfica para escucharla."
                else "Ahora el dedo mueve la gráfica y la acerca o aleja."
            )
        }
        capa.alEmpezar = { motor?.detenerAudio() }
        capa.alTocarDosVeces = { btnReproducir.performClick() }
    }

    /** Mapa de puntos que GeoGebra manda al cambiar la gráfica un parámetro o la vista */
    private fun procesarMapa(respuesta: JSONObject) {
        if (planActual == null) return
        val arregloSeries = respuesta.getJSONArray("series")
        val columnas = List(arregloSeries.length()) { i ->
            val paso = arregloSeries.getJSONArray(i)
            DoubleArray(paso.length()) { j -> paso.getDouble(j) }
        }
        capa.fijarMapa(
            respuesta.getDouble("xMin"), respuesta.getDouble("xMax"),
            respuesta.getDouble("yMin"), respuesta.getDouble("yMax"),
            columnas
        )
    }

    // parametros a,b,c

    private fun reconstruirParametros(nombres: List<String>) {
        contenedorParametros.removeAllViews()
        valores.keys.retainAll(nombres.toSet())
        txtTituloParametros.visibility = if (nombres.isEmpty()) View.GONE else View.VISIBLE
        nombres.forEach { nombre ->
            val valor = valores.getOrPut(nombre) { 1.0 }
            agregarControl(nombre, valor)
        }
    }

    private fun agregarControl(nombre: String, valorInicial: Double) {
        val amarillo = ColorStateList.valueOf(0xFFFFD600.toInt())

        val etiqueta = TextView(actividad).apply {
            text = textoParametro(nombre, valorInicial)
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }
        val barra = SeekBar(actividad).apply {
            max = 200 // -10.0 a 10.0, paso 0.1
            progress = ((valorInicial + 10.0) * 10.0).roundToInt()
            progressTintList = amarillo
            thumbTintList = amarillo
            contentDescription = "Parámetro $nombre"
            ViewCompat.setStateDescription(this, textoParametro(nombre, valorInicial))
        }

        barra.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(barraMovida: SeekBar, progreso: Int, delUsuario: Boolean) {
                val valor = (progreso - 100) / 10.0
                valores[nombre] = valor
                val texto = textoParametro(nombre, valor)
                etiqueta.text = texto
                ViewCompat.setStateDescription(barraMovida, texto)
                if (graficaLista && planActual != null) {
                    vistaWeb.evaluateJavascript("fijarParametro('$nombre', $valor)", null)
                }
                if (delUsuario) decirParametroConRetraso(texto)
            }

            override fun onStartTrackingTouch(barraMovida: SeekBar) = Unit
            override fun onStopTrackingTouch(barraMovida: SeekBar) = Unit
        })

        val fila = LinearLayout(actividad).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, aPixeles(8), 0, aPixeles(8))
            addView(etiqueta)
            addView(barra)
        }
        contenedorParametros.addView(fila)
    }

    private fun textoParametro(nombre: String, valor: Double): String {
        val num = if (valor == valor.roundToInt().toDouble()) valor.roundToInt().toString()
        else String.format(Locale.forLanguageTag("es-MX"), "%.1f", valor)
        return "$nombre = $num"
    }


    private fun decirParametroConRetraso(texto: String) {
        hablarParametro?.let { manejador.removeCallbacks(it) }
        val tarea = Runnable { if (!talkBackActivo()) motor?.hablar(texto) }
        hablarParametro = tarea
        manejador.postDelayed(tarea, 350)
    }

    // mensajes

    //texto con talkback
    private fun mostrar(mensaje: String) {
        if (!destruido) txtEstado.text = mensaje
    }

    /** Texto + voz propia si TalkBack no está activo. */
    private fun anunciar(mensaje: String) {
        calculando = false
        mostrar(mensaje)
        if (!talkBackActivo()) motor?.hablar(mensaje)
    }

    private fun talkBackActivo(): Boolean {
        val gestorAccesibilidad = actividad.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return gestorAccesibilidad.isEnabled && gestorAccesibilidad.isTouchExplorationEnabled
    }

    private fun aPixeles(valor: Int): Int = (valor * actividad.resources.displayMetrics.density).toInt()

    // ciclo

    fun destruir() {
        destruido = true
        manejador.removeCallbacksAndMessages(null)
        motor?.apagar()
        motor = null
        vistaWeb.stopLoading()
        (vistaWeb.parent as? ViewGroup)?.removeView(vistaWeb)
        vistaWeb.destroy()
    }
}
