package com.example.sara

import android.adservices.adid.AdId
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import com.example.sara.R

class MainActivity : AppCompatActivity() {
    private lateinit var contenedorPrincipal: FrameLayout

    private lateinit var btnHome : ImageButton
    private lateinit var btnAprendizaje : ImageButton
    private lateinit var btnGraficar : ImageButton

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazamos el contenedor principal y los botones
        contenedorPrincipal = findViewById(R.id.contenedorPrincipal)

        btnHome = findViewById<ImageButton>(R.id.btnHome)
        btnAprendizaje = findViewById<ImageButton>(R.id.btnAprendizaje)
        btnGraficar = findViewById<ImageButton>(R.id.btnGraficar)

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

    private fun cambiarPantalla(layoutResId: Int){
        // Limpiar
        contenedorPrincipal.removeAllViews()

        // Nueva pantallas
        val nuevaPantalla: View = LayoutInflater.from(this).inflate(layoutResId, contenedorPrincipal, false)
        contenedorPrincipal.addView(nuevaPantalla)
    }

    private fun btnActivo(btn: ImageButton){
        // Colores
        val amarillo = Color.parseColor("#ffb905")
        val morado = Color.parseColor("#ee03ff")

        // Todos apagados
        btnHome.setBackgroundResource(R.drawable.btn_morado)
        btnAprendizaje.setBackgroundResource(R.drawable.btn_morado)
        btnGraficar.setBackgroundResource(R.drawable.btn_morado)

        btn.setBackgroundResource(R.drawable.btn_amarillo)
    }
}