package com.example.animeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.data.Anime
import com.example.animeapp.data.HomeData
import com.example.animeapp.data.Translations
import com.example.animeapp.ui.CarouselAdapter
import com.example.animeapp.ui.CoverLoader
import com.example.animeapp.ui.HomeViewModel
import com.example.animeapp.ui.UiState
import com.example.animeapp.ui.openAnimeDetail
import com.example.animeapp.ui.selectTab
import com.google.android.material.card.MaterialCardView
import com.example.animeapp.data.AnimeRepository
import kotlinx.coroutines.launch

// Pantalla Inicio
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val viewModel: HomeViewModel by viewModels()

    private val carouselAdapters = mutableListOf<CarouselAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scroll: View = view.findViewById(R.id.scrollHome)
        val progress: ProgressBar = view.findViewById(R.id.progressHome)
        val error: View = view.findViewById(R.id.errorHome)

        view.findViewById<View>(R.id.fakeSearch).setOnClickListener { selectTab(R.id.searchFragment) }
        error.findViewById<View>(R.id.buttonRetry).setOnClickListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    AnimeRepository.favoriteIds.collect { ids ->
                        carouselAdapters.forEach { it.favoriteIds = ids }
                    }
                }
                viewModel.state.collect { state ->
                    progress.isVisible = state is UiState.Loading
                    error.isVisible = state is UiState.Error
                    scroll.isVisible = state is UiState.Success
                    when (state) {
                        is UiState.Success -> render(view, state.data)
                        is UiState.Error -> error.findViewById<TextView>(R.id.textErrorMessage).text = state.message
                        UiState.Loading -> Unit
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshIfFavoritesChanged()
    }

    // Arma destacado y carruseles
    private fun render(root: View, data: HomeData) {
        renderHero(root, data)

        val container: LinearLayout = root.findViewById(R.id.containerSections)
        container.removeAllViews()
        carouselAdapters.clear()

        addSection(
            container,
            title = "Top ratings",
            subtitle = "Los 10 mejor puntuados",
            animes = data.top10,
            showRank = true,
            onSeeAll = { selectTab(R.id.topFragment) }
        )

        data.genreSections.forEach { section ->
            addSection(
                container,
                title = Translations.genre(section.genre.name),
                subtitle = "Porque es de tus géneros favoritos",
                animes = section.animes,
                showRank = false,
                onSeeAll = null
            )
        }

        root.findViewById<View>(R.id.textFavoritesHint).isVisible = data.genreSections.isEmpty()
    }

    private fun renderHero(root: View, data: HomeData) {
        val card: MaterialCardView = root.findViewById(R.id.cardHero)
        val anime = data.pick
        card.isVisible = anime != null
        if (anime == null) return

        CoverLoader.load(root.findViewById<ImageView>(R.id.imageHero), anime.hdImageUrl(), anime.title)
        root.findViewById<TextView>(R.id.textHeroReason).text = data.pickReason
        root.findViewById<TextView>(R.id.textHeroTitle).text = anime.title
        root.findViewById<TextView>(R.id.textHeroMeta).text =
            listOf(Translations.score(anime.score), Translations.metaLine(anime))
                .filter { it.isNotEmpty() }
                .joinToString("  ·  ")
        root.findViewById<TextView>(R.id.textHeroGenres).text = Translations.genresLine(anime, max = 4)
        root.findViewById<TextView>(R.id.textHeroSynopsis).text = anime.synopsis.orEmpty()

        root.findViewById<View>(R.id.buttonHero).setOnClickListener { openAnimeDetail(anime) }
        card.setOnClickListener { openAnimeDetail(anime) }
    }

    // Agrega un carrusel
    private fun addSection(
        container: LinearLayout,
        title: String,
        subtitle: String,
        animes: List<Anime>,
        showRank: Boolean,
        onSeeAll: (() -> Unit)?
    ) {
        if (animes.isEmpty()) return
        val section = LayoutInflater.from(container.context)
            .inflate(R.layout.section_carousel, container, false)

        section.findViewById<TextView>(R.id.textSectionTitle).text = title
        section.findViewById<TextView>(R.id.textSectionSubtitle).text = subtitle

        val seeAll = section.findViewById<TextView>(R.id.textSeeAll)
        seeAll.isVisible = onSeeAll != null
        seeAll.setOnClickListener { onSeeAll?.invoke() }

        val adapter = CarouselAdapter(showRank, { openAnimeDetail(it) }, { toggleFavorite(it) })
        adapter.favoriteIds = AnimeRepository.favoriteIds.value
        carouselAdapters.add(adapter)
        section.findViewById<RecyclerView>(R.id.recyclerSection).apply {
            layoutManager = LinearLayoutManager(section.context, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
        adapter.submitList(animes)
        container.addView(section)
    }

    // Corazón
    private fun toggleFavorite(anime: Anime) {
        viewLifecycleOwner.lifecycleScope.launch { AnimeRepository.toggleFavorite(anime) }
    }
}
