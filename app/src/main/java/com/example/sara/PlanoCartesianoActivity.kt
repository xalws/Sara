package com.example.sara

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sara.databinding.VistaPlanoCartesianoBinding

class PlanoCartesianoActivity : AppCompatActivity() {
    private lateinit var enlace: VistaPlanoCartesianoBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaPlanoCartesianoBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // "Siguiente" abre la parte 2 (cuadrantes)
        enlace.btnSiguientePlano.setOnClickListener {
            startActivity(Intent(this, PlanoCartesiano2Activity::class.java))
        }
    }
}
