package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sara.databinding.VistaPlanoCartesiano2Binding

class PlanoCartesiano2Activity : AppCompatActivity() {
    private lateinit var enlace: VistaPlanoCartesiano2Binding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaPlanoCartesiano2Binding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }

        // "Anterior" cierra esta pantalla y regresa a la parte 1
        enlace.btnAnteriorPlano.setOnClickListener {
            finish()
        }
    }
}
