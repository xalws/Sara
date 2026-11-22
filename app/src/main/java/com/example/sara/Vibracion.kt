package com.example.sara

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
Pistas táctiles para los ejes:
- Eje X (la gráfica lo cruza o lo toca): una vibración firme.
- Eje Y (x = 0): dos toques cortos.
 */
class Vibracion(contexto: Context) {

    private val vibrador: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contexto.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            contexto.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun ejeX() = vibrar(longArrayOf(0, 60))

    fun ejeY() = vibrar(longArrayOf(0, 20, 70, 20))

    private fun vibrar(patron: LongArray) {
        val dispositivo = vibrador ?: return
        if (!dispositivo.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dispositivo.vibrate(VibrationEffect.createWaveform(patron, -1))
        } else {
            @Suppress("DEPRECATION")
            dispositivo.vibrate(patron, -1)
        }
    }
}
