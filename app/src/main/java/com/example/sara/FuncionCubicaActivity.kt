package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sara.databinding.VistaFuncionCubicaBinding


class FuncionCubicaActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionCubicaBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionCubicaBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la gráfica del ejemplo en el plano cartesiano
        enlace.planoSonoro.configurar("f de x igual a x al cubo menos 3x", -2.5, 2.5, -9.0, 9.0) { x -> x * x * x - 3 * x }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoCubica)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
