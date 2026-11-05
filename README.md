Toshokan 図書館

App de Android para explorar anime, hecha en Kotlin con la API de Jikan (MyAnimeList).
Toshokan significa "biblioteca" en japonés.

Qué tiene

Inicio con una recomendación del día y carruseles (top 10 y tus géneros favoritos)
Buscador que tolera errores (con "nrt" encontrás Naruto) y filtro por género
Top con podio y ranking completo
Detalle hecho en Jetpack Compose, con la lista de episodios y su puntaje
Favoritos guardados con SQLite, se pueden ver sin internet

Cómo correrla

Abrir el proyecto en Android Studio
Sincronizar Gradle
Ejecutar en un celular o emulador (Android 7 o más)

Ahora mismo usa datos de prueba porque Jikan estaba caído. Para usar la API real, en `data/RetrofitClient.kt` cambiar `USE_FAKE_DATA` a `false`.

Endpoints

- `top/anime`
- `anime?q=`
- `anime?genres=`
- `anime/{id}/episodes`

VIDEO: https://youtube.com/shorts/AZIyQY2GYgM?feature=share
