package com.pandulapeter.campfire.feature.main.songs

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.annotation.IdRes
import androidx.databinding.DataBindingUtil
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.PlaylistRepository
import com.pandulapeter.campfire.data.repository.SongDetailRepository
import com.pandulapeter.campfire.data.repository.SongRepository
import com.pandulapeter.campfire.databinding.ViewSearchControlsBinding
import com.pandulapeter.campfire.feature.main.shared.baseSongList.OldBaseSongListFragment
import com.pandulapeter.campfire.feature.shared.widget.ToolbarButton
import com.pandulapeter.campfire.feature.shared.widget.ToolbarTextInputView
import com.pandulapeter.campfire.integration.AnalyticsManager
import com.pandulapeter.campfire.util.BundleArgumentDelegate
import com.pandulapeter.campfire.util.animatedDrawable
import com.pandulapeter.campfire.util.consume
import com.pandulapeter.campfire.util.drawable
import com.pandulapeter.campfire.util.onPropertyChanged
import org.koin.android.ext.android.inject


class SongsFragment : OldBaseSongListFragment<SongsViewModel>() {

    private val songRepository by inject<SongRepository>()
    private val songDetailRepository by inject<SongDetailRepository>()
    private val preferenceDatabase by inject<PreferenceDatabase>()
    private val playlistRepository by inject<PlaylistRepository>()

    override val viewModel: SongsViewModel by lazy {
        SongsViewModel(
            context = getCampfireActivity(),
            songRepository = songRepository,
            songDetailRepository = songDetailRepository,
            preferenceDatabase = preferenceDatabase,
            playlistRepository = playlistRepository,
            analyticsManager = analyticsManager,
            toolbarTextInputView = ToolbarTextInputView(getCampfireActivity().toolbarContext, R.string.songs_search, true).apply { title.updateToolbarTitle(R.string.main_songs) }
        )
    }
    private var Bundle.isTextInputVisible by BundleArgumentDelegate.Boolean("isTextInputVisible")
    private var Bundle.searchQuery by BundleArgumentDelegate.String("searchQuery")
    private var Bundle.isEraseButtonVisible by BundleArgumentDelegate.Boolean("isEraseButtonVisible")
    private var Bundle.isEraseButtonEnabled by BundleArgumentDelegate.Boolean("isEraseButtonEnabled")
    private val searchToggle: ToolbarButton by lazy {
        getCampfireActivity().toolbarContext.createToolbarButton(R.drawable.ic_search_24dp) {
            viewModel.toggleTextInputVisibility()
        }
    }
    private val eraseButton: ToolbarButton by lazy {
        getCampfireActivity().toolbarContext.createToolbarButton(R.drawable.ic_eraser_24dp) {
            viewModel.toolbarTextInputView.textInput.setText("")
        }.apply {
            scaleX = 0f
            scaleY = 0f
            alpha = 0.5f
            isEnabled = false
        }
    }
    private val drawableCloseToSearch by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getCampfireActivity().animatedDrawable(R.drawable.avd_close_to_search_24dp) else getCampfireActivity().drawable(R.drawable.ic_search_24dp)
    }
    private val drawableSearchToClose by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getCampfireActivity().animatedDrawable(R.drawable.avd_search_to_close_24dp) else getCampfireActivity().drawable(R.drawable.ic_close_24dp)
    }
    private val searchControlsViewModel = SearchControlsViewModel(false)
    private val searchControlsBinding by lazy {
        DataBindingUtil.inflate<ViewSearchControlsBinding>(LayoutInflater.from(getCampfireActivity().toolbarContext), R.layout.view_search_controls, null, false).apply {
            viewModel = searchControlsViewModel
            executePendingBindings()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyticsManager.onTopLevelScreenOpened(AnalyticsManager.PARAM_VALUE_SCREEN_SONGS)
        viewModel.shouldUpdateSearchToggleDrawable.observeAndReset {
            searchToggle.setImageDrawable((if (it) drawableSearchToClose else drawableCloseToSearch).apply { (this as? AnimatedVectorDrawableCompat)?.start() })
            getCampfireActivity().transitionMode = true
            binding.root.post {
                if (isAdded) {
                    searchControlsViewModel.isVisible.set(it)
                }
            }
        }
        viewModel.languages.observe { languages ->
            if (languages != null) {
                getCampfireActivity().updateAppBarView(searchControlsBinding.root)
                getCampfireActivity().enableSecondaryNavigationDrawer(R.menu.songs)
                initializeCompoundButton(R.id.downloaded_only) { viewModel.shouldShowDownloadedOnly }
                initializeCompoundButton(R.id.show_explicit) { viewModel.shouldShowExplicit }
                initializeCompoundButton(R.id.sort_by_title) { viewModel.sortingMode == SongsViewModel.SortingMode.TITLE }
                initializeCompoundButton(R.id.sort_by_artist) { viewModel.sortingMode == SongsViewModel.SortingMode.ARTIST }
                initializeCompoundButton(R.id.sort_by_popularity) { viewModel.sortingMode == SongsViewModel.SortingMode.POPULARITY }
                getCampfireActivity().secondaryNavigationMenu.findItem(R.id.filter_by_language).subMenu.run {
                    clear()
                    languages.forEachIndexed { index, language ->
                        add(R.id.language_container, language.nameResource, index, language.nameResource).apply {
                            setActionView(R.layout.widget_checkbox)
                            initializeCompoundButton(language.nameResource) { !viewModel.disabledLanguageFilters.contains(language.id) }
                        }
                    }
                }
                getCampfireActivity().toolbarContext.let { context ->
                    getCampfireActivity().updateToolbarButtons(
                        listOf(
                            eraseButton,
                            searchToggle,
                            context.createToolbarButton(R.drawable.ic_filter_and_sort_24dp) { getCampfireActivity().openSecondaryNavigationDrawer() }
                        ))
                }
            }
        }
        viewModel.isFastScrollEnabled.observe { binding.recyclerView.setFastScrollEnabled(it) }
        viewModel.shouldOpenSecondaryNavigationDrawer.observeAndReset { getCampfireActivity().openSecondaryNavigationDrawer() }
        viewModel.shouldShowEraseButton.observe { eraseButton.animate().scaleX(if (it) 1f else 0f).scaleY(if (it) 1f else 0f).start() }
        viewModel.shouldEnableEraseButton.observe {
            eraseButton.animate().alpha(if (it) 1f else 0.5f).start()
            eraseButton.isEnabled = it
        }
        savedInstanceState?.let {
            searchControlsViewModel.isVisible.set(savedInstanceState.isTextInputVisible)
            if (it.isTextInputVisible) {
                searchToggle.setImageDrawable(getCampfireActivity().drawable(R.drawable.ic_close_24dp))
                viewModel.toolbarTextInputView.textInput.run {
                    setText(savedInstanceState.searchQuery)
                    setSelection(text.length)
                    viewModel.query = text.toString()
                }
                viewModel.toolbarTextInputView.showTextInput()
            }
            viewModel.shouldShowEraseButton.value = savedInstanceState.isEraseButtonVisible
            viewModel.shouldEnableEraseButton.value = savedInstanceState.isEraseButtonEnabled
        }
        viewModel.toolbarTextInputView.textInput.requestFocus()
        searchControlsViewModel.searchInTitles.onPropertyChanged(this) {
            binding.root.postDelayed(
                { if (isAdded) viewModel.shouldSearchInTitles = it },
                COMPOUND_BUTTON_LONG_TRANSITION_DELAY
            )
        }
        searchControlsViewModel.searchInArtists.onPropertyChanged(this) {
            binding.root.postDelayed(
                { if (isAdded) viewModel.shouldSearchInArtists = it },
                COMPOUND_BUTTON_LONG_TRANSITION_DELAY
            )
        }
        getCampfireActivity().showPlayStoreRatingDialogIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.restoreToolbarButtons()
    }

    override fun onPause() {
        super.onPause()
        toolbarWidth = viewModel.toolbarTextInputView.width
    }

    override fun onSaveInstanceState(outState: Bundle) = outState.run {
        super.onSaveInstanceState(this)
        isTextInputVisible = viewModel.toolbarTextInputView.isTextInputVisible
        searchQuery = viewModel.query
        isEraseButtonVisible = viewModel.shouldShowEraseButton.value == true
        isEraseButtonEnabled = viewModel.shouldEnableEraseButton.value == true
    }

    override fun onNavigationItemSelected(menuItem: MenuItem) = viewModel.run {
        when (menuItem.itemId) {
            R.id.downloaded_only -> consumeAndUpdateBoolean(menuItem, {
                analyticsManager.onSongsFilterToggled(AnalyticsManager.PARAM_VALUE_FILTER_DOWNLOADED_ONLY, it)
                shouldShowDownloadedOnly = it
            }, { shouldShowDownloadedOnly })
            R.id.show_explicit -> consumeAndUpdateBoolean(menuItem, {
                analyticsManager.onSongsFilterToggled(AnalyticsManager.PARAM_VALUE_FILTER_SHOW_EXPLICIT, it)
                shouldShowExplicit = it
            }, { shouldShowExplicit })
            R.id.sort_by_title -> consumeAndUpdateSortingMode(SongsViewModel.SortingMode.TITLE) {
                analyticsManager.onSongsSortingModeUpdated(AnalyticsManager.PARAM_VALUE_BY_TITLE)
                sortingMode = it
            }
            R.id.sort_by_artist -> consumeAndUpdateSortingMode(SongsViewModel.SortingMode.ARTIST) {
                analyticsManager.onSongsSortingModeUpdated(AnalyticsManager.PARAM_VALUE_BY_ARTIST)
                sortingMode = it
            }
            R.id.sort_by_popularity -> consumeAndUpdateSortingMode(SongsViewModel.SortingMode.POPULARITY) {
                analyticsManager.onSongsSortingModeUpdated(AnalyticsManager.PARAM_VALUE_BY_POPULARITY)
                sortingMode = it
            }
            else -> consumeAndUpdateLanguageFilter(menuItem, viewModel.languages.value.orEmpty().find { it.nameResource == menuItem.itemId }?.id ?: "")
        }
    }

    override fun inflateToolbarTitle(context: Context) = viewModel.toolbarTextInputView

    override fun onBackPressed() = if (viewModel.toolbarTextInputView.isTextInputVisible) {
        viewModel.toggleTextInputVisibility()
        true
    } else super.onBackPressed()

    override fun onDetailScreenOpened() {
        if (viewModel.toolbarTextInputView.isTextInputVisible && viewModel.query.trim().isEmpty()) {
            viewModel.toggleTextInputVisibility()
        }
    }

    private fun consumeAndUpdateLanguageFilter(menuItem: MenuItem, languageId: String) = consume {
        viewModel.disabledLanguageFilters.run {
            viewModel.disabledLanguageFilters = toMutableSet().apply { if (contains(languageId)) remove(languageId) else add(languageId) }
            analyticsManager.onSongsFilterToggled(AnalyticsManager.PARAM_VALUE_FILTER_LANGUAGE + languageId, contains(languageId))
            (menuItem.actionView as? CompoundButton).updateCheckedStateWithDelay(contains(languageId))
        }
    }

    private inline fun consumeAndUpdateSortingMode(sortingMode: SongsViewModel.SortingMode, crossinline setValue: (SongsViewModel.SortingMode) -> Unit) = consume {
        setValue(sortingMode)
        (getCampfireActivity().secondaryNavigationMenu[R.id.sort_by_title].actionView as? CompoundButton).updateCheckedStateWithDelay(sortingMode == SongsViewModel.SortingMode.TITLE)
        (getCampfireActivity().secondaryNavigationMenu[R.id.sort_by_artist].actionView as? CompoundButton).updateCheckedStateWithDelay(sortingMode == SongsViewModel.SortingMode.ARTIST)
        (getCampfireActivity().secondaryNavigationMenu[R.id.sort_by_popularity].actionView as? CompoundButton)?.updateCheckedStateWithDelay(sortingMode == SongsViewModel.SortingMode.POPULARITY)
    }

    private operator fun Menu.get(@IdRes id: Int) = findItem(id)

    companion object {
        const val COMPOUND_BUTTON_LONG_TRANSITION_DELAY = 300L
    }
}