package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt
import com.example.sara.databinding.VistaFuncionRaizBinding


class FuncionRaizActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionRaizBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionRaizBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Sonido que recorre la gráfica del ejemplo en el plano cartesiano.
        // Para x menor que 2 la raíz no existe (sqrt devuelve NaN) y se escucha silencio.
        enlace.planoSonoro.configurar("f de x igual a raíz cuadrada de x menos 2, más 1", -1.0, 10.0, -1.0, 5.0) { x ->
            sqrt(x - 2) + 1
        }
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoRaiz)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
