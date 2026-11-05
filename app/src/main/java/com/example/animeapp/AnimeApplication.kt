package com.example.animeapp

import android.app.Application
import com.example.animeapp.data.AnimeRepository

// Se crea antes que cualquier pantalla: prepara el repositorio (y SQLite)
class AnimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AnimeRepository.init(this)
    }
}
