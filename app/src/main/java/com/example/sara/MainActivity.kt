package com.example.sara

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

data class DatosLeccion(
    val tituloResId: Int,
    val definicionResId: Int,
    val formulaResId: Int,
    val propiedadesResId: Int,
    val ejemploResId: Int,
)

class MainActivity : AppCompatActivity() {
    private lateinit var contenedorPrincipal: FrameLayout

    private lateinit var btnHome: ImageButton
    private lateinit var btnAprendizaje: ImageButton
    private lateinit var btnGraficar: ImageButton

    private var grafica: GraficaController? = null
    private var pantallaActual: Int = R.layout.vista_home

    private val lecciones = mapOf(
        R.id.btnLeccionCuadratica to DatosLeccion(
            tituloResId = R.string.leccion_cuadratica_titulo,
            definicionResId = R.string.leccion_cuadratica_definicion,
            formulaResId = R.string.leccion_cuadratica_formula,
            propiedadesResId = R.string.leccion_cuadratica_propiedades,
            ejemploResId = R.string.leccion_cuadratica_ejemplo,
        ),
        R.id.btnLeccionLogaritmica to DatosLeccion(
            tituloResId = R.string.leccion_logaritmica_titulo,
            definicionResId = R.string.leccion_logaritmica_definicion,
            formulaResId = R.string.leccion_logaritmica_formula,
            propiedadesResId = R.string.leccion_logaritmica_propiedades,
            ejemploResId = R.string.leccion_logaritmica_ejemplo,
        ),
        R.id.btnLeccionTrigonometrica to DatosLeccion(
            tituloResId = R.string.leccion_trigonometrica_titulo,
            definicionResId = R.string.leccion_trigonometrica_definicion,
            formulaResId = R.string.leccion_trigonometrica_formula,
            propiedadesResId = R.string.leccion_trigonometrica_propiedades,
            ejemploResId = R.string.leccion_trigonometrica_ejemplo,
        ),
        R.id.btnLeccionCoseno to DatosLeccion(
            tituloResId = R.string.leccion_coseno_titulo,
            definicionResId = R.string.leccion_coseno_definicion,
            formulaResId = R.string.leccion_coseno_formula,
            propiedadesResId = R.string.leccion_coseno_propiedades,
            ejemploResId = R.string.leccion_coseno_ejemplo,
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazamos el contenedor principal y los botones
        contenedorPrincipal = findViewById(R.id.contenedorPrincipal)

        btnHome = findViewById(R.id.btnHome)
        btnAprendizaje = findViewById(R.id.btnAprendizaje)
        btnGraficar = findViewById(R.id.btnGraficar)

        // Manejo del botón atrás del sistema para volver a la lista de lecciones si estamos en el detalle
        onBackPressedDispatcher.addCallback(
            owner = this,
            onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    if (pantallaActual == R.layout.vista_leccion) {
                        cambiarPantalla(R.layout.vista_aprendizaje)
                        btnActivo(btnAprendizaje)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )

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
            cambiarPantalla(R.layout.vista_grafica)
            btnActivo(btnGraficar)
        }
    }

    private fun cambiarPantalla(layoutResId: Int) {
        pantallaActual = layoutResId

        // Liberar el graficador anterior (WebView, voz y audio)
        grafica?.destruir()
        grafica = null

        // Limpiar
        contenedorPrincipal.removeAllViews()

        // Nueva pantalla
        val nuevaPantalla: View =
            LayoutInflater.from(this).inflate(layoutResId, contenedorPrincipal, false)
        contenedorPrincipal.addView(nuevaPantalla)

        if (layoutResId == R.layout.vista_grafica) {
            grafica = GraficaController(this, nuevaPantalla)
        } else if (layoutResId == R.layout.vista_aprendizaje) {
            configurarBotonesLeccion(nuevaPantalla)
        }
    }

    private fun configurarBotonesLeccion(vista: View) {
        if (vista is ViewGroup) {
            for (i in 0 until vista.childCount) {
                configurarBotonesLeccion(vista.getChildAt(i))
            }
        }

        val id = vista.id
        if (id != View.NO_ID) {
            val entryName = runCatching { resources.getResourceEntryName(id) }.getOrNull()
            if ((entryName?.startsWith("btnLeccion") == true) && lecciones.containsKey(id)) {
                vista.setOnClickListener {
                    mostrarLeccion(id)
                }
            }
        }
    }

    private fun mostrarLeccion(id: Int) {
        val (tituloResId, definicionResId, formulaResId, propiedadesResId, ejemploResId) =
            lecciones[id] ?: return

        pantallaActual = R.layout.vista_leccion
        grafica?.destruir()
        grafica = null
        contenedorPrincipal.removeAllViews()

        val vistaLeccion =
            LayoutInflater.from(this).inflate(R.layout.vista_leccion, contenedorPrincipal, false)
        contenedorPrincipal.addView(vistaLeccion)

        // Asignar el contenido de la lección seleccionada
        vistaLeccion.findViewById<TextView>(R.id.txtTituloLeccion)?.setText(tituloResId)
        vistaLeccion.findViewById<TextView>(R.id.txtDefinicionLeccion)?.setText(definicionResId)
        vistaLeccion.findViewById<TextView>(R.id.txtFormulaLeccion)?.setText(formulaResId)
        vistaLeccion.findViewById<TextView>(R.id.txtPropiedadesLeccion)?.setText(propiedadesResId)
        vistaLeccion.findViewById<TextView>(R.id.txtEjemploLeccion)?.setText(ejemploResId)

        // Botón volver a la lista de lecciones
        vistaLeccion.findViewById<View>(R.id.btnVolverLeccion)?.setOnClickListener {
            cambiarPantalla(R.layout.vista_aprendizaje)
            btnActivo(btnAprendizaje)
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
        grafica?.destruir()
        grafica = null
        super.onDestroy()
    }
}
