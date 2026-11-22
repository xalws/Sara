package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.sqrt
import com.example.sara.databinding.VistaFuncionConicasBinding


class FuncionConicasActivity : AppCompatActivity() {
    private lateinit var enlace: VistaFuncionConicasBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaFuncionConicasBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // Las ventanas están elegidas para que los extremos (±5, ±3, ±2) caigan en una muestra del dibujo y las dos mitades se unan sin hueco

        // Circunferencia x² + y² = 25
        val circunferencia = { x: Double -> raizSegura(25 - x * x) }
        enlace.planoCircunferencia.configurar(
            "la circunferencia x cuadrada más y cuadrada igual a 25. Suenan a la vez la mitad de arriba, más aguda, y la de abajo, más grave",
            -6.25, 6.25, -6.25, 6.25,
            complemento = { x -> -circunferencia(x) },
            escalaIgual = true,
            f = circunferencia
        )
        enlace.planoCircunferencia.vincularBotones(enlace.btnSonidoCircunferencia)

        // Elipse x²/9 + y²/4 = 1
        val elipse = { x: Double -> 2 * raizSegura(1 - x * x / 9) }
        enlace.planoElipse.configurar(
            "la elipse x cuadrada entre 9 más y cuadrada entre 4 igual a 1. Suenan a la vez la mitad de arriba, más aguda, y la de abajo, más grave",
            -4.0, 4.0, -3.0, 3.0,
            complemento = { x -> -elipse(x) },
            escalaIgual = true,
            f = elipse
        )
        enlace.planoElipse.vincularBotones(enlace.btnSonidoElipse)

        // Hipérbola X²/4 − Y²/9 = 1
        val hiperbola = { x: Double -> 3 * raizSegura(x * x / 4 - 1) }
        enlace.planoHiperbola.configurar(
            "la hipérbola x cuadrada entre 4 menos y cuadrada entre 9 igual a 1. Suenan a la vez la mitad de arriba, más aguda, y la de abajo, más grave",
            -5.0, 5.0, -7.0, 7.0,
            complemento = { x -> -hiperbola(x) },
            f = hiperbola
        )
        enlace.planoHiperbola.vincularBotones(enlace.btnSonidoHiperbola)
    }

    /** Raíz cuadrada que tolera errores de redondeo en los extremos fuera de ellos no existe (NaN) */
    private fun raizSegura(valor: Double): Double =
        if (valor < -1e-9) Double.NaN else sqrt(max(0.0, valor))

    override fun onStop() {
        super.onStop()
        enlace.planoCircunferencia.detener()
        enlace.planoElipse.detener()
        enlace.planoHiperbola.detener()
    }
}
