package com.pandulapeter.campfire.old.feature.home.shared.songInfoList

import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.old.data.repository.DownloadedSongRepository
import com.pandulapeter.campfire.old.data.repository.PlaylistRepository
import com.pandulapeter.campfire.old.data.repository.SongInfoRepository
import com.pandulapeter.campfire.old.data.repository.UserPreferenceRepository
import com.pandulapeter.campfire.old.feature.home.shared.SpacesItemDecoration
import com.pandulapeter.campfire.old.feature.home.shared.homeChild.HomeChildFragment
import com.pandulapeter.campfire.util.dimension
import com.pandulapeter.campfire.util.hideKeyboard
import com.pandulapeter.campfire.util.onEventTriggered
import org.koin.android.ext.android.inject

/**
 * Displays the list of all available songs from the backend. The list is searchable and filterable
 * and contains headers. The list is also cached locally and automatically updated after a period or
 * manually using the pull-to-refresh gesture.
 *
 * Controlled by subclasses of [SongInfoListViewModel].
 */
abstract class SongInfoListFragment<B : ViewDataBinding, out VM : SongInfoListViewModel>(@LayoutRes layoutResourceId: Int) :
    HomeChildFragment<B, VM>(layoutResourceId) {
    protected val userPreferenceRepository by inject<UserPreferenceRepository>()
    protected val songInfoRepository by inject<SongInfoRepository>()
    protected val downloadedSongRepository by inject<DownloadedSongRepository>()
    protected val playlistRepository by inject<PlaylistRepository>()

    protected abstract fun getRecyclerView(): RecyclerView

    protected abstract fun getCoordinatorLayout(): CoordinatorLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let { context ->
            // Initialize the list.
            getRecyclerView().run {
                adapter = viewModel.adapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                addItemDecoration(SpacesItemDecoration(context.dimension(R.dimen.content_padding)))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                        if (dy > 0) {
                            hideKeyboard(activity?.currentFocus)
                        }
                    }
                })
            }
            // Display error snackbar with Retry action if the download fails.
            viewModel.shouldShowDownloadErrorSnackbar.onEventTriggered(this) {
                it?.let { songInfo ->
                    if (isAdded) {
                        getCoordinatorLayout().showSnackbar(message = getString(R.string.song_item_song_download_failed, songInfo.title),
                            actionButton = R.string.try_again,
                            action = { viewModel.downloadSong(songInfo) })
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        userPreferenceRepository.subscribe(viewModel)
        songInfoRepository.subscribe(viewModel)
        downloadedSongRepository.subscribe(viewModel)
        playlistRepository.subscribe(viewModel)
    }

    override fun onStop() {
        super.onStop()
        userPreferenceRepository.unsubscribe(viewModel)
        songInfoRepository.unsubscribe(viewModel)
        downloadedSongRepository.unsubscribe(viewModel)
        playlistRepository.unsubscribe(viewModel)
    }
}