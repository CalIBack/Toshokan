package com.example.animeapp.data

import java.text.Normalizer

// Búsqueda "tolerante": encuentra "Naruto" escribiendo "nar", "nrt" o "naru".
// Devuelve un puntaje (más alto = mejor coincidencia) o null si no coincide.
object FuzzySearch {

    fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents.replace(Regex("[^a-z0-9 ]"), " ").replace(Regex(" +"), " ").trim()
    }

    fun score(query: String, text: String): Int? {
        val q = normalize(query).replace(" ", "")
        val t = normalize(text)
        if (q.isEmpty() || t.isEmpty()) return null

        val compact = t.replace(" ", "")

        // 1) El título empieza con lo escrito: "naru" -> "Naruto"
        if (compact.startsWith(q)) return 1000 - compact.length

        // 2) Lo escrito aparece dentro del título: "chemist" -> "Fullmetal Alchemist"
        val index = compact.indexOf(q)
        if (index >= 0) return 800 - index

        // 3) Iniciales de cada palabra: "fmab" -> "FullMetal Alchemist Brotherhood"
        val words = t.split(" ")
        val initials = words.joinToString("") { it.first().toString() }
        if (q.length >= 2 && initials.startsWith(q)) return 700 - initials.length

        // 4) Letras en orden con huecos: "nrt" -> "NaRuTo"
        // La primera letra tiene que ser el comienzo de una palabra,
        // y no se permiten demasiados huecos (si no, todo coincidiría con todo).
        val wordStarts = mutableSetOf<Int>()
        var position = 0
        for (word in words) {
            wordStarts.add(position)
            position += word.length
        }

        var from = 0
        var first = -1
        var last = -1
        var gaps = 0
        for (c in q) {
            val found = compact.indexOf(c, from)
            if (found < 0) return null
            if (first < 0) first = found
            if (last >= 0) gaps += found - last - 1
            last = found
            from = found + 1
        }
        if (first !in wordStarts) return null
        if (gaps > maxOf(3, q.length * 2)) return null
        return 500 - gaps * 10 - first
    }

    // Mejor puntaje entre varios títulos (ej: japonés e inglés)
    fun bestScore(query: String, titles: List<String>): Int? =
        titles.mapNotNull { score(query, it) }.maxOrNull()

    // Filtra y ordena una lista: mejores coincidencias primero
    fun <T> filter(query: String, items: List<T>, titlesOf: (T) -> List<String>): List<T> =
        items.mapNotNull { item -> bestScore(query, titlesOf(item))?.let { item to it } }
            .sortedByDescending { it.second }
            .map { it.first }
}
