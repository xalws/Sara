package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sara.databinding.VistaFuncionesBinding


class FuncionesActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionesBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionesBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la gráfica del ejemplo en el plano cartesiano
        enlace.planoSonoro.configurar("f de x igual a 2x más 1", -4.5, 4.5, -10.0, 10.0) { x -> 2 * x + 1 }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoFunciones)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
