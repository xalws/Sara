package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import com.example.sara.databinding.VistaFuncionValorAbsolutoBinding


class FuncionValorAbsolutoActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionValorAbsolutoBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionValorAbsolutoBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la gráfica del ejemplo en el plano cartesiano
        enlace.planoSonoro.configurar("f de x igual a valor absoluto de x menos 2, más 1", -3.0, 7.0, -2.0, 8.0) { x -> abs(x - 2) + 1 }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoValorAbsoluto)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
