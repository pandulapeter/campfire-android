package com.pandulapeter.campfire.data.repository

import com.pandulapeter.campfire.data.dao.SongDetailDao
import com.pandulapeter.campfire.data.database.SongDetailDatabase
import com.pandulapeter.campfire.data.model.Song
import com.pandulapeter.campfire.data.model.SongDetail
import com.pandulapeter.campfire.data.repository.shared.Repository
import com.pandulapeter.campfire.networking.NetworkManager
import com.pandulapeter.campfire.util.enqueueCall
import com.pandulapeter.campfire.util.swap
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async

class SongDetailRepository(
    private val networkManager: NetworkManager,
    private val songDetailDatabase: SongDetailDatabase
) : Repository<SongDetailRepository.Subscriber>() {
    private var isInitialized = false
    private val data = mutableListOf<SongDetailDao.SongDetailMetadata>()
    private val downloadQueue = mutableListOf<String>()

    init {
        refreshDataSet()
    }

    fun downloadSong(song: Song) {
        downloadQueue.add(song.id)
        notifyDownloadQueChanged()
        networkManager.service.getSong(song.id).enqueueCall(
            onSuccess = { songDetail ->
                songDetail.version = song.version ?: 0
                async(UI) {
                    async(CommonPool) { songDetailDatabase.songDetailDao().insert(songDetail) }.await()
                    refreshDataSet()
                    downloadQueue.remove(song.id)
                    notifyDownloadQueChanged()
                    subscribers.forEach { it.onSuccess(songDetail) }
                }
            },
            onFailure = {
                downloadQueue.remove(song.id)
                notifyDownloadQueChanged()
                subscribers.forEach { it.onError(song) }
            }
        )
    }

    fun isSongDownloading(songId: String) = downloadQueue.contains(songId)

    fun isSongDownloaded(songId: String) = data.find { it.id == songId } != null

    fun getSongVersion(songId: String) = data.find { it.id == songId }?.version

    fun deleteSong(songId: String) {
        data.swap(data.filter { it.id == songId })
        async(CommonPool) {
            songDetailDatabase.songDetailDao().delete(songId)
        }
    }

    fun deleteAllSongs() {
        data.clear()
        async(CommonPool) {
            songDetailDatabase.songDetailDao().deleteAll()
        }
    }

    private fun refreshDataSet() {
        async(UI) {
            async(CommonPool) {
                songDetailDatabase.songDetailDao().getAllMetadata()
            }.await().let {
                data.swap(it)
                isInitialized = true
            }
        }
    }

    private fun notifyDownloadQueChanged() = subscribers.forEach { it.onDownloadQueueChanged(downloadQueue) }

    interface Subscriber {

        fun onSuccess(songDetail: SongDetail)

        fun onDownloadQueueChanged(songIds: List<String>)

        fun onError(song: Song)
    }
}