package com.example.sara

import android.app.Activity
import android.content.Intent

const val EXTRA_SECCION = "seccion"
const val SECCION_APRENDIZAJE = "aprendizaje"

/**
 * CLEAR_TOP cierra las pantallas de temas abiertas
 * y SINGLE_TOP reutiliza la MainActivity existente, que recibe el Intent en onNewIntent.
 */
fun Activity.regresarAAprendizaje() {
    val intencion = Intent(this, MainActivity::class.java)
        .putExtra(EXTRA_SECCION, SECCION_APRENDIZAJE)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    startActivity(intencion)
    finish()
}
