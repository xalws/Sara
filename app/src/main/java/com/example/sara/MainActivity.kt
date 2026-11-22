package com.example.sara

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var contenedorPrincipal: FrameLayout

    private lateinit var btnInicio: Button
    private lateinit var btnAprendizaje: Button
    private lateinit var btnGraficar: Button

    private var grafica: ControladorGrafica? = null
    private var inicio: ControladorInicio? = null

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        setContentView(R.layout.vista_principal)

        // Evita que el menú y el contenido queden detrás de las barras del sistema
        findViewById<View>(R.id.raizPrincipal).respetarBarrasDelSistema()

        // Enlazamos el contenedor principal con los botones
        contenedorPrincipal = findViewById(R.id.contenedorPrincipal)

        btnInicio = findViewById(R.id.btnInicio)
        btnAprendizaje = findViewById(R.id.btnAprendizaje)
        btnGraficar = findViewById(R.id.btnGraficar)

        // Inicio por defecto o Aprendizaje si se llegó con el botón de regresar
        if (pideAprendizaje(intent)) {
            mostrarAprendizaje()
        } else {
            mostrarInicio()
        }

        btnInicio.setOnClickListener {
            mostrarInicio()
        }

        btnAprendizaje.setOnClickListener {
            mostrarAprendizaje()
        }

        btnGraficar.setOnClickListener {
            mostrarGraficadora()
        }
    }

    override fun onNewIntent(intencion: Intent) {
        super.onNewIntent(intencion)
        setIntent(intencion)
        if (pideAprendizaje(intencion)) {
            mostrarAprendizaje()
        }
    }

    private fun pideAprendizaje(intencion: Intent?): Boolean =
        intencion?.getStringExtra(EXTRA_SECCION) == SECCION_APRENDIZAJE

    private fun mostrarInicio() {
        cambiarPantalla(R.layout.vista_inicio)
        marcarBotonActivo(btnInicio)
    }

    private fun mostrarGraficadora() {
        cambiarPantalla(R.layout.vista_grafica)
        marcarBotonActivo(btnGraficar)
    }

    private fun mostrarAprendizaje() {
        cambiarPantalla(R.layout.vista_aprendizaje)
        marcarBotonActivo(btnAprendizaje)
    }

    private fun cambiarPantalla(idDiseno: Int) {
        // Liberar el graficador anterior (WebView, voz y audio)
        grafica?.destruir()
        grafica = null
        inicio?.destruir()
        inicio = null

        // Limpiar
        contenedorPrincipal.removeAllViews()

        // Nueva pantalla
        val nuevaPantalla: View =
            LayoutInflater.from(this).inflate(idDiseno, contenedorPrincipal, false)
        contenedorPrincipal.addView(nuevaPantalla)

        if (idDiseno == R.layout.vista_inicio) {
            inicio = ControladorInicio(this, nuevaPantalla) { mostrarGraficadora() }
        }

        if (idDiseno == R.layout.vista_grafica) {
            grafica = ControladorGrafica(this, nuevaPantalla)
        }

        if (idDiseno == R.layout.vista_aprendizaje) {
            configurarBotonesAprendizaje(nuevaPantalla)
        }
    }
    // abrir pantalla
    private fun configurarBotonesAprendizaje(pantalla: View) {
        val btnTemaPlano = pantalla.findViewById<Button>(R.id.btnTemaPlano)
        val btnTemaConceptos = pantalla.findViewById<Button>(R.id.btnTemaConceptos)
        val btnTemaFunciones = pantalla.findViewById<Button>(R.id.btnTemaFunciones)
        val btnTemaRecta = pantalla.findViewById<Button>(R.id.btnTemaRecta)
        val btnTemaLineal = pantalla.findViewById<Button>(R.id.btnTemaLineal)
        val btnTemaCuadratica = pantalla.findViewById<Button>(R.id.btnTemaCuadratica)
        val btnTemaCubica = pantalla.findViewById<Button>(R.id.btnTemaCubica)
        val btnTemaValorAbsoluto = pantalla.findViewById<Button>(R.id.btnTemaValorAbsoluto)
        val btnTemaRaiz = pantalla.findViewById<Button>(R.id.btnTemaRaiz)
        val btnTemaExponencial = pantalla.findViewById<Button>(R.id.btnTemaExponencial)
        val btnTemaLogaritmica = pantalla.findViewById<Button>(R.id.btnTemaLogaritmica)
        val btnTemaConicas = pantalla.findViewById<Button>(R.id.btnTemaConicas)
        val btnTemaCirculoUnitario = pantalla.findViewById<Button>(R.id.btnTemaCirculoUnitario)
        val btnTemaSenoCoseno = pantalla.findViewById<Button>(R.id.btnTemaSenoCoseno)
        val btnTemaTangente = pantalla.findViewById<Button>(R.id.btnTemaTangente)

        btnTemaPlano.setOnClickListener {
            startActivity(Intent(this, PlanoCartesianoActivity::class.java))
        }
        btnTemaConceptos.setOnClickListener {
            startActivity(Intent(this, ConceptosActivity::class.java))
        }
        btnTemaFunciones.setOnClickListener {
            startActivity(Intent(this, FuncionesActivity::class.java))
        }
        btnTemaRecta.setOnClickListener {
            startActivity(Intent(this, FuncionRectaActivity::class.java))
        }
        btnTemaLineal.setOnClickListener {
            startActivity(Intent(this, FuncionLinealActivity::class.java))
        }
        btnTemaCuadratica.setOnClickListener {
            startActivity(Intent(this, FuncionCuadraticaActivity::class.java))
        }
        btnTemaCubica.setOnClickListener {
            startActivity(Intent(this, FuncionCubicaActivity::class.java))
        }
        btnTemaValorAbsoluto.setOnClickListener {
            startActivity(Intent(this, FuncionValorAbsolutoActivity::class.java))
        }
        btnTemaRaiz.setOnClickListener {
            startActivity(Intent(this, FuncionRaizActivity::class.java))
        }
        btnTemaExponencial.setOnClickListener {
            startActivity(Intent(this, FuncionExponencialActivity::class.java))
        }
        btnTemaLogaritmica.setOnClickListener {
            startActivity(Intent(this, FuncionLogaritmicaActivity::class.java))
        }
        btnTemaConicas.setOnClickListener {
            startActivity(Intent(this, FuncionConicasActivity::class.java))
        }
        btnTemaCirculoUnitario.setOnClickListener {
            startActivity(Intent(this, CirculoUnitarioActivity::class.java))
        }
        btnTemaSenoCoseno.setOnClickListener {
            startActivity(Intent(this, FuncionSenoCosenoActivity::class.java))
        }
        btnTemaTangente.setOnClickListener {
            startActivity(Intent(this, FuncionTangenteActivity::class.java))
        }
    }

   // se pinta de amarillo al seleccionar un boton
    private fun marcarBotonActivo(boton: Button) {
        for (pestana in listOf(btnInicio, btnGraficar, btnAprendizaje)) {
            pestana.isSelected = pestana === boton
        }
    }

    override fun onStop() {
        super.onStop()
        inicio?.detener()
    }

    override fun onDestroy() {
        inicio?.destruir()
        inicio = null
        grafica?.destruir()
        grafica = null
        super.onDestroy()
    }
}