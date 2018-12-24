package com.pandulapeter.campfire.feature.main.shared.baseSongList

import android.content.Context
import androidx.annotation.CallSuper
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.model.local.Playlist
import com.pandulapeter.campfire.data.model.local.SongDetailMetadata
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.model.remote.SongDetail
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.PlaylistRepository
import com.pandulapeter.campfire.data.repository.SongDetailRepository
import com.pandulapeter.campfire.data.repository.SongRepository
import com.pandulapeter.campfire.feature.main.shared.recycler.RecyclerAdapter
import com.pandulapeter.campfire.feature.main.shared.recycler.viewModel.CollectionItemViewModel
import com.pandulapeter.campfire.feature.main.shared.recycler.viewModel.ItemViewModel
import com.pandulapeter.campfire.feature.main.shared.recycler.viewModel.SongItemViewModel
import com.pandulapeter.campfire.feature.shared.deprecated.OldCampfireViewModel
import com.pandulapeter.campfire.feature.shared.widget.StateLayout
import com.pandulapeter.campfire.integration.AnalyticsManager
import com.pandulapeter.campfire.util.UI
import com.pandulapeter.campfire.util.WORKER
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@Deprecated("Extend from BaseSongListViewModel instead.")
abstract class OldBaseSongListViewModel(
    protected val context: Context,
    protected val songRepository: SongRepository,
    protected val songDetailRepository: SongDetailRepository,
    protected val preferenceDatabase: PreferenceDatabase,
    protected val playlistRepository: PlaylistRepository,
    protected val analyticsManager: AnalyticsManager
) : OldCampfireViewModel(), SongRepository.Subscriber, SongDetailRepository.Subscriber,
    PlaylistRepository.Subscriber {

    val isSwipeRefreshEnabled = ObservableBoolean(true)
    abstract val screenName: String
    private var coroutine: CoroutineContext? = null
    protected var songs = sequenceOf<Song>()
    val collection = ObservableField<CollectionItemViewModel?>()
    val adapter = RecyclerAdapter()
    val shouldShowUpdateErrorSnackbar = ObservableBoolean()
    val downloadSongError = ObservableField<Song?>()
    val isLoading = ObservableBoolean()
    val state = ObservableField<StateLayout.State>(StateLayout.State.LOADING)
    open val placeholderText = R.string.campfire
    val buttonText = MutableLiveData<Int>()
    open val buttonIcon = 0
    var isDetailScreenOpen = false
    open val cardTransitionName = ""
    open val imageTransitionName = ""

    @CallSuper
    override fun subscribe() {
        songRepository.subscribe(this)
        songDetailRepository.subscribe(this)
        playlistRepository.subscribe(this)
        isDetailScreenOpen = false
    }

    @CallSuper
    override fun unsubscribe() {
        songRepository.unsubscribe(this)
        songDetailRepository.unsubscribe(this)
        playlistRepository.unsubscribe(this)
    }

    @CallSuper
    override fun onSongRepositoryDataUpdated(data: List<Song>) {
        songs = data.asSequence()
        updateAdapterItems(true)
    }

    override fun onSongRepositoryLoadingStateChanged(isLoading: Boolean) {
        this.isLoading.set(isLoading)
        if (songs.toList().isEmpty() && isLoading) {
            state.set(StateLayout.State.LOADING)
        }
    }

    override fun onSongRepositoryUpdateError() {
        if (songs.toList().isEmpty()) {
            analyticsManager.onConnectionError(true, screenName)
            state.set(StateLayout.State.ERROR)
        } else {
            analyticsManager.onConnectionError(false, screenName)
            shouldShowUpdateErrorSnackbar.set(true)
        }
    }

    override fun onSongDetailRepositoryUpdated(downloadedSongs: List<SongDetailMetadata>) {
        if (songs.toList().isNotEmpty()) {
            updateAdapterItems()
        }
    }

    override fun onSongDetailRepositoryDownloadSuccess(songDetail: SongDetail) {
        adapter.items.indexOfLast { it is SongItemViewModel && it.song.id == songDetail.id }.let { index ->
            if (index != RecyclerView.NO_POSITION && (adapter.items[index] as? SongItemViewModel)?.song?.version == songDetail.version) {
                adapter.notifyItemChanged(
                    index,
                    RecyclerAdapter.Payload.DownloadStateChanged(SongItemViewModel.DownloadState.Downloaded.UpToDate)
                )
            }
        }
    }

    override fun onSongDetailRepositoryDownloadQueueChanged(songIds: List<String>) {
        songIds.forEach { songId ->
            adapter.items.indexOfLast { it is SongItemViewModel && it.song.id == songId }.let { index ->
                if (index != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(
                        index,
                        RecyclerAdapter.Payload.DownloadStateChanged(SongItemViewModel.DownloadState.Downloading)
                    )
                }
            }
        }
    }

    override fun onSongDetailRepositoryDownloadError(song: Song) {
        analyticsManager.onConnectionError(!songDetailRepository.isSongDownloaded(song.id), song.id)
        downloadSongError.set(song)
        adapter.items.indexOfLast { it is SongItemViewModel && it.song.id == song.id }.let { index ->
            if (index != RecyclerView.NO_POSITION) {
                adapter.notifyItemChanged(
                    index,
                    RecyclerAdapter.Payload.DownloadStateChanged(
                        when {
                            songDetailRepository.isSongDownloaded(song.id) -> SongItemViewModel.DownloadState.Downloaded.Deprecated
                            song.isNew -> SongItemViewModel.DownloadState.NotDownloaded.New
                            else -> SongItemViewModel.DownloadState.NotDownloaded.Old
                        }
                    )
                )
            }
        }
    }

    override fun onPlaylistsUpdated(playlists: List<Playlist>) {
        if (songs.toList().isNotEmpty()) {
            updateAdapterItems()
        }
    }

    override fun onSongAddedToPlaylistForTheFirstTime(songId: String) {
        adapter.items.indexOfLast { it is SongItemViewModel && it.song.id == songId }.let { index ->
            if (index != RecyclerView.NO_POSITION) {
                adapter.notifyItemChanged(
                    index,
                    RecyclerAdapter.Payload.IsSongInAPlaylistChanged(true)
                )
            }
        }
    }

    override fun onSongRemovedFromAllPlaylists(songId: String) {
        adapter.items.indexOfLast { it is SongItemViewModel && it.song.id == songId }.let { index ->
            if (index != RecyclerView.NO_POSITION) {
                adapter.notifyItemChanged(
                    index,
                    RecyclerAdapter.Payload.IsSongInAPlaylistChanged(false)
                )
            }
        }
    }

    override fun onPlaylistOrderChanged(playlists: List<Playlist>) = Unit

    abstract fun onActionButtonClicked()

    fun updateData() = songRepository.updateData()

    fun downloadSong(song: Song) = songDetailRepository.getSongDetail(song, true)

    fun areThereMoreThanOnePlaylists() = playlistRepository.cache.size > 1

    fun toggleFavoritesState(songId: String) {
        if (playlistRepository.isSongInPlaylist(Playlist.FAVORITES_ID, songId)) {
            analyticsManager.onSongPlaylistStateChanged(
                songId,
                playlistRepository.getPlaylistCountForSong(songId) - 1,
                screenName,
                playlistRepository.cache.size > 1
            )
            playlistRepository.removeSongFromPlaylist(Playlist.FAVORITES_ID, songId)
        } else {
            analyticsManager.onSongPlaylistStateChanged(
                songId,
                playlistRepository.getPlaylistCountForSong(songId) + 1,
                screenName,
                playlistRepository.cache.size > 1
            )
            playlistRepository.addSongToPlaylist(Playlist.FAVORITES_ID, songId)
        }
    }

    protected open fun canUpdateUI() = true

    protected abstract fun Sequence<Song>.createViewModels(): List<ItemViewModel>

    @CallSuper
    protected open fun onListUpdated(items: List<ItemViewModel>) {
        state.set(if (items.isEmpty()) StateLayout.State.ERROR else StateLayout.State.NORMAL)
    }

    protected fun updateAdapterItems(shouldScrollToTop: Boolean = false) {
        if (canUpdateUI() && playlistRepository.isCacheLoaded() && songRepository.isCacheLoaded() && songDetailRepository.isCacheLoaded()) {
            coroutine?.cancel()
            coroutine = GlobalScope.launch(UI) {
                async(WORKER) { songs.createViewModels() }.await().let {
                    adapter.shouldScrollToTop = shouldScrollToTop
                    adapter.items = it
                    onListUpdated(it)
                }
                coroutine = null
            }
        }
    }
}