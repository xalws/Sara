package com.example.sara

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var contenedorPrincipal: FrameLayout

    private lateinit var btnHome: ImageButton
    private lateinit var btnAprendizaje: ImageButton
    private lateinit var btnGraficar: ImageButton

    private var grafica: GraficaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            cambiarPantalla(R.layout.vista_grafica)
            btnActivo(btnGraficar)
        }
    }

    private fun cambiarPantalla(layoutResId: Int) {
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