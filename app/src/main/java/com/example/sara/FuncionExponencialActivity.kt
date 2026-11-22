package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow
import com.example.sara.databinding.VistaFuncionExponencialBinding

class FuncionExponencialActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionExponencialBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionExponencialBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la gráfica del ejemplo en el plano cartesiano
        enlace.planoSonoro.configurar("f de x igual a 2 elevado a la x", -4.0, 4.0, -2.0, 17.0) { x -> 2.0.pow(x) }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoExponencial)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
