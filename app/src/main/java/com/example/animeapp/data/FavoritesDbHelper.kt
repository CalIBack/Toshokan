package com.example.animeapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson

// Base de datos SQLite local para los favoritos.
// - Columnas propias para lo que se consulta con SQL (título, puntaje, géneros, fecha).
// - Columna data_json con el anime completo, para mostrar el detalle sin internet.
class FavoritesDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "anime_favorites.db"
        // Versión 2: se agregaron ids de géneros y el JSON completo
        private const val DATABASE_VERSION = 2

        private const val TABLE = "favorites"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_SCORE = "score"
        private const val COL_IMAGE = "image_url"
        private const val COL_GENRES = "genres"     // formato: "|1:Action|8:Drama|"
        private const val COL_SAVED_AT = "saved_at"
        private const val COL_JSON = "data_json"
    }

    private val gson = Gson()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_TITLE TEXT NOT NULL,
                $COL_SCORE REAL,
                $COL_IMAGE TEXT,
                $COL_GENRES TEXT NOT NULL,
                $COL_SAVED_AT INTEGER NOT NULL,
                $COL_JSON TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    // De la versión 1 a la 2 cambió la estructura: se recrea la tabla
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    // INSERT (o reemplazo si ya existía)
    fun addFavorite(anime: Anime) {
        val genresText = anime.genres.orEmpty()
            .joinToString(separator = "|", prefix = "|", postfix = "|") { "${it.id}:${it.name}" }
        val values = ContentValues().apply {
            put(COL_ID, anime.id)
            put(COL_TITLE, anime.title)
            put(COL_SCORE, anime.score)
            put(COL_IMAGE, anime.coverUrl())
            put(COL_GENRES, genresText)
            put(COL_SAVED_AT, System.currentTimeMillis())
            put(COL_JSON, gson.toJson(anime))
        }
        writableDatabase.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // DELETE
    fun removeFavorite(animeId: Int) {
        writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(animeId.toString()))
    }

    fun isFavorite(animeId: Int): Boolean {
        readableDatabase.query(
            TABLE, arrayOf(COL_ID), "$COL_ID = ?", arrayOf(animeId.toString()),
            null, null, null
        ).use { cursor ->
            return cursor.count > 0
        }
    }

    // SELECT solo de los ids (para saber rápido qué tarjetas llevan corazón lleno)
    fun getFavoriteIds(): Set<Int> {
        val ids = mutableSetOf<Int>()
        readableDatabase.query(TABLE, arrayOf(COL_ID), null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) ids.add(cursor.getInt(0))
        }
        return ids
    }

    // SELECT de todos o filtrado por género (con LIKE sobre la columna genres).
    // "|1:" no coincide con "|10:", por eso se guardan los separadores.
    fun getFavorites(genreId: Int? = null): List<Anime> {
        val selection = if (genreId != null) "$COL_GENRES LIKE ?" else null
        val args = if (genreId != null) arrayOf("%|$genreId:%") else null

        val result = mutableListOf<Anime>()
        readableDatabase.query(
            TABLE, arrayOf(COL_JSON), selection, args, null, null, "$COL_SAVED_AT DESC"
        ).use { cursor ->
            val jsonIndex = cursor.getColumnIndexOrThrow(COL_JSON)
            while (cursor.moveToNext()) {
                val anime = runCatching {
                    gson.fromJson(cursor.getString(jsonIndex), Anime::class.java)
                }.getOrNull()
                if (anime != null) result.add(anime)
            }
        }
        return result
    }
}
