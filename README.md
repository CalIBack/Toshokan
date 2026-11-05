# Toshokan 図書館

*Toshokan* significa "biblioteca" en japonés.

Aplicación Android en **Kotlin** para explorar series y películas de anime, consumiendo los endpoints públicos de MyAnimeList a través de **[Jikan API](https://jikan.moe/)**.

## Funcionalidades

- **Inicio:** recomendación del día (cambia en cada apertura; si hay favoritos, sale de tus géneros preferidos), carrusel del Top 10 con ranking y puntuación, y hasta 3 carruseles con los mejores animes de los géneros que más aparecen en tus favoritos.
- **Buscar:** búsqueda difusa ("nrt" encuentra "Naruto") combinada con la búsqueda de Jikan, más filtro por género.
- **Top ratings:** podio con marcos dorado, plateado y bronce, y ranking completo con scroll infinito. Empates de puntuación ordenados alfabéticamente.
- **Detalle (Jetpack Compose):** portada HD, títulos alternativos, puntuación, ranking, popularidad, información técnica, géneros, sinopsis expandible y **lista de episodios con su puntuación**.
- **Favoritos (SQLite):** buscador propio y filtro por género; funciona sin internet.
- Barra de navegación inferior, estados de carga, pantallas de error con "Reintentar" y control del límite de peticiones de Jikan.

## Arquitectura

```
Fragment (XML) / Compose  →  ViewModel (StateFlow)  →  AnimeRepository  →  Jikan (Retrofit) / FakeJikanApi / SQLite
```

## Endpoints utilizados (todos de Jikan)

- `GET /v4/top/anime?page={n}` — ranking
- `GET /v4/anime?q={query}` — búsqueda por nombre
- `GET /v4/anime?genres={id}&order_by=score&sort=desc` — mejores de un género
- `GET /v4/anime/{id}/episodes?page={n}` — episodios con puntuación

## Datos de prueba

Si la API de Jikan no está disponible, la app funciona con datos locales de prueba.
En `data/RetrofitClient.kt`:

```kotlin
private const val USE_FAKE_DATA = false // true = datos de prueba, false = API real
```

## Cómo ejecutar

1. Abrir el proyecto en **Android Studio** y sincronizar Gradle.
2. Ejecutar en un dispositivo con Android 7.0 (API 24) o superior.
3. Tests unitarios: clic derecho en `app/src/test/.../FuzzySearchTest.kt` → Run.
