package com.pandulapeter.campfire.feature.main.history

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.model.local.HistoryItem
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.HistoryRepository
import com.pandulapeter.campfire.data.repository.PlaylistRepository
import com.pandulapeter.campfire.data.repository.SongDetailRepository
import com.pandulapeter.campfire.data.repository.SongRepository
import com.pandulapeter.campfire.feature.CampfireActivity
import com.pandulapeter.campfire.feature.main.shared.baseSongList.BaseSongListViewModel
import com.pandulapeter.campfire.feature.main.shared.recycler.viewModel.HeaderItemViewModel
import com.pandulapeter.campfire.feature.main.shared.recycler.viewModel.ItemViewModel
import com.pandulapeter.campfire.feature.main.shared.recycler.viewModel.SongItemViewModel
import com.pandulapeter.campfire.feature.shared.InteractionBlocker
import com.pandulapeter.campfire.integration.AnalyticsManager
import com.pandulapeter.campfire.util.mutableLiveDataOf
import java.util.Calendar

class HistoryViewModel(
    context: Context,
    songRepository: SongRepository,
    songDetailRepository: SongDetailRepository,
    private val historyRepository: HistoryRepository,
    preferenceDatabase: PreferenceDatabase,
    playlistRepository: PlaylistRepository,
    interactionBlocker: InteractionBlocker,
    analyticsManager: AnalyticsManager
) : BaseSongListViewModel(context, songRepository, songDetailRepository, preferenceDatabase, playlistRepository, analyticsManager, interactionBlocker),
    HistoryRepository.Subscriber {

    val shouldShowDeleteAll = mutableLiveDataOf(false)
    val shouldOpenSongs = MutableLiveData<Boolean?>()
    private var songToDeleteId: String? = null
    private var history = listOf<HistoryItem>()
    private val Calendar.year get() = get(Calendar.YEAR)
    private val Calendar.month get() = get(Calendar.MONTH)
    private val Calendar.week get() = get(Calendar.WEEK_OF_YEAR)
    private val Calendar.day get() = get(Calendar.DAY_OF_YEAR)
    override val screenName = AnalyticsManager.PARAM_VALUE_SCREEN_HISTORY
    override val placeholderText = R.string.history_placeholder
    override val buttonIcon = R.drawable.ic_songs

    init {
        buttonText.value = R.string.go_to_songs
        preferenceDatabase.lastScreen = CampfireActivity.SCREEN_HISTORY
    }

    override fun subscribe() {
        super.subscribe()
        historyRepository.subscribe(this)
    }

    override fun unsubscribe() {
        super.unsubscribe()
        historyRepository.unsubscribe(this)
    }

    override fun canUpdateUI() = historyRepository.isCacheLoaded()

    override fun Sequence<Song>.createViewModels() = filter { it.id != songToDeleteId }
        .filter { song -> history.firstOrNull { it.id == song.id } != null }
        .sortedByDescending { song -> history.first { it.id == song.id }.lastOpenedAt }
        .map { SongItemViewModel(context, songDetailRepository, playlistRepository, it) }
        .toMutableList<ItemViewModel>()
        .apply {
            val headerIndices = mutableListOf<Int>()
            val songsOnly = filterIsInstance<SongItemViewModel>().map { it.song }
            songsOnly.forEachIndexed { index, _ ->
                if (index == 0 || getHeaderTitle(index, songsOnly) != getHeaderTitle(index - 1, songsOnly)) {
                    headerIndices.add(index)
                }
            }
            (headerIndices.size - 1 downTo 0).forEach { position ->
                val index = headerIndices[position]
                add(index, HeaderItemViewModel(getHeaderTitle(index, songsOnly)))
            }
        }

    override fun onListUpdated(items: List<ItemViewModel>) {
        super.onListUpdated(items)
        shouldShowDeleteAll.value = items.isNotEmpty()
    }

    override fun onActionButtonClicked() {
        shouldOpenSongs.value = true
    }

    override fun onHistoryUpdated(history: List<HistoryItem>) {
        val oldFirst = this.history.sortedByDescending { it.lastOpenedAt }.firstOrNull()?.lastOpenedAt
        val newFirst = history.sortedByDescending { it.lastOpenedAt }.firstOrNull()?.lastOpenedAt
        val shouldScrollToTop = oldFirst != newFirst
        this.history = history.toList()
        updateAdapterItems(shouldScrollToTop)
    }

    private fun getHeaderTitle(position: Int, songs: List<Song>): Int {
        val timestamp = history.firstOrNull { it.id == songs[position].id }?.lastOpenedAt ?: 0L
        if (timestamp == 0L) {
            return 0
        }
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        if (Math.abs(now.timeInMillis - then.timeInMillis) < 30 * 60 * 1000) {
            return R.string.history_now
        }
        if (now.year == then.year && now.month == then.month && now.day == then.day) {
            return R.string.history_today
        }
        val yesterday = Calendar.getInstance().apply { timeInMillis -= 24 * 60 * 60 * 1000 }
        if (yesterday.year == then.year && yesterday.month == then.month && yesterday.day == then.day) {
            return R.string.history_yesterday
        }
        return when (Math.abs(now.year - then.year)) {
            0 -> when (Math.abs(now.month - then.month)) {
                0 -> when (Math.abs(now.week - then.week)) {
                    0 -> R.string.history_this_week
                    1 -> R.string.history_last_week
                    else -> R.string.history_this_month
                }
                1 -> R.string.history_last_month
                else -> R.string.history_this_year
            }
            1 -> R.string.history_last_year
            else -> R.string.history_a_long_time_ago
        }
    }

    fun deleteAllSongs() = historyRepository.deleteAllHistory()

    fun deleteSongTemporarily(songId: String) {
        songToDeleteId = songId
        updateAdapterItems()
    }

    fun cancelDeleteSong() {
        songToDeleteId = null
        updateAdapterItems()
    }

    fun deleteSongPermanently() {
        songToDeleteId?.let {
            historyRepository.deleteHistoryItem(it)
            songToDeleteId = null
        }
    }
}