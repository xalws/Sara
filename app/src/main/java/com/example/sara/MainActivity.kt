package com.example.sara

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnHome: ImageButton
    private lateinit var btnAprendizaje: ImageButton
    private lateinit var btnGraficar: ImageButton

    private lateinit var contenedorPrincipal: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Botones del menú
        btnHome = findViewById(R.id.btnHome)
        btnAprendizaje = findViewById(R.id.btnAprendizaje)
        btnGraficar = findViewById(R.id.btnGraficar)

        // Contenedor
        contenedorPrincipal = findViewById(R.id.contenedorPrincipal)

        // Mostrar Home al iniciar
        mostrarVista(R.layout.vista_home)

        // Botón Home
        btnHome.setOnClickListener {
            mostrarVista(R.layout.vista_home)
        }

        // Botón Aprendizaje
        btnAprendizaje.setOnClickListener {
            mostrarVista(R.layout.vista_aprendizaje)
        }

        // Botón Graficar
        btnGraficar.setOnClickListener {
            // Pendiente hasta crear vista_graficacion.xml
        }
    }

    private fun mostrarVista(layout: Int) {
        contenedorPrincipal.removeAllViews()
        layoutInflater.inflate(layout, contenedorPrincipal)
    }
}