package com.example.sara

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sara.databinding.VistaConceptosBinding

class ConceptosActivity : AppCompatActivity() {
    private lateinit var enlace: VistaConceptosBinding

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)
        enlace = VistaConceptosBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        enlace.root.respetarBarrasDelSistema()
        enlace.btnRegresar.setOnClickListener { regresarAAprendizaje() }
    }
}
