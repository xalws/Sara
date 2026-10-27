package com.example.sara

import org.json.JSONArray
import org.json.JSONObject

/**
 * modo: "explicita" (y = f(x)) o "implicita" (cualquier ecuación en x, y).
 * parametros: letras distintas de x, y que se convierten en controles deslizantes (a, b, c...).
 */
data class PlanGrafica(
    val modo: String,
    val curva: String,
    val evaluacion: String,
    val parametros: List<String>
) {
    fun aJson(valores: Map<String, Double>): JSONObject {
        val params = JSONArray()
        parametros.forEach { n ->
            params.put(JSONObject().put("n", n).put("v", valores[n] ?: 1.0))
        }
        return JSONObject()
            .put("modo", modo)
            .put("curva", curva)
            .put("eval", evaluacion)
            .put("params", params)
    }
}

sealed class Analisis {
    data class Valido(val plan: PlanGrafica) : Analisis()
    data class Invalido(val mensaje: String) : Analisis()
}

object AnalizadorFuncion {

    const val MAX_PARAMETROS = 6

    // Español -> GeoGebra. "log" es base 10 (como en los libros); "ln" es el natural.
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

    // Más largos primero para que "arcsen" gane a "sen"
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

    private fun explicita(expr: String): PlanGrafica =
        PlanGrafica("explicita", expr, "", parametros(expr))

    private fun implicita(izq: String, der: String): PlanGrafica =
        PlanGrafica("implicita", "$izq = $der", "($izq)-($der)", parametros("$izq $der"))

    private fun limpiar(s: String): String = s
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

    private fun expandir(run: String): String {
        val bajo = run.lowercase()
        val sb = StringBuilder()
        var i = 0
        while (i < run.length) {
            if (sb.isNotEmpty()) sb.append('*')
            val c = candidatos.firstOrNull { bajo.startsWith(it, i) }
            if (c == null) {
                sb.append(run[i])
                i++
            } else if (c == "pi") {
                sb.append("pi")
                i += 2
            } else {
                sb.append(alias[c] ?: c)
                i += c.length
                if (i < run.length) {
                    // función pegada a su argumento: sinx -> sin(x)
                    sb.append('(').append(expandir(run.substring(i))).append(')')
                }
                return sb.toString()
            }
        }
        return sb.toString()
    }

    private fun tieneVariable(texto: String, v: Char): Boolean =
        Regex("(?<![A-Za-z])" + v + "(?![A-Za-z])").containsMatchIn(texto)

    private fun parametros(texto: String): List<String> =
        letras.findAll(texto).map { it.value }
            .filter { it.length == 1 && it !in setOf("x", "y", "z", "e", "i") }
            .distinct()
            .toList()
}
