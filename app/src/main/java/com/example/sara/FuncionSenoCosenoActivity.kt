package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.PI
import kotlin.math.sin
import com.example.sara.databinding.VistaFuncionSenoCosenoBinding


class FuncionSenoCosenoActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionSenoCosenoBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionSenoCosenoBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la gráfica del ejemplo en el plano cartesiano
        enlace.planoSonoro.configurar("f de x igual a 2 por seno de x", -2 * PI, 2 * PI, -3.0, 3.0) { x -> 2 * sin(x) }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoSenoCoseno)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
