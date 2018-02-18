package com.pandulapeter.campfire.feature.home.collections

import com.pandulapeter.campfire.CollectionsBinding
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.repository.UserPreferenceRepository
import com.pandulapeter.campfire.feature.home.shared.homeChild.HomeChildFragment
import com.pandulapeter.campfire.integration.AppShortcutManager
import org.koin.android.ext.android.inject

/**
 * Displays a list of curated song collections.
 *
 * Controlled by [CollectionsViewModel].
 */
class CollectionsFragment : HomeChildFragment<CollectionsBinding, CollectionsViewModel>(R.layout.fragment_collections) {
    private val appShortcutManager by inject<AppShortcutManager>()
    private val userPrefRepository by inject<UserPreferenceRepository>()

    override fun createViewModel() = CollectionsViewModel(analyticsManager, appShortcutManager, userPrefRepository)

    override fun getAppBarLayout() = binding.appBarLayout
}