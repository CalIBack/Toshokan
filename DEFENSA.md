# Guía de defensa — Toshokan

Preguntas probables y cómo responderlas, en orden de cómo funciona la app.

## 1. ¿Cómo se conecta la app a la API?
- `JikanApi` es una **interfaz** con funciones anotadas (`@GET`, `@Query`). Retrofit genera la implementación que arma la URL y hace la petición HTTP.
- `RetrofitClient` es un `object` (**singleton**): una única instancia de Retrofit para toda la app. `BASE_URL` termina en `/` y cada `@GET` se agrega a continuación.
- **Gson** convierte el JSON en las `data class` de `AnimeModels.kt`. `@SerializedName` une el nombre del JSON (`mal_id`) con el de Kotlin (`id`).
- Los campos que pueden faltar (`score`, `episodes`, `synopsis`) son **nullable** (`?`), y en la UI se muestra "—" o "Sin sinopsis".

## 2. ¿Por qué `suspend` y `lifecycleScope.launch`?
- Una petición de red no puede hacerse en el hilo principal (la app se congelaría). Las **coroutines** la ejecutan sin bloquear la UI.
- `viewLifecycleOwner.lifecycleScope` cancela la tarea automáticamente si se cierra la pantalla.

## 3. ¿Cómo funciona el buscador "en tiempo real"?
- `doAfterTextChanged` se ejecuta en cada tecla.
- **Debounce:** se cancela la búsqueda anterior (`searchJob?.cancel()`) y se espera 500 ms (`delay(500)`). Solo se llama a la API cuando el usuario deja de escribir.
- Motivo: Jikan limita a ~3 peticiones por segundo; sin esto aparecen errores **HTTP 429**.
- Cancelar el `Job` también cancela la petición en curso, así no llegan resultados de una búsqueda vieja.
- Texto vacío → vuelve a cargar el top.

## 4. ¿Cómo funciona el RecyclerView?
- `AnimeAdapter` tiene 3 métodos clave:
  - `onCreateViewHolder`: crea una fila a partir de `anime_item.xml`.
  - `onBindViewHolder`: llena la fila (título, score, portada con **Picasso**).
  - `getItemCount`: cuántos elementos hay.
- El **ViewHolder** guarda las vistas para no buscarlas cada vez; las filas se **reciclan** al hacer scroll.
- El clic se recibe con una lambda (`onItemClick`) que define el Fragment.

## 5. ¿Cómo se navega entre pantallas?
- **Navigation Component**: `nav_graph.xml` define los destinos (lista, detalle, favoritos) y las acciones entre ellos.
- `MainActivity` solo contiene la Toolbar y el `NavHostFragment`, que muestra el Fragment actual.
- El anime se pasa completo en un `Bundle`; por eso `Anime` implementa `Serializable`.

## 6. ¿Cómo conviven XML y Jetpack Compose?
- Lista y favoritos: Fragments con XML (lo que pide la consigna).
- Detalle: `AnimeDetailFragment` devuelve un **`ComposeView`**, y adentro todo es Compose (`Column`, `Text`, `Card`, `FlowRow`, `AssistChip`, `Button`).
- `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed` libera la UI de Compose al salir de la pantalla.
- Picasso no tiene versión para Compose, así que la imagen HD se muestra en un `ImageView` clásico dentro de **`AndroidView`**.

## 7. ¿Cómo funciona el estado en Compose (botón de favoritos)?
- `var isFavorite by remember { mutableStateOf(...) }`: cuando cambia el valor, Compose **redibuja** el botón automáticamente (recomposición).
- `remember` conserva el valor entre recomposiciones.

## 8. ¿Cómo funciona SQLite?
- `FavoritesDbHelper` extiende **`SQLiteOpenHelper`**:
  - `onCreate`: `CREATE TABLE` la primera vez que se usa la base.
  - `onUpgrade`: se ejecuta si se sube `DATABASE_VERSION`.
- Operaciones: `insertWithOnConflict` (guardar), `delete` (quitar), `query` (consultar/listar).
- Se guardan todos los datos del detalle → la pantalla de favoritos funciona **sin internet**.
- Los géneros (una lista) se guardan como texto separado por comas y se reconstruyen al leer.
- `cursor.use { }` cierra el cursor automáticamente.

## 9. ¿Qué es `FakeJikanApi`?
- Implementa la **misma interfaz** que la API real con 20 animes de prueba. Se usó mientras el servidor de Jikan estaba caído.
- El resto de la app no cambia: depende de la interfaz `JikanApi`, no de una implementación concreta.
- Se activa/desactiva con `USE_FAKE_DATA` en `RetrofitClient`.

## 10. Manejo de errores
- `HttpException` 429 → "Demasiadas búsquedas seguidas".
- Otros errores (sin conexión, timeout) → "Error de conexión".
- `CancellationException` se relanza: una búsqueda cancelada a propósito no es un error.
- Lista vacía → mensaje "Sin resultados".

## 11. ¿Por qué ViewModel y Repository?
- **ViewModel:** guarda el estado de cada pantalla. Sobrevive a la rotación y al cambio de pestaña, así no se repiten llamadas a la API. Expone un `StateFlow<UiState>` (Cargando / Éxito / Error) que el Fragment observa con `repeatOnLifecycle`.
- **Repository (`AnimeRepository`):** único lugar que sabe de dónde vienen los datos (Jikan, datos de prueba, caché en memoria o SQLite). Además controla el límite de Jikan: deja 400 ms entre peticiones y reintenta una vez si recibe 429.

## 12. ¿Cómo se arma el Inicio?
- **Top 10:** primera página de `/top/anime`, ordenada por puntuación y, si empatan, alfabéticamente (`Ranking.sort`).
- **Carruseles por género:** se cuentan los géneros de los favoritos, se toman los 3 más repetidos y se pide `/anime?genres=id&order_by=score` (hasta 12 por carrusel). Si no hay favoritos, no aparecen.
- **Recomendación del día:** se elige al azar una vez por apertura de la app. Si hay favoritos, sale de esos carruseles (sin repetir favoritos y con puntuación ≥ 7.5); si no, de los mejores del top.

## 13. ¿Cómo funciona la búsqueda difusa?
- `FuzzySearch.score` prueba, en orden: empieza con el texto, contiene el texto, iniciales ("fma") y letras en orden con huecos ("nrt" → NaRuTo).
- Limitación honesta: Jikan no hace búsqueda difusa, así que "nrt" solo encuentra animes que la app ya conoce (top, géneros, favoritos y búsquedas anteriores).
- Está cubierta por tests unitarios (`FuzzySearchTest`).

## 14. ¿Cómo funciona el podio y el scroll infinito?
- `TopAdapter` tiene **dos tipos de vista**: el podio (posiciones 1 a 3) y las filas (desde la 4).
- Al acercarse al final se pide la página siguiente (`OnScrollListener`), hasta 8 páginas (top 200).

## 15. ¿Cómo se filtran los favoritos?
- Por género, con **SQL**: `genres LIKE '%|1:%'` (se guardan como `|1:Action|8:Drama|` para que `|1:` no coincida con `|10:`).
- Por texto, con la búsqueda difusa en Kotlin.
- La base pasó a **versión 2** (`onUpgrade`): se agregaron los ids de géneros y el anime completo en JSON para mostrar el detalle sin internet.
