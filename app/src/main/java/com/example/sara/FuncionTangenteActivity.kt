package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.tan
import com.example.sara.databinding.VistaFuncionTangenteBinding


class FuncionTangenteActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionTangenteBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionTangenteBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre tres tramos de la tangente, de menos 3pi/2 a 3pi/2
        enlace.planoSonoro.configurar("f de x igual a tangente de x", -1.5 * PI, 1.5 * PI, -4.0, 4.0) { x ->
            tangenteSinAsintotas(x)
        }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoTangente)
    }

    private fun tangenteSinAsintotas(x: Double): Double =
        if (abs(cos(x)) < 0.015) Double.NaN else tan(x)

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
