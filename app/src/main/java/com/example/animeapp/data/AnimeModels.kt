package com.example.animeapp.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Respuestas de Jikan
data class AnimeResponse(
    @field:SerializedName("data") val data: List<Anime>?,
    @field:SerializedName("pagination") val pagination: Pagination? = null
)

data class EpisodesResponse(
    @field:SerializedName("data") val data: List<Episode>?,
    @field:SerializedName("pagination") val pagination: Pagination? = null
)

data class Pagination(
    @field:SerializedName("has_next_page") val hasNextPage: Boolean = false,
    @field:SerializedName("last_visible_page") val lastVisiblePage: Int = 1
)

// Anime (Serializable para pasarlo entre pantallas)
data class Anime(
    @field:SerializedName("mal_id") val id: Int,
    @field:SerializedName("title") val title: String,
    @field:SerializedName("title_english") val titleEnglish: String? = null,
    @field:SerializedName("title_japanese") val titleJapanese: String? = null,
    @field:SerializedName("type") val type: String? = null,
    @field:SerializedName("episodes") val episodes: Int? = null,
    @field:SerializedName("status") val status: String? = null,
    @field:SerializedName("score") val score: Double? = null,
    @field:SerializedName("scored_by") val scoredBy: Int? = null,
    @field:SerializedName("rank") val rank: Int? = null,
    @field:SerializedName("popularity") val popularity: Int? = null,
    @field:SerializedName("members") val members: Int? = null,
    @field:SerializedName("synopsis") val synopsis: String? = null,
    @field:SerializedName("year") val year: Int? = null,
    @field:SerializedName("season") val season: String? = null,
    @field:SerializedName("duration") val duration: String? = null,
    @field:SerializedName("rating") val rating: String? = null,
    @field:SerializedName("images") val images: Images? = null,
    @field:SerializedName("genres") val genres: List<Genre>? = null,
    @field:SerializedName("studios") val studios: List<Studio>? = null
) : Serializable {
    fun coverUrl(): String? = images?.jpg?.imageUrl ?: images?.jpg?.largeImageUrl

    fun hdImageUrl(): String? = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl

    fun allTitles(): List<String> = listOfNotNull(title, titleEnglish)

    fun hasGenre(genreId: Int): Boolean = genres.orEmpty().any { it.id == genreId }
}

data class Images(
    @field:SerializedName("jpg") val jpg: ImageUrls?
) : Serializable

data class ImageUrls(
    @field:SerializedName("image_url") val imageUrl: String?,
    @field:SerializedName("large_image_url") val largeImageUrl: String?
) : Serializable

data class Genre(
    @field:SerializedName("mal_id") val id: Int,
    @field:SerializedName("name") val name: String
) : Serializable

data class Studio(
    @field:SerializedName("name") val name: String
) : Serializable

data class Episode(
    @field:SerializedName("mal_id") val number: Int,
    @field:SerializedName("title") val title: String?,
    @field:SerializedName("aired") val aired: String?,
    @field:SerializedName("score") val score: Double?,
    @field:SerializedName("filler") val filler: Boolean = false,
    @field:SerializedName("recap") val recap: Boolean = false
)

// Datos del inicio
data class GenreSection(val genre: Genre, val animes: List<Anime>)

data class HomeData(
    val pick: Anime?,
    val pickReason: String,
    val top10: List<Anime>,
    val genreSections: List<GenreSection>
)
