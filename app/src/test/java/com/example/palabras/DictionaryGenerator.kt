package com.example.palabras

import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.Normalizer

fun main() {
    val wordSources = listOf(
        "https://raw.githubusercontent.com/dwyl/spanish-words/master/spanish_words_list.txt",
        "https://raw.githubusercontent.com/javierarce/spanish-wordlist/master/palabras.txt"
    )

    val finalWords = mutableSetOf<String>()
    val regex = Regex("^[a-zñ]{3,20}$")

    println("Descargando y procesando corpus...")

    wordSources.forEach { source ->
        try {
            URL(source).openStream().bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val normalized = normalize(line)
                    if (regex.matches(normalized)) {
                        finalWords.add(normalized)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error cargando fuente: $source")
        }
    }

    // Guardar en assets
    val json = JSONObject()
    json.put("palabras", finalWords.toList().sorted())

    val outputFile = File("app/src/main/assets/dictionary.json")
    outputFile.parentFile.mkdirs()
    outputFile.writeText(json.toString())

    println("¡Éxito! Se han generado ${finalWords.size} palabras en ${outputFile.absolutePath}")
}

fun normalize(input: String): String {
    val string = input.lowercase().trim()
    val normalized = Normalizer.normalize(string, Normalizer.Form.NFD)
    val result = StringBuilder()
    for (char in normalized) {
        when (char) {
            'ñ' -> result.append('ñ')
            in 'a'..'z' -> result.append(char)
            // Ignorar tildes y caracteres especiales
        }
    }
    return result.toString()
}
