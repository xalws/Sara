package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sara.databinding.VistaFuncionCuadraticaBinding


class FuncionCuadraticaActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionCuadraticaBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionCuadraticaBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la gráfica del ejemplo en el plano cartesiano
        enlace.planoSonoro.configurar("f de x igual a x al cuadrado menos 4x más 3", -1.0, 5.0, -2.0, 8.0) { x -> x * x - 4 * x + 3 }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoCuadratica)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
