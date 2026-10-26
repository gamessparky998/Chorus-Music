package pushkar.chorus.music.lyrics

import android.content.Context
import android.util.LruCache
import pushkar.chorus.music.constants.LyricsProviderOrderKey
import pushkar.chorus.music.constants.PreferredLyricsProvider
import pushkar.chorus.music.constants.PreferredLyricsProviderKey
import pushkar.chorus.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import pushkar.chorus.music.extensions.toEnum
import pushkar.chorus.music.models.MediaMetadata
import pushkar.chorus.music.utils.NetworkConnectivityObserver
import pushkar.chorus.music.utils.dataStore
import pushkar.chorus.music.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {

    private suspend fun resolveLyricsProviders(): List<LyricsProvider> {
        val preferences = context.dataStore.data.first()
        val orderString = preferences[LyricsProviderOrderKey].orEmpty()

        if (orderString.isNotBlank()) {
            return LyricsProviderRegistry.getOrderedProviders(orderString)
        }

        val preferredEnum = preferences[PreferredLyricsProviderKey]
            .toEnum(PreferredLyricsProvider.YOULYPLUS)
        val preferredName = LyricsProviderRegistry.getProviderNameForEnum(preferredEnum)
        val defaultOrder = LyricsProviderRegistry.getDefaultProviderOrder()
        val migratedOrder = listOf(preferredName) + defaultOrder.filter { it != preferredName }
        return migratedOrder.mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
    }

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(
        mediaMetadata: MediaMetadata,
        initialLyrics: LyricsWithProvider? = null,
        onUpgrade: suspend (LyricsWithProvider) -> Unit = {},
    ): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val isLocal = mediaMetadata.id.startsWith("content://")
        val cacheKey = listOf(
            mediaMetadata.id,
            mediaMetadata.artists.joinToString { it.name },
            mediaMetadata.title,
            mediaMetadata.duration.toString(),
            mediaMetadata.album?.title.orEmpty()
        ).joinToString("|")

        val cached = cache.get(cacheKey)?.firstOrNull()
        if (cached != null && cached.lyrics != LYRICS_NOT_FOUND) {
            return LyricsWithProvider(cached.lyrics, cached.providerName)
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val providers = resolveLyricsProviders()
            .let { if (isLocal) it.filterNot { p -> p.name == "YouTube Music" || p.name == "YouTube Subtitle" } else it }
            .filter { it.isEnabled(context) }
        if (providers.isEmpty()) return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")

        return coroutineScope {
            val policy = LyricsUpgradePolicy()
            initialLyrics?.takeIf { initial -> providers.any { it.name == initial.provider } }?.let {
                policy.consider(it.provider, providers.indexOfFirst { p -> p.name == it.provider }, it.lyrics)
            }
            val remaining = providers.indices.toMutableList()
            val fallback = providers.indexOfFirst { it.name == "LrcLib" }
            if (fallback > 2) {
                remaining.remove(fallback)
                remaining.add(2, fallback)
            }

            val answers = Channel<Pair<Int, Result<String>>>(Channel.UNLIMITED)
            val jobs = mutableListOf<Job>()
            val completed = hashSetOf<Int>()
            var inFlight = 0

            val cleanTitle = pushkar.chorus.music.lyrics.LyricsUtils.cleanSongTitleForLyrics(mediaMetadata.title)
            val artistName = mediaMetadata.artists.joinToString { it.name }

            fun fillWindow() {
                while (inFlight < 3 && remaining.isNotEmpty()) {
                    val bestWord = policy.best?.takeIf { it.sync == LyricsSyncLevel.WORD }
                    val next = if (bestWord == null) 0 else remaining.indexOfFirst { it < bestWord.priority }
                    if (next < 0) break
                    val index = remaining.removeAt(next)
                    inFlight++
                    jobs += launch(Dispatchers.IO) {
                        try {
                            val result = providers[index].getLyrics(
                                mediaMetadata.id,
                                cleanTitle,
                                artistName,
                                mediaMetadata.duration,
                                mediaMetadata.album?.title,
                            )
                            answers.send(index to result)
                        } catch (e: Exception) {
                            answers.send(index to Result.failure(e))
                        }
                    }
                }
            }

            val deadline = System.nanoTime() / 1_000_000 + 20_000L
            try {
                fillWindow()
                while (inFlight > 0) {
                    val timeout = deadline - System.nanoTime() / 1_000_000
                    if (timeout <= 0) break
                    val answer = withTimeoutOrNull(timeout) { answers.receive() } ?: break
                    val (index, result) = answer
                    inFlight--
                    completed += index
                    val provider = providers[index]
                    result.getOrNull()?.takeIf {
                        it.isNotBlank() && it != LYRICS_NOT_FOUND &&
                                provider.isEnabled(context) &&
                                pushkar.chorus.music.lyrics.LyricsUtils.isValidLyrics(it, cleanTitle)
                    }?.let { validLyrics ->
                        policy.consider(provider.name, index, validLyrics)
                    }?.let { upgraded ->
                        val upgradedResult = LyricsWithProvider(upgraded.lyrics, upgraded.provider)
                        cache.put(cacheKey, listOf(LyricsResult(upgraded.provider, upgraded.lyrics)))
                        onUpgrade(upgradedResult)
                    }
                    val best = policy.best
                    if (best?.sync == LyricsSyncLevel.WORD && (0 until best.priority).all { it in completed }) break
                    fillWindow()
                }

                val finalResult = policy.best?.let {
                    val res = LyricsWithProvider(it.lyrics, it.provider)
                    cache.put(cacheKey, listOf(LyricsResult(it.provider, it.lyrics)))
                    res
                } ?: LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
                finalResult
            } finally {
                jobs.forEach { it.cancel() }
                answers.cancel()
            }
        }
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val isLocal = mediaId.startsWith("content://")
        val providers = resolveLyricsProviders()
            .let { if (isLocal) it.filterNot { p -> p.name == "YouTube Music" || p.name == "YouTube Subtitle" } else it }
            .filter { it.isEnabled(context) }

        val cacheKey = listOf(
            mediaId,
            songArtists,
            songTitle,
            duration.toString(),
            album.orEmpty(),
            providers.joinToString(",") { it.name }
        ).joinToString("|")

        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        val cleanTitle = pushkar.chorus.music.lyrics.LyricsUtils.cleanSongTitleForLyrics(songTitle)
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return
        }

        val allResult = java.util.concurrent.CopyOnWriteArrayList<LyricsResult>()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            val jobs = providers.map { provider ->
                launch(Dispatchers.IO) {
                    try {
                        withTimeoutOrNull(10_000) {
                            provider.getAllLyrics(mediaId, cleanTitle, songArtists, duration, album) { lyrics ->
                                if (lyrics != LYRICS_NOT_FOUND && lyrics.isNotBlank() && pushkar.chorus.music.lyrics.LyricsUtils.isValidLyrics(
                                        lyrics,
                                        cleanTitle
                                    )
                                ) {
                                    val result = LyricsResult(provider.name, lyrics)
                                    allResult += result
                                    callback(result)
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            jobs.forEach { it.join() }
            cache.put(cacheKey, allResult.toList())
        }

        currentLyricsJob?.join()
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 50
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)
