package com.example.animeapp

import android.app.Application
import com.example.animeapp.data.AnimeRepository

// Inicia el repositorio al abrir la app
class AnimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AnimeRepository.init(this)
    }
}
