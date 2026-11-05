package com.example.animeapp.data

// Orden de los rankings: mayor puntuación primero; si hay empate, por orden alfabético.
// Los animes sin puntuación van al final.
object Ranking {
    fun sort(animes: List<Anime>): List<Anime> =
        animes.distinctBy { it.id }
            .sortedWith(
                compareByDescending<Anime> { it.score ?: -1.0 }
                    .thenBy { it.title.lowercase() }
            )
}
