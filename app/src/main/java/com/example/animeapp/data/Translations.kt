package com.example.animeapp.data

// Textos de la API al español
object Translations {
    private val genres = mapOf(
        "Action" to "Acción", "Adventure" to "Aventura", "Comedy" to "Comedia",
        "Mystery" to "Misterio", "Drama" to "Drama", "Fantasy" to "Fantasía",
        "Horror" to "Terror", "Romance" to "Romance", "Sci-Fi" to "Ciencia ficción",
        "Sports" to "Deportes", "Slice of Life" to "Recuentos de la vida",
        "Supernatural" to "Sobrenatural", "Suspense" to "Suspenso",
        "Award Winning" to "Premiado", "Gourmet" to "Gastronomía",
        "Avant Garde" to "Vanguardia", "Boys Love" to "Boys Love", "Girls Love" to "Girls Love"
    )

    fun genre(name: String): String = genres[name] ?: name

    fun status(status: String?): String = when (status) {
        "Finished Airing" -> "Finalizado"
        "Currently Airing" -> "En emisión"
        "Not yet aired" -> "Por estrenar"
        null -> "—"
        else -> status
    }

    fun type(type: String?): String? = when (type) {
        "Movie" -> "Película"
        "Special" -> "Especial"
        "Music" -> "Musical"
        else -> type
    }

    fun season(season: String?): String? = when (season) {
        "winter" -> "Invierno"
        "spring" -> "Primavera"
        "summer" -> "Verano"
        "fall" -> "Otoño"
        else -> null
    }

    fun metaLine(anime: Anime): String = listOfNotNull(
        type(anime.type),
        anime.year?.toString(),
        anime.episodes?.let { if (it == 1) "1 ep" else "$it eps" }
    ).joinToString(" · ")

    fun genresLine(anime: Anime, max: Int = 3): String =
        anime.genres.orEmpty().take(max).joinToString(" · ") { genre(it.name) }

    fun score(score: Double?): String = if (score != null) "★ %.2f".format(score) else "★ —"

    val browseGenres = listOf(
        Genre(1, "Action"), Genre(2, "Adventure"), Genre(4, "Comedy"), Genre(8, "Drama"),
        Genre(10, "Fantasy"), Genre(22, "Romance"), Genre(24, "Sci-Fi"), Genre(7, "Mystery"),
        Genre(37, "Supernatural"), Genre(41, "Suspense"), Genre(30, "Sports"),
        Genre(36, "Slice of Life"), Genre(14, "Horror")
    )
}
