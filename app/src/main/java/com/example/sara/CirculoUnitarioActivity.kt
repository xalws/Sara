package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.sqrt
import com.example.sara.databinding.VistaCirculoUnitarioBinding


class CirculoUnitarioActivity : AppCompatActivity() {
    private lateinit var enlace: VistaCirculoUnitarioBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaCirculoUnitarioBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // El círculo no es función: suenan y se dibujan a la vez la mitad de arriba y la de abajo.
        // Con la ventana de menos 1.25 a 1.25, x = +-1 cae justo en una muestra del dibujo.
        val mitadArriba = { x: Double ->
            val valor = 1 - x * x
            if (valor < -1e-9) Double.NaN else sqrt(max(0.0, valor))
        }
        enlace.planoSonoro.configurar(
            "x cuadrada más y cuadrada igual a 1, el círculo unitario. Suenan a la vez la mitad de arriba, más aguda, y la de abajo, más grave",
            -1.25, 1.25, -1.25, 1.25,
            complemento = { x -> -mitadArriba(x) },
            escalaIgual = true,
            f = mitadArriba
        )
        enlace.planoSonoro.vincularBotones(enlace.btnSonidoCirculo)
    }

    override fun onStop() {
        super.onStop()
        enlace.planoSonoro.detener()
    }
}
