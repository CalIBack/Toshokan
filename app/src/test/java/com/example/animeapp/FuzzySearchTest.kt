package com.example.animeapp

import com.example.animeapp.data.Anime
import com.example.animeapp.data.FuzzySearch
import com.example.animeapp.data.Ranking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchTest {
    private val titles = listOf(
        "Naruto", "Naruto: Shippuuden", "Fullmetal Alchemist: Brotherhood",
        "Kimetsu no Yaiba", "Kimi no Na wa.", "Hunter x Hunter (2011)"
    )

    private fun search(query: String) = FuzzySearch.filter(query, titles) { listOf(it) }

    @Test
    fun prefijo_encuentraNaruto() {
        assertEquals("Naruto", search("nar").first())
    }

    @Test
    fun letrasSalteadas_encuentranNaruto() {
        assertEquals("Naruto", search("nrt").first())
    }

    @Test
    fun iniciales_encuentranFullmetal() {
        assertEquals("Fullmetal Alchemist: Brotherhood", search("fma").first())
    }

    @Test
    fun ignoraMayusculasYAcentos() {
        assertTrue(search("NÁRUTO").contains("Naruto"))
    }

    @Test
    fun textoSinRelacion_noDevuelveNada() {
        assertTrue(search("zzz").isEmpty())
        assertNull(FuzzySearch.score("qwerty", "Naruto"))
    }

    @Test
    fun ranking_empateSeOrdenaAlfabeticamente() {
        val list = listOf(
            Anime(id = 1, title = "Zeta", score = 9.0),
            Anime(id = 2, title = "Alfa", score = 9.0),
            Anime(id = 3, title = "Beta", score = 9.5),
            Anime(id = 4, title = "Sin puntaje", score = null)
        )
        assertEquals(listOf("Beta", "Alfa", "Zeta", "Sin puntaje"), Ranking.sort(list).map { it.title })
    }
}
