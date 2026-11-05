package com.example.animeapp.data

// Ranking: mayor puntaje primero, empate alfabético
object Ranking {
    fun sort(animes: List<Anime>): List<Anime> =
        animes.distinctBy { it.id }
            .sortedWith(
                compareByDescending<Anime> { it.score ?: -1.0 }
                    .thenBy { it.title.lowercase() }
            )
}
