package com.example.sara

import org.json.JSONArray
import org.json.JSONObject

/**
  modos: "explicita" (y = f(x)) o "implicita"
  parametros: letras distintas de x, y que se convierten en controles deslizantes (a, b, candidato...).
 */
data class PlanGrafica(
    val modo: String,
    val curva: String,
    val evaluacion: String,
    val parametros: List<String>
) {
    fun aJson(valores: Map<String, Double>): JSONObject {
        val listaParametros = JSONArray()
        parametros.forEach { nombre ->
            listaParametros.put(JSONObject().put("nombre", nombre).put("valor", valores[nombre] ?: 1.0))
        }
        return JSONObject()
            .put("modo", modo)
            .put("curva", curva)
            .put("evaluacion", evaluacion)
            .put("parametros", listaParametros)
    }
}

sealed class Analisis {
    data class Valido(val plan: PlanGrafica) : Analisis()
    data class Invalido(val mensaje: String) : Analisis()
}

object AnalizadorFuncion {

    const val MAX_PARAMETROS = 6

    private val alias = mapOf(
        "sen" to "sin", "tg" to "tan", "tang" to "tan", "ctg" to "cot", "cosec" to "csc",
        "arcsen" to "asin", "arcsin" to "asin", "arccos" to "acos", "arctan" to "atan",
        "arctg" to "atan", "raiz" to "sqrt", "log" to "log10"
    )

    private val funciones = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "sqrt", "cbrt", "abs", "exp", "ln", "log10",
        "floor", "ceil", "round", "sgn"
    )

    private val candidatos: List<String> =
        (alias.keys + funciones + "pi").sortedByDescending { it.length }

    private val letras = Regex("[A-Za-z]+")
    private val encabezado = Regex("^([A-Za-z]\\s*\\(\\s*x\\s*\\)|y)$")

    fun analizar(entrada: String): Analisis {
        val texto = expandirLetras(limpiar(entrada))
        if (texto.isEmpty()) return Analisis.Invalido("Escribe una función o ecuación.")
        if (tieneVariable(texto, 'z')) {
            return Analisis.Invalido("Por ahora solo se grafica en dos dimensiones. Usa x e y.")
        }

        val partes = texto.split("=")
        if (partes.size > 2) return Analisis.Invalido("Hay más de un signo igual.")

        val plan: PlanGrafica
        if (partes.size == 2) {
            val izq = partes[0].trim()
            val der = partes[1].trim()
            if (izq.isEmpty() || der.isEmpty()) {
                return Analisis.Invalido("Falta un lado de la ecuación.")
            }
            plan = when {
                encabezado.matches(izq) && !tieneVariable(der, 'y') -> explicita(der)
                der == "y" && !tieneVariable(izq, 'y') -> explicita(izq)
                else -> implicita(izq, der)
            }
        } else {
            plan = if (tieneVariable(texto, 'y')) implicita(texto, "0") else explicita(texto)
        }

        if (plan.parametros.size > MAX_PARAMETROS) {
            return Analisis.Invalido("Máximo $MAX_PARAMETROS parámetros (letras distintas de x e y).")
        }
        return Analisis.Valido(plan)
    }

    private fun explicita(expresion: String): PlanGrafica =
        PlanGrafica("explicita", expresion, "", parametros(expresion))

    private fun implicita(izq: String, der: String): PlanGrafica =
        PlanGrafica("implicita", "$izq = $der", "($izq)-($der)", parametros("$izq $der"))

    private fun limpiar(texto: String): String = texto
        .replace("²", "^2").replace("³", "^3")
        .replace("π", "pi").replace("√", "sqrt")
        .replace('−', '-').replace('×', '*').replace('·', '*').replace('÷', '/')
        .replace('[', '(').replace(']', ')')
        .replace(Regex("(\\d),(\\d)"), "$1.$2") // coma decimal
        .replace(Regex("\\s+"), " ")
        .trim()

    /** "ax" -> "a*x", "sinx" -> "sin(x)", "sen" -> "sin", "pix" -> "pi*x". */
    private fun expandirLetras(texto: String): String =
        letras.replace(texto) { expandir(it.value) }

    private fun expandir(grupo: String): String {
        val bajo = grupo.lowercase()
        val resultado = StringBuilder()
        var i = 0
        while (i < grupo.length) {
            if (resultado.isNotEmpty()) resultado.append('*')
            val candidato = candidatos.firstOrNull { bajo.startsWith(it, i) }
            if (candidato == null) {
                resultado.append(grupo[i])
                i++
            } else if (candidato == "pi") {
                resultado.append("pi")
                i += 2
            } else {
                resultado.append(alias[candidato] ?: candidato)
                i += candidato.length
                if (i < grupo.length) {
                    // función pegada a su argumento: sinx -> sin(x)
                    resultado.append('(').append(expandir(grupo.substring(i))).append(')')
                }
                return resultado.toString()
            }
        }
        return resultado.toString()
    }

    private fun tieneVariable(texto: String, variable: Char): Boolean =
        Regex("(?<![A-Za-z])" + variable + "(?![A-Za-z])").containsMatchIn(texto)

    private fun parametros(texto: String): List<String> =
        letras.findAll(texto).map { it.value }
            .filter { it.length == 1 && it !in setOf("x", "y", "z", "e", "i") }
            .distinct()
            .toList()
}
