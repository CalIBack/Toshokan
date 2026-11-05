package com.example.animeapp.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.animeapp.AnimeDetailFragment
import com.example.animeapp.R
import com.example.animeapp.data.Anime
import com.google.android.material.bottomnavigation.BottomNavigationView

// Abre el detalle desde cualquier pestaña, mandando el anime completo
fun Fragment.openAnimeDetail(anime: Anime) {
    val bundle = Bundle().apply { putSerializable(AnimeDetailFragment.ARG_ANIME, anime) }
    findNavController().navigate(R.id.action_global_animeDetail, bundle)
}

// Cambia de pestaña como si el usuario tocara la barra inferior
fun Fragment.selectTab(tabId: Int) {
    requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = tabId
}
