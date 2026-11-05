package com.example.animeapp.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Endpoints de Jikan (MyAnimeList). sfw=true filtra contenido para adultos.
interface JikanApi {

    // Animes mejor puntuados, de a 25 por página: /v4/top/anime?page=1
    @GET("top/anime?sfw=true")
    suspend fun getTopAnime(@Query("page") page: Int): AnimeResponse

    // Búsqueda por nombre: /v4/anime?q=naruto
    @GET("anime?sfw=true")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int
    ): AnimeResponse

    // Mejores animes de un género: /v4/anime?genres=1&order_by=score&sort=desc
    @GET("anime?sfw=true&order_by=score&sort=desc")
    suspend fun getAnimeByGenre(
        @Query("genres") genreId: Int,
        @Query("limit") limit: Int
    ): AnimeResponse

    // Episodios con su puntuación: /v4/anime/{id}/episodes?page=1 (100 por página)
    @GET("anime/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") animeId: Int,
        @Query("page") page: Int
    ): EpisodesResponse
}
