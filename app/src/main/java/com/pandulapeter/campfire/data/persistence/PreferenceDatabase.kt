package com.pandulapeter.campfire.data.persistence

import android.content.Context
import com.pandulapeter.campfire.data.model.local.Language
import com.pandulapeter.campfire.feature.main.collections.CollectionsViewModel
import com.pandulapeter.campfire.feature.main.options.preferences.PreferencesViewModel
import com.pandulapeter.campfire.feature.main.songs.SongsViewModel
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PreferenceDatabase(context: Context) {

    companion object {
        private const val TRANSPOSITION_PREFIX = "transposition"

        fun shouldEnableGermanNotationByDefault(locale: String) = when (locale) {
            "AUT", "CZE", "DEU", "SWE", "DNK", "EST", "FIN", "HUN", "LVA", "NOR", "POL", "SRB", "SVK" -> true
            else -> false
        }

        fun getDefaultLanguageFilters(locale: String) = mutableSetOf<String>().apply {
            if (!Language.SupportedLanguages.SPANISH.countryCodes.contains(locale)) {
                add(Language.Known.Spanish.id)
            }
            if (!Language.SupportedLanguages.HUNGARIAN.countryCodes.contains(locale)) {
                add(Language.Known.Hungarian.id)
            }
            if (!Language.SupportedLanguages.ROMANIAN.countryCodes.contains(locale)) {
                add(Language.Known.Romanian.id)
            }
        }
    }

    private val preferences = context.applicationContext.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private val locale by lazy { Locale.getDefault().isO3Country.toUpperCase() }

    // General
    var lastScreen by PreferenceFieldDelegate.String("lastScreen", "")
    var disabledLanguageFilters by PreferenceFieldDelegate.StringSet("disabledLanguageFilters", getDefaultLanguageFilters(locale))
    var shouldShowExplicit by PreferenceFieldDelegate.Boolean("shouldShowExplicit")

    // Home
    var isOnboardingDone by PreferenceFieldDelegate.Boolean("isOnboardingDone")
    var shouldShowSongOfTheDay by PreferenceFieldDelegate.Boolean("shouldShowSongOfTheDay", true)
    var shouldShowNewCollections by PreferenceFieldDelegate.Boolean("shouldShowNewCollections", true)
    var shouldShowNewSongs by PreferenceFieldDelegate.Boolean("shouldShowNewSongs", true)
    var shouldShowRandomCollections by PreferenceFieldDelegate.Boolean("shouldShowRandomCollections", true)
    var shouldShowRandomSongs by PreferenceFieldDelegate.Boolean("shouldShowRandomSongs", true)
    var isSearchInSongsEnabled by PreferenceFieldDelegate.Boolean("isSearchInSongsEnabled", true)
    var isSearchInCollectionsEnabled by PreferenceFieldDelegate.Boolean("isSearchInCollectionsEnabled", true)

    // Collections
    var lastCollectionsUpdateTimestamp by PreferenceFieldDelegate.Long("lastCollectionsUpdateTimestamp")
    var collectionsSortingMode by PreferenceFieldDelegate.Int("collectionsSortingMode", CollectionsViewModel.SortingMode.UPLOAD_DATE.intValue)
    var shouldShowSavedOnly by PreferenceFieldDelegate.Boolean("shouldShowSavedOnly")
    var shouldSearchInCollectionTitles by PreferenceFieldDelegate.Boolean("shouldSearchInCollectionTitles", true)
    var shouldSearchInCollectionDescriptions by PreferenceFieldDelegate.Boolean("shouldSearchInCollectionDescriptions", true)

    // Songs
    var lastSongsUpdateTimestamp by PreferenceFieldDelegate.Long("lastSongsUpdateTimestamp")
    var songsSortingMode by PreferenceFieldDelegate.Int("songsSortingMode", SongsViewModel.SortingMode.TITLE.intValue)
    var shouldSearchInTitles by PreferenceFieldDelegate.Boolean("shouldSearchInTitles", true)
    var shouldSearchInArtists by PreferenceFieldDelegate.Boolean("shouldSearchInArtists", true)
    var shouldShowDownloadedOnly by PreferenceFieldDelegate.Boolean("shouldShowDownloadedOnly")

    // Preferences
    var shouldShowChords by PreferenceFieldDelegate.Boolean("shouldShowChords", true)
    var shouldUseGermanNotation by PreferenceFieldDelegate.Boolean("shouldUseGermanNotation", shouldEnableGermanNotationByDefault(locale))
    var fontSize by PreferenceFieldDelegate.Float("fontSize", 1f)
    var theme by PreferenceFieldDelegate.Int("theme", PreferencesViewModel.Theme.AUTOMATIC.id)
    var language by PreferenceFieldDelegate.String("language", PreferencesViewModel.Language.AUTOMATIC.id)
    var shouldShowExitConfirmation by PreferenceFieldDelegate.Boolean("shouldShowExitConfirmation", true)
    var shouldShareCrashReports by PreferenceFieldDelegate.Boolean("shouldShareCrashReports")
    var shouldShareUsageData by PreferenceFieldDelegate.Boolean("shouldShareUsageData")
    var playlistHistory by PreferenceFieldDelegate.StringSet("playlistHistory", setOf())

    // First time user experience
    var ftuxLastSeenChangelog by PreferenceFieldDelegate.Int("ftuxLastSeenChangelog")
    var ftuxHistoryCompleted by PreferenceFieldDelegate.Boolean("ftuxHistoryCompleted")
    var ftuxPlaylistSwipeCompleted by PreferenceFieldDelegate.Boolean("ftuxPlaylistSwipeCompleted")
    var ftuxPlaylistDragCompleted by PreferenceFieldDelegate.Boolean("ftuxPlaylistDragCompleted")
    var ftuxManagePlaylistsSwipeCompleted by PreferenceFieldDelegate.Boolean("ftuxManagePlaylistsSwipeCompleted")
    var ftuxManagePlaylistsDragCompleted by PreferenceFieldDelegate.Boolean("ftuxManagePlaylistsDragCompleted")
    var ftuxManageDownloadsCompleted by PreferenceFieldDelegate.Boolean("ftuxManageDownloadsCompleted")
    var ftuxPlaylistPagerSwipeCompleted by PreferenceFieldDelegate.Boolean("ftuxPlaylistPagerSwipeCompleted")
    var fontSizePinchCompleted by PreferenceFieldDelegate.Boolean("fontSizePinchCompleted")
    var songsOpened by PreferenceFieldDelegate.Int("songsOpened")
    var ratingDialogShown by PreferenceFieldDelegate.Boolean("ratingDialogShown")

    // Song transposition values
    fun getTransposition(songId: String) = preferences.getInt(TRANSPOSITION_PREFIX + songId, 0)

    fun setTransposition(songId: String, transposition: Int) = preferences.edit().putInt(TRANSPOSITION_PREFIX + songId, transposition).apply()

    private sealed class PreferenceFieldDelegate<T>(protected val key: kotlin.String, protected val defaultValue: T) : ReadWriteProperty<PreferenceDatabase, T> {

        class Boolean(key: kotlin.String, defaultValue: kotlin.Boolean = false) : PreferenceFieldDelegate<kotlin.Boolean>(key, defaultValue) {

            override fun getValue(thisRef: PreferenceDatabase, property: KProperty<*>) = thisRef.preferences.getBoolean(key, defaultValue)

            override fun setValue(thisRef: PreferenceDatabase, property: KProperty<*>, value: kotlin.Boolean) =
                thisRef.preferences.edit().putBoolean(key, value).apply()
        }

        class Int(key: kotlin.String, defaultValue: kotlin.Int = 0) : PreferenceFieldDelegate<kotlin.Int>(key, defaultValue) {

            override fun getValue(thisRef: PreferenceDatabase, property: KProperty<*>) = thisRef.preferences.getInt(key, defaultValue)

            override fun setValue(thisRef: PreferenceDatabase, property: KProperty<*>, value: kotlin.Int) =
                thisRef.preferences.edit().putInt(key, value).apply()
        }

        class Float(key: kotlin.String, defaultValue: kotlin.Float = 0f) : PreferenceFieldDelegate<kotlin.Float>(key, defaultValue) {

            override fun getValue(thisRef: PreferenceDatabase, property: KProperty<*>) = thisRef.preferences.getFloat(key, defaultValue)

            override fun setValue(thisRef: PreferenceDatabase, property: KProperty<*>, value: kotlin.Float) =
                thisRef.preferences.edit().putFloat(key, value).apply()
        }

        class Long(key: kotlin.String, defaultValue: kotlin.Long = 0) : PreferenceFieldDelegate<kotlin.Long>(key, defaultValue) {

            override fun getValue(thisRef: PreferenceDatabase, property: KProperty<*>) = thisRef.preferences.getLong(key, defaultValue)

            override fun setValue(thisRef: PreferenceDatabase, property: KProperty<*>, value: kotlin.Long) =
                thisRef.preferences.edit().putLong(key, value).apply()
        }

        class String(key: kotlin.String, defaultValue: kotlin.String = "") : PreferenceFieldDelegate<kotlin.String>(key, defaultValue) {

            override fun getValue(thisRef: PreferenceDatabase, property: KProperty<*>) = thisRef.preferences.getString(key, defaultValue) ?: defaultValue

            override fun setValue(thisRef: PreferenceDatabase, property: KProperty<*>, value: kotlin.String) =
                thisRef.preferences.edit().putString(key, value).apply()
        }

        class StringSet(key: kotlin.String, defaultValue: Set<kotlin.String>) : PreferenceFieldDelegate<Set<kotlin.String>>(key, defaultValue) {

            override fun getValue(thisRef: PreferenceDatabase, property: KProperty<*>): Set<kotlin.String> = thisRef.preferences.getStringSet(key, defaultValue) ?: setOf()

            override fun setValue(thisRef: PreferenceDatabase, property: KProperty<*>, value: Set<kotlin.String>) =
                thisRef.preferences.edit().putStringSet(key, value).apply()
        }
    }
}