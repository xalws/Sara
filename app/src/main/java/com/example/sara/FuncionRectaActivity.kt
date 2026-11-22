package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sara.databinding.VistaFuncionRectaBinding


class FuncionRectaActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionRectaBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionRectaBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la recta horizontal y = 1: el tono se mantiene igual de principio a fin
        enlace.planoSonoro.configurar("y igual a 1, una recta horizontal", -5.0, 5.0, -5.0, 5.0) { 1.0 }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoRecta)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
