package com.example.animeapp.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Endpoints de Jikan
interface JikanApi {
    @GET("top/anime?sfw=true")
    suspend fun getTopAnime(@Query("page") page: Int): AnimeResponse

    @GET("anime?sfw=true")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int
    ): AnimeResponse

    @GET("anime?sfw=true&order_by=score&sort=desc")
    suspend fun getAnimeByGenre(
        @Query("genres") genreId: Int,
        @Query("limit") limit: Int
    ): AnimeResponse

    @GET("anime/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") animeId: Int,
        @Query("page") page: Int
    ): EpisodesResponse
}
