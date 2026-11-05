package com.example.animeapp.data

import kotlinx.coroutines.delay

// Datos de PRUEBA que imitan a la API de Jikan mientras el servidor no responde.
// Implementa la misma interfaz JikanApi: el resto de la app no sabe
// si los datos vienen de internet o de acá.
// Los valores son aproximados y solo sirven para probar la app.
object FakeJikanApi : JikanApi {

    private const val PAGE_SIZE = 25
    private const val EPISODES_PAGE_SIZE = 100

    override suspend fun getTopAnime(page: Int): AnimeResponse {
        delay(600) // simula la demora de la red
        val sorted = Ranking.sort(animes)
        return paginate(sorted, page, PAGE_SIZE)
    }

    override suspend fun searchAnime(query: String, limit: Int): AnimeResponse {
        delay(600)
        // Igual que Jikan: busca el texto dentro del título (no es difusa)
        val results = animes.filter { anime ->
            anime.allTitles().any { it.contains(query, ignoreCase = true) }
        }
        return AnimeResponse(results.take(limit), Pagination(hasNextPage = false))
    }

    override suspend fun getAnimeByGenre(genreId: Int, limit: Int): AnimeResponse {
        delay(600)
        val results = Ranking.sort(animes.filter { it.hasGenre(genreId) })
        return AnimeResponse(results.take(limit), Pagination(hasNextPage = results.size > limit))
    }

    override suspend fun getEpisodes(animeId: Int, page: Int): EpisodesResponse {
        delay(600)
        val anime = animes.firstOrNull { it.id == animeId }
        if (anime == null || anime.type == "Movie") {
            return EpisodesResponse(emptyList(), Pagination(hasNextPage = false))
        }
        // Si sigue en emisión (episodes = null) se simulan 40 episodios emitidos
        val total = anime.episodes ?: 40
        val from = (page - 1) * EPISODES_PAGE_SIZE + 1
        val to = minOf(total, page * EPISODES_PAGE_SIZE)
        val list = if (from > to) emptyList() else (from..to).map { number ->
            // Puntuación "pseudoaleatoria" pero siempre igual para el mismo episodio
            val score = 3.8 + ((animeId * 31 + number * 17) % 120) / 100.0
            Episode(
                number = number,
                title = "Episode $number",
                aired = fakeAiredDate(anime.year ?: 2010, number),
                score = Math.round(score * 100) / 100.0,
                filler = total > 100 && number % 9 == 0,
                recap = total > 24 && number % 25 == 0
            )
        }
        return EpisodesResponse(list, Pagination(hasNextPage = to < total))
    }

    private fun paginate(list: List<Anime>, page: Int, size: Int): AnimeResponse {
        val from = (page - 1) * size
        val items = list.drop(from).take(size)
        val lastPage = maxOf(1, (list.size + size - 1) / size)
        return AnimeResponse(items, Pagination(hasNextPage = page < lastPage, lastVisiblePage = lastPage))
    }

    private fun fakeAiredDate(year: Int, episode: Int): String {
        val week = episode - 1
        val y = year + week / 52
        val month = (week % 52) / 4 + 1
        val day = (week % 4) * 7 + 1
        return "%04d-%02d-%02dT00:00:00+00:00".format(y, month.coerceAtMost(12), day)
    }

    // ---------- Catálogo de prueba ----------

    private val genreNames = mapOf(
        1 to "Action", 2 to "Adventure", 4 to "Comedy", 7 to "Mystery", 8 to "Drama",
        10 to "Fantasy", 14 to "Horror", 22 to "Romance", 24 to "Sci-Fi", 30 to "Sports",
        36 to "Slice of Life", 37 to "Supernatural", 41 to "Suspense"
    )

    private fun anime(
        id: Int, title: String, english: String?, score: Double?, episodes: Int?,
        type: String, status: String, year: Int?, season: String?, studio: String,
        genreIds: List<Int>, synopsis: String
    ) = Anime(
        id = id,
        title = title,
        titleEnglish = english,
        type = type,
        episodes = episodes,
        status = status,
        score = score,
        scoredBy = if (score != null) (id * 7919) % 2_000_000 + 150_000 else null,
        popularity = (id * 13) % 500 + 1,
        members = (id * 104729) % 3_500_000 + 200_000,
        synopsis = synopsis,
        year = year,
        season = season,
        duration = if (type == "Movie") "1 hr 50 min" else "24 min per ep",
        rating = "PG-13 - Teens 13 or older",
        images = null, // sin URL: la app genera una portada con las iniciales
        genres = genreIds.map { Genre(it, genreNames.getValue(it)) },
        studios = listOf(Studio(studio))
    )

    private const val DONE = "Finished Airing"
    private const val AIRING = "Currently Airing"

    // El ranking (#1, #2...) se calcula ordenando por puntuación
    private val animes = withRanks(listOf(
        anime(52991, "Sousou no Frieren", "Frieren: Beyond Journey's End", 9.3, 28, "TV", DONE, 2023, "fall", "Madhouse", listOf(2, 8, 10),
            "After the hero's party defeats the Demon King, the elf mage Frieren outlives her companions and sets out on a new journey to understand people."),
        anime(5114, "Fullmetal Alchemist: Brotherhood", "Fullmetal Alchemist: Brotherhood", 9.1, 64, "TV", DONE, 2009, "spring", "Bones", listOf(1, 2, 8, 10),
            "Two brothers search for the Philosopher's Stone after a failed alchemy ritual costs them dearly."),
        anime(9253, "Steins;Gate", "Steins;Gate", 9.07, 24, "TV", DONE, 2011, "spring", "White Fox", listOf(8, 24, 41),
            "A self-proclaimed mad scientist discovers a way to send messages to the past, with terrible consequences."),
        anime(28977, "Gintama°", "Gintama Season 4", 9.06, 51, "TV", DONE, 2015, "spring", "Bandai Namco Pictures", listOf(1, 4, 24),
            "Samurai Gintoki and his friends take odd jobs in an Edo invaded by aliens."),
        anime(38524, "Shingeki no Kyojin Season 3 Part 2", "Attack on Titan Season 3 Part 2", 9.05, 10, "TV", DONE, 2019, "spring", "Wit Studio", listOf(1, 8, 41),
            "The Survey Corps returns to Shiganshina to uncover the truth hidden in the basement."),
        anime(11061, "Hunter x Hunter (2011)", "Hunter x Hunter", 9.03, 148, "TV", DONE, 2011, "fall", "Madhouse", listOf(1, 2, 10),
            "Gon leaves his island to become a Hunter and find the father who abandoned him."),
        anime(32281, "Kimi no Na wa.", "Your Name.", 8.83, 1, "Movie", DONE, 2016, null, "CoMix Wave Films", listOf(8, 22, 37),
            "Two teenagers who have never met start mysteriously swapping bodies."),
        anime(199, "Sen to Chihiro no Kamikakushi", "Spirited Away", 8.77, 1, "Movie", DONE, 2001, null, "Studio Ghibli", listOf(2, 37),
            "A girl trapped in a world of spirits must work in a bathhouse to save her parents."),
        anime(1, "Cowboy Bebop", "Cowboy Bebop", 8.75, 26, "TV", DONE, 1998, "spring", "Sunrise", listOf(1, 24),
            "A crew of bounty hunters travels the solar system aboard the spaceship Bebop."),
        anime(21, "One Piece", "One Piece", 8.72, null, "TV", AIRING, 1999, "fall", "Toei Animation", listOf(1, 2, 10),
            "Monkey D. Luffy sets sail to find the legendary treasure and become King of the Pirates."),
        anime(1535, "Death Note", "Death Note", 8.62, 37, "TV", DONE, 2006, "fall", "Madhouse", listOf(37, 41),
            "A student finds a notebook that kills anyone whose name is written in it."),
        anime(40748, "Jujutsu Kaisen", "Jujutsu Kaisen", 8.56, 24, "TV", DONE, 2020, "fall", "MAPPA", listOf(1, 10, 37),
            "A student swallows a cursed finger and joins a school of sorcerers."),
        anime(16498, "Shingeki no Kyojin", "Attack on Titan", 8.55, 25, "TV", DONE, 2013, "spring", "Wit Studio", listOf(1, 8, 41),
            "Humanity lives behind giant walls to protect itself from man-eating Titans."),
        anime(20583, "Haikyuu!!", "Haikyu!!", 8.43, 25, "TV", DONE, 2014, "spring", "Production I.G", listOf(4, 30),
            "A short boy with an incredible jump joins his high school volleyball team."),
        anime(30276, "One Punch Man", "One Punch Man", 8.49, 12, "TV", DONE, 2015, "fall", "Madhouse", listOf(1, 4),
            "A hero who can defeat any enemy with a single punch struggles with boredom."),
        anime(38000, "Kimetsu no Yaiba", "Demon Slayer: Kimetsu no Yaiba", 8.45, 26, "TV", DONE, 2019, "spring", "ufotable", listOf(1, 10, 37),
            "Tanjiro becomes a demon slayer to save his sister, who was turned into a demon."),
        anime(1735, "Naruto: Shippuuden", "Naruto Shippuden", 8.27, 500, "TV", DONE, 2007, "winter", "Pierrot", listOf(1, 2, 10),
            "Naruto returns after years of training to face the Akatsuki organization."),
        anime(23273, "Shigatsu wa Kimi no Uso", "Your Lie in April", 8.64, 22, "TV", DONE, 2014, "fall", "A-1 Pictures", listOf(8, 22),
            "A piano prodigy who lost his ability to hear music meets a free-spirited violinist."),
        anime(37450, "Seishun Buta Yarou wa Bunny Girl Senpai no Yume wo Minai", "Rascal Does Not Dream of Bunny Girl Senpai", 8.23, 13, "TV", DONE, 2018, "fall", "CloverWorks", listOf(8, 22, 37),
            "A high schooler meets an actress who has become invisible to everyone around her."),
        anime(20, "Naruto", "Naruto", 8.0, 220, "TV", DONE, 2002, "fall", "Pierrot", listOf(1, 2, 10),
            "A young ninja shunned by his village dreams of becoming its leader, the Hokage."),
        anime(269, "Bleach", "Bleach", 7.94, 366, "TV", DONE, 2004, "fall", "Pierrot", listOf(1, 2, 37),
            "A teenager gains the powers of a Soul Reaper and protects the living from evil spirits."),
        anime(31964, "Boku no Hero Academia", "My Hero Academia", 7.86, 13, "TV", DONE, 2016, "spring", "Bones", listOf(1),
            "A boy born without superpowers dreams of becoming a hero in a world full of them."),
        anime(31240, "Re:Zero kara Hajimeru Isekai Seikatsu", "Re:ZERO -Starting Life in Another World-", 8.23, 25, "TV", DONE, 2016, "spring", "White Fox", listOf(8, 10, 41),
            "A young man transported to a fantasy world discovers he returns to life after every death."),
        anime(34599, "Made in Abyss", "Made in Abyss", 8.65, 13, "TV", DONE, 2017, "summer", "Kinema Citrus", listOf(2, 8, 10, 7),
            "An orphan girl descends into a gigantic, mysterious abyss in search of her mother."),
        anime(99999, "Anime de prueba sin datos", null, null, null, "TV", "Not yet aired", null, null, "Desconocido", emptyList(),
            "")
    ))

    private fun withRanks(list: List<Anime>): List<Anime> {
        val ranks = Ranking.sort(list.filter { it.score != null })
            .mapIndexed { index, anime -> anime.id to index + 1 }
            .toMap()
        return list.map { it.copy(rank = ranks[it.id]) }
    }
}
