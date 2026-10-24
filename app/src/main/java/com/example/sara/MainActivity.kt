package com.example.sara

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var contenedorPrincipal: FrameLayout

    private lateinit var btnHome: ImageButton
    private lateinit var btnAprendizaje: ImageButton
    private lateinit var btnGraficar: ImageButton

    private lateinit var accesibilidad: AccessibilityEngine
    private var webGeogebra: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accesibilidad = AccessibilityEngine(this)

        // Enlazamos el contenedor principal y los botones
        contenedorPrincipal = findViewById(R.id.contenedorPrincipal)

        btnHome = findViewById(R.id.btnHome)
        btnAprendizaje = findViewById(R.id.btnAprendizaje)
        btnGraficar = findViewById(R.id.btnGraficar)

        // Home por defecto
        cambiarPantalla(R.layout.vista_home)
        btnActivo(btnHome)

        // Funcionalidad para botones de menu
        btnHome.setOnClickListener {
            cambiarPantalla(R.layout.vista_home)
            btnActivo(btnHome)
        }

        btnAprendizaje.setOnClickListener {
            cambiarPantalla(R.layout.vista_aprendizaje)
            btnActivo(btnAprendizaje)
        }

        btnGraficar.setOnClickListener {
            val vista = cambiarPantalla(R.layout.vista_grafica)
            configurarGrafica(vista)
            btnActivo(btnGraficar)
        }
    }

    private fun cambiarPantalla(layoutResId: Int): View {
        // Liberar el WebView anterior y detener sonido
        accesibilidad.stopSweep()
        webGeogebra?.destroy()
        webGeogebra = null

        contenedorPrincipal.removeAllViews()

        val nuevaPantalla: View =
            LayoutInflater.from(this).inflate(layoutResId, contenedorPrincipal, false)
        contenedorPrincipal.addView(nuevaPantalla)
        return nuevaPantalla
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun configurarGrafica(vista: View) {
        val web = vista.findViewById<WebView>(R.id.contenedorGeogebra)
        val etFuncion = vista.findViewById<EditText>(R.id.etFuncion)
        val btnGraficarFuncion = vista.findViewById<View>(R.id.btnGraficarFuncion)
        val btnReproducir = vista.findViewById<View>(R.id.btnReproducirGrafica)

        webGeogebra = web
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        // Evita que el ScrollView robe los gestos (zoom / mover la gráfica)
        web.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        web.loadUrl("file:///android_asset/geogebra.html")

        btnGraficarFuncion.setOnClickListener {
            val expresion = etFuncion.text.toString().trim()
            if (expresion.isEmpty()) {
                accesibilidad.speak("Escribe una función primero")
                return@setOnClickListener
            }

            web.evaluateJavascript("graficar(${JSONObject.quote(expresion)})") { res ->
                val estado = JSONArray("[$res]").optString(0)
                accesibilidad.speak(
                    when (estado) {
                        "ok1" -> "Función graficada"
                        "ok2" -> "Función de dos variables graficada en tres dimensiones"
                        "impl" -> "Ecuación graficada"
                        "cargando" -> "GeoGebra aún está cargando, intenta de nuevo"
                        else -> "No pude graficar esa función, revisa la escritura"
                    }
                )
            }
        }

        btnReproducir.setOnClickListener {
            web.evaluateJavascript("muestrear(120, -10, 10)") { res ->
                val arreglo = try {
                    JSONArray(res)
                } catch (e: Exception) {
                    JSONArray()
                }

                if (arreglo.length() == 0) {
                    accesibilidad.speak("Primero grafica una función")
                    return@evaluateJavascript
                }

                val valores = List(arreglo.length()) { i ->
                    if (arreglo.isNull(i)) null else arreglo.getDouble(i)
                }
                accesibilidad.speak("Reproduciendo gráfica de izquierda a derecha")
                accesibilidad.playSweep(valores, startDelayMs = 2000)
            }
        }
    }

    private fun btnActivo(btn: ImageButton) {
        // Todos apagados
        btnHome.setBackgroundResource(R.drawable.btn_morado)
        btnAprendizaje.setBackgroundResource(R.drawable.btn_morado)
        btnGraficar.setBackgroundResource(R.drawable.btn_morado)

        btn.setBackgroundResource(R.drawable.btn_amarillo)
    }

    override fun onDestroy() {
        webGeogebra?.destroy()
        accesibilidad.shutdown()
        super.onDestroy()
    }
}
