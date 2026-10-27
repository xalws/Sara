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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Maneja la pantalla vista_grafica (tu XML, sin cambios):
 * GeoGebra en el WebView + parámetros (a, b, c...) + sonificación.
 */
class GraficaController(private val activity: Activity, root: View) {

    private val webView: WebView = root.findViewById(R.id.contenedorGeogebra)
    private val etFuncion: EditText = root.findViewById(R.id.etFuncion)
    private val btnGraficar: AppCompatButton = root.findViewById(R.id.btnGraficar)
    private val btnReproducir: AppCompatButton = root.findViewById(R.id.btnReproducirGrafica)

    // Vistas que se agregan por código debajo de los botones
    private lateinit var teclado: TecladoMatematico
    private val txtEstado: TextView
    private val txtTituloParametros: TextView
    private val contenedorParametros: LinearLayout

    private var motor: AccessibilityEngine? = AccessibilityEngine(activity)
    private val handler = Handler(Looper.getMainLooper())
    private var hablarParametro: Runnable? = null

    private var destruido = false
    private var graficaLista = false
    private var calculando = false
    private var planActual: PlanGrafica? = null
    private val valores = linkedMapOf<String, Double>()

    init {
        // Contenedor vertical de tu XML (el padre de la fila de botones)
        val contenido = (btnGraficar.parent as View).parent as LinearLayout

        txtEstado = TextView(activity).apply {
            setTextColor(Color.parseColor("#D0D0D0"))
            textSize = 18f
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
        }
        txtTituloParametros = TextView(activity).apply {
            text = "Parámetros"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(25) }
        }
        contenedorParametros = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        // Teclado matemático propio, justo debajo de los botones
        teclado = TecladoMatematico(activity, etFuncion) { graficar() }
        val pos = contenido.indexOfChild(btnGraficar.parent as View) + 1
        contenido.addView(txtEstado, pos)
        contenido.addView(teclado.vista, pos + 1)
        contenido.addView(txtTituloParametros, pos + 2)
        contenido.addView(contenedorParametros, pos + 3)

        etFuncion.hint = "Ej: x^2   a*x^2+b   x^2+y^2=25"
        etFuncion.imeOptions = EditorInfo.IME_ACTION_DONE
        etFuncion.setSingleLine(true)
        // No abrir el teclado del sistema: se usa el teclado matemático
        etFuncion.setShowSoftInputOnFocus(false)
        etFuncion.setOnClickListener { teclado.mostrar() }
        etFuncion.setOnFocusChangeListener { _, tieneFoco -> if (tieneFoco) teclado.mostrar() }
        webView.contentDescription = "Gráfica de la función. Usa el botón Reproducir para escucharla."

        configurarWebView()
        configurarBotones()
        mostrar("Cargando GeoGebra (se necesita internet)...")
    }

    // ---------- WebView ----------

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun configurarWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true

        // Evita el cuadro negro: modo oscuro automático del WebView y render por GPU
        webView.setBackgroundColor(Color.WHITE)
        if (Build.VERSION.SDK_INT >= 33) {
            s.isAlgorithmicDarkeningAllowed = false
        } else if (Build.VERSION.SDK_INT >= 29) {
            @Suppress("DEPRECATION")
            s.forceDark = WebSettings.FORCE_DARK_OFF
        }
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null) // si va lento, quita esta línea

        WebView.setWebContentsDebuggingEnabled(true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                Log.d("SaraWeb", "${m.message()} (${m.sourceId()}:${m.lineNumber()})")
                if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR && !graficaLista) {
                    mostrar("Error en la página: ${m.message()}")
                }
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                Log.e("SaraWeb", "Error ${error.description} en ${request.url}")
                if (request.isForMainFrame || request.url.toString().contains("geogebra")) {
                    mostrar("No se pudo cargar GeoGebra: ${error.description}")
                }
            }
        }
        webView.addJavascriptInterface(Puente(), "Android")

        // Que el ScrollView no se robe el arrastre/zoom de la gráfica
        webView.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        handler.postDelayed({
            if (!graficaLista) mostrar("GeoGebra no terminó de cargar. Revisa tu internet.")
        }, 15000)

        val html = activity.assets.open("geogebra.html").bufferedReader().use { it.readText() }
        webView.loadDataWithBaseURL("https://www.geogebra.org", html, "text/html", "UTF-8", null)
    }

    /** Llamado desde JavaScript (otro hilo): se pasa todo al hilo principal. */
    private inner class Puente {
        @JavascriptInterface
        fun evento(tipo: String, datos: String) {
            activity.runOnUiThread {
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
                val r = JSONObject(datos)
                if (r.optBoolean("ok")) {
                    graficaLista = true
                    val params = planActual?.parametros.orEmpty()
                    anunciar(
                        if (params.isEmpty()) "Función graficada. Pulsa Reproducir para escucharla."
                        else "Función graficada. Ajusta ${params.joinToString(", ")} con los controles deslizantes."
                    )
                } else {
                        planActual = null
                    anunciar(r.optString("mensaje", "No se pudo graficar."))
                }
            }
            "muestras" -> procesarMuestras(JSONObject(datos))
        }
    }

    private fun dibujar(plan: PlanGrafica) {
        webView.evaluateJavascript("graficar(${plan.aJson(valores)})", null)
    }

    // ---------- Botones ----------

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
                    val n = if (plan.modo == "implicita") 60 else 120
                    webView.evaluateJavascript("muestrear('${plan.modo}', $n)", null)
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
                reconstruirParametros(plan.parametros)
                // Se intenta siempre: la página responde si aún no está lista
                dibujar(plan)
            }
        }
    }

    private fun procesarMuestras(r: JSONObject) {
        calculando = false
        if (!r.optBoolean("ok")) {
            anunciar(r.optString("mensaje", "No se pudo calcular el sonido."))
            return
        }
        val arr = r.getJSONArray("series")
        val series = List(arr.length()) { i ->
            val paso = arr.getJSONArray(i)
            List(paso.length()) { j -> paso.getDouble(j) }
        }
        if (series.all { it.isEmpty() }) {
            anunciar("No hay puntos de la gráfica en la zona visible.")
            return
        }
        mostrar("Reproduciendo de izquierda a derecha.")
        motor?.reproducirSerie(series, r.getDouble("yMin"), r.getDouble("yMax"))
    }

    // ---------- Parámetros (a, b, c...) ----------

    private fun reconstruirParametros(nombres: List<String>) {
        contenedorParametros.removeAllViews()
        valores.keys.retainAll(nombres.toSet())
        txtTituloParametros.visibility = if (nombres.isEmpty()) View.GONE else View.VISIBLE
        nombres.forEach { n ->
            val valor = valores.getOrPut(n) { 1.0 }
            agregarControl(n, valor)
        }
    }

    private fun agregarControl(nombre: String, valorInicial: Double) {
        val amarillo = ColorStateList.valueOf(0xFFFFB905.toInt())

        val etiqueta = TextView(activity).apply {
            text = textoParametro(nombre, valorInicial)
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }
        val barra = SeekBar(activity).apply {
            max = 200 // -10.0 a 10.0, paso 0.1
            progress = ((valorInicial + 10.0) * 10.0).roundToInt()
            progressTintList = amarillo
            thumbTintList = amarillo
            contentDescription = "Parámetro $nombre"
            ViewCompat.setStateDescription(this, textoParametro(nombre, valorInicial))
        }

        barra.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progreso: Int, delUsuario: Boolean) {
                val v = (progreso - 100) / 10.0
                valores[nombre] = v
                val texto = textoParametro(nombre, v)
                etiqueta.text = texto
                ViewCompat.setStateDescription(sb, texto)
                if (graficaLista && planActual != null) {
                    webView.evaluateJavascript("setParam('$nombre', $v)", null)
                }
                if (delUsuario) decirParametroConRetraso(texto)
            }

            override fun onStartTrackingTouch(sb: SeekBar) = Unit
            override fun onStopTrackingTouch(sb: SeekBar) = Unit
        })

        val fila = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            addView(etiqueta)
            addView(barra)
        }
        contenedorParametros.addView(fila)
    }

    private fun textoParametro(nombre: String, v: Double): String {
        val num = if (v == v.roundToInt().toDouble()) v.roundToInt().toString()
        else String.format(Locale.forLanguageTag("es-MX"), "%.1f", v)
        return "$nombre = $num"
    }

    /** Sin TalkBack: lee el valor cuando el usuario deja de mover el control. */
    private fun decirParametroConRetraso(texto: String) {
        hablarParametro?.let { handler.removeCallbacks(it) }
        val tarea = Runnable { if (!talkBackActivo()) motor?.speak(texto) }
        hablarParametro = tarea
        handler.postDelayed(tarea, 350)
    }

    // ---------- Mensajes ----------

    /** Solo texto (TalkBack lo lee por ser región "live"). */
    private fun mostrar(mensaje: String) {
        if (!destruido) txtEstado.text = mensaje
    }

    /** Texto + voz propia si TalkBack no está activo. */
    private fun anunciar(mensaje: String) {
        calculando = false
        mostrar(mensaje)
        if (!talkBackActivo()) motor?.speak(mensaje)
    }

    private fun talkBackActivo(): Boolean {
        val am = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled && am.isTouchExplorationEnabled
    }

    private fun dp(v: Int): Int = (v * activity.resources.displayMetrics.density).toInt()

    // ---------- Ciclo de vida ----------

    fun destruir() {
        destruido = true
        handler.removeCallbacksAndMessages(null)
        motor?.shutdown()
        motor = null
        webView.stopLoading()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }
}
