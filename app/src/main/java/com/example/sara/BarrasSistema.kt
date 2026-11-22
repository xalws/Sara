package com.example.sara

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.respetarBarrasDelSistema() {
    val paddingIzquierdo = paddingLeft
    val paddingSuperior = paddingTop
    val paddingDerecho = paddingRight
    val paddingInferior = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { vista, margenes ->
        val barras = margenes.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val teclado = margenes.getInsets(WindowInsetsCompat.Type.ime())
        vista.setPadding(
            paddingIzquierdo + barras.left,
            paddingSuperior + barras.top,
            paddingDerecho + barras.right,
            paddingInferior + maxOf(barras.bottom, teclado.bottom)
        )
        margenes
    }
}
