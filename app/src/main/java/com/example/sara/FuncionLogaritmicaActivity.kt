package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.ln
import com.example.sara.databinding.VistaFuncionLogaritmicaBinding


class FuncionLogaritmicaActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionLogaritmicaBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionLogaritmicaBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la gráfica del ejemplo en el plano cartesiano
        enlace.planoSonoro.configurar("f de x igual a logaritmo base 2 de x", -1.0, 8.0, -4.0, 4.0) { x -> ln(x) / ln(2.0) }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoLogaritmica)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
