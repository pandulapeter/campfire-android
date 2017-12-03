package com.pandulapeter.campfire.data.storage

import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.pandulapeter.campfire.data.model.DownloadedSong
import com.pandulapeter.campfire.data.model.Playlist
import com.pandulapeter.campfire.data.model.SongInfo


/**
 * Wrapper for storing complex data in a local database.
 *
 * TODO: This data shouldn't be stored in Shared Preferences, replace with a Room-based implementation.
 */
class DataStorageManager(context: Context, private val gson: Gson) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    /**
     * The cached list of items from the cloud.
     */
    var cloudCache: List<SongInfo>
        get() = try {
            gson.fromJson(sharedPreferences.getString(KEY_CACHE, VALUE_EMPTY_JSON_ARRAY), object : TypeToken<List<SongInfo>>() {}.type)
        } catch (_: JsonSyntaxException) {
            listOf()
        }
        set(value) {
            sharedPreferences.edit().putString(KEY_CACHE, gson.toJson(value)).apply()
        }

    /**
     * The list of downloads songs.
     */
    var downloads: List<DownloadedSong>
        get() = try {
            gson.fromJson(sharedPreferences.getString(KEY_DOWNLOADED_SONGS, VALUE_EMPTY_JSON_ARRAY), object : TypeToken<List<DownloadedSong>>() {}.type)
        } catch (_: JsonSyntaxException) {
            listOf()
        }
        set(value) {
            sharedPreferences.edit().putString(KEY_DOWNLOADED_SONGS, gson.toJson(value)).apply()
        }

    /**
     * The list of song ID-s that belong to a playlist.
     *
     * TODO: Delete this.
     */
    var favorites: List<String>
        get() = try {
            gson.fromJson(sharedPreferences.getString(KEY_FAVORITES, VALUE_EMPTY_JSON_ARRAY), object : TypeToken<List<String>>() {}.type)
        } catch (_: JsonSyntaxException) {
            listOf()
        }
        set(value) {
            sharedPreferences.edit().putString(KEY_FAVORITES, gson.toJson(value)).apply()
        }

    var playlists: List<Playlist>
        get() {
            val list: List<Playlist> = try {
                gson.fromJson(sharedPreferences.getString(KEY_PLAYLISTS, VALUE_EMPTY_JSON_ARRAY), object : TypeToken<List<String>>() {}.type)
            } catch (_: JsonSyntaxException) {
                listOf()
            }
            return if (list.isEmpty()) listOf(Playlist.Favorites(listOf())) else list
        }
        set(value) {
            sharedPreferences.edit().putString(KEY_PLAYLISTS, gson.toJson(value)).apply()
        }


    companion object {
        private const val VALUE_EMPTY_JSON_ARRAY = "[]"
        private const val KEY_CACHE = "cache"
        private const val KEY_DOWNLOADED_SONGS = "downloaded_songs"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PLAYLISTS = "playlists"
    }
}