package com.example.animeapp.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import com.example.animeapp.data.Anime
import com.example.animeapp.data.Translations
import com.google.android.material.card.MaterialCardView

// DiffUtil compara la lista vieja con la nueva y solo redibuja lo que cambió
private val AnimeDiff = object : DiffUtil.ItemCallback<Anime>() {
    override fun areItemsTheSame(oldItem: Anime, newItem: Anime) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Anime, newItem: Anime) = oldItem == newItem
}

// Pinta el corazón: lleno y rojo si es favorito, contorno blanco si no
fun bindHeart(heart: ImageView, isFavorite: Boolean) {
    val context = heart.context
    heart.setImageResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
    heart.imageTintList = ColorStateList.valueOf(
        ContextCompat.getColor(context, if (isFavorite) R.color.heart else R.color.white)
    )
    heart.contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos"
}

// ---------- Carrusel (tarjetas tipo póster) ----------

class CarouselAdapter(
    private val showRank: Boolean,
    private val onClick: (Anime) -> Unit,
    private val onHeartClick: (Anime) -> Unit
) : ListAdapter<Anime, CarouselAdapter.Holder>(AnimeDiff) {

    // Ids de favoritos; al cambiar se redibujan los corazones
    var favoriteIds: Set<Int> = emptySet()
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imagePoster)
        val rank: TextView = view.findViewById(R.id.textRankBadge)
        val score: TextView = view.findViewById(R.id.textCarouselScore)
        val title: TextView = view.findViewById(R.id.textCarouselTitle)
        val heart: ImageView = view.findViewById(R.id.imageHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_carousel, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val anime = getItem(position)
        holder.title.text = anime.title
        holder.score.text = Translations.score(anime.score)
        holder.rank.visibility = if (showRank) View.VISIBLE else View.GONE
        holder.rank.text = "#${position + 1}"
        CoverLoader.load(holder.image, anime.hdImageUrl(), anime.title)
        bindHeart(holder.heart, anime.id in favoriteIds)
        holder.heart.setOnClickListener { onHeartClick(anime) }
        holder.itemView.setOnClickListener { onClick(anime) }
    }
}

// ---------- Fila de lista ----------

class AnimeRowHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val rank: TextView = view.findViewById(R.id.textRowRank)
    private val cover: ImageView = view.findViewById(R.id.imageRowCover)
    private val title: TextView = view.findViewById(R.id.textRowTitle)
    private val meta: TextView = view.findViewById(R.id.textRowMeta)
    private val genres: TextView = view.findViewById(R.id.textRowGenres)
    private val score: TextView = view.findViewById(R.id.textRowScore)
    private val heart: ImageView = view.findViewById(R.id.imageRowHeart)

    fun bind(
        anime: Anime,
        position: Int?,
        isFavorite: Boolean,
        onClick: (Anime) -> Unit,
        onHeartClick: (Anime) -> Unit
    ) {
        rank.visibility = if (position != null) View.VISIBLE else View.GONE
        rank.text = position?.toString()
        title.text = anime.title
        meta.text = Translations.metaLine(anime)
        genres.text = Translations.genresLine(anime)
        genres.visibility = if (genres.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        score.text = Translations.score(anime.score)
        CoverLoader.load(cover, anime.coverUrl(), anime.title)
        bindHeart(heart, isFavorite)
        heart.setOnClickListener { onHeartClick(anime) }
        itemView.setOnClickListener { onClick(anime) }
    }

    companion object {
        fun create(parent: ViewGroup) = AnimeRowHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_anime_row, parent, false)
        )
    }
}

// Lista simple (Buscar y Favoritos)
class AnimeRowAdapter(
    private val onClick: (Anime) -> Unit,
    private val onHeartClick: (Anime) -> Unit
) : ListAdapter<Anime, AnimeRowHolder>(AnimeDiff) {

    var favoriteIds: Set<Int> = emptySet()
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AnimeRowHolder.create(parent)

    override fun onBindViewHolder(holder: AnimeRowHolder, position: Int) {
        val anime = getItem(position)
        holder.bind(anime, null, anime.id in favoriteIds, onClick, onHeartClick)
    }
}

// ---------- Top ratings: podio + ranking ----------
// Dos tipos de vista: el encabezado con el podio (posiciones 1 a 3)
// y las filas normales desde la posición 4.

class TopAdapter(
    private val onClick: (Anime) -> Unit,
    private val onHeartClick: (Anime) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var ranking: List<Anime> = emptyList()

    var favoriteIds: Set<Int> = emptySet()
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    // Al cargar otra página el ranking se reordena completo (empates incluidos),
    // por eso se redibuja toda la lista.
    fun submit(newRanking: List<Anime>) {
        ranking = newRanking
        notifyDataSetChanged()
    }

    private val hasPodium get() = ranking.size >= 3

    override fun getItemCount(): Int =
        if (hasPodium) 1 + (ranking.size - 3) else ranking.size

    override fun getItemViewType(position: Int): Int =
        if (hasPodium && position == 0) TYPE_PODIUM else TYPE_ROW

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_PODIUM) {
            PodiumHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_podium, parent, false))
        } else {
            AnimeRowHolder.create(parent)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PodiumHolder) {
            holder.bind(ranking.take(3), onClick)
        } else if (holder is AnimeRowHolder) {
            val index = if (hasPodium) position + 2 else position // fila 1 = puesto 4
            val anime = ranking[index]
            holder.bind(anime, index + 1, anime.id in favoriteIds, onClick, onHeartClick)
        }
    }

    class PodiumHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val slots = listOf(
            view.findViewById<View>(R.id.podium1),
            view.findViewById<View>(R.id.podium2),
            view.findViewById<View>(R.id.podium3)
        )

        fun bind(top3: List<Anime>, onClick: (Anime) -> Unit) {
            val context = itemView.context
            val colors = listOf(R.color.gold, R.color.silver, R.color.bronze)
            val medals = listOf("🥇", "🥈", "🥉")
            val posterHeights = listOf(190, 150, 140)
            val pedestalHeights = listOf(64, 44, 30)
            val density = context.resources.displayMetrics.density

            top3.forEachIndexed { i, anime ->
                val slot = slots[i]
                val color = ContextCompat.getColor(context, colors[i])

                slot.findViewById<TextView>(R.id.textMedal).text = medals[i]
                slot.findViewById<TextView>(R.id.textPodiumTitle).text = anime.title
                slot.findViewById<TextView>(R.id.textPodiumScore).text = Translations.score(anime.score)

                val card = slot.findViewById<MaterialCardView>(R.id.cardPodium)
                card.strokeColor = color
                card.layoutParams.height = (posterHeights[i] * density).toInt()
                card.requestLayout()
                CoverLoader.load(slot.findViewById(R.id.imagePodium), anime.hdImageUrl(), anime.title)

                val pedestal = slot.findViewById<TextView>(R.id.textPedestal)
                pedestal.text = "${i + 1}"
                pedestal.backgroundTintList = ColorStateList.valueOf(color)
                pedestal.layoutParams.height = (pedestalHeights[i] * density).toInt()
                pedestal.requestLayout()

                slot.setOnClickListener { onClick(anime) }
            }
        }
    }

    companion object {
        private const val TYPE_PODIUM = 0
        private const val TYPE_ROW = 1
    }
}
