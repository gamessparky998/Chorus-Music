

package pushkar.chorus.music.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.models.filterYoutubeShorts
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.HomePage
import com.music.innertube.utils.completed
import pushkar.chorus.music.constants.HideExplicitKey
import pushkar.chorus.music.constants.HideVideoSongsKey
import pushkar.chorus.music.constants.HideYoutubeShortsKey
import pushkar.chorus.music.constants.InnerTubeCookieKey
import pushkar.chorus.music.constants.QuickPicks
import pushkar.chorus.music.constants.QuickPicksKey
import pushkar.chorus.music.db.MusicDatabase
import pushkar.chorus.music.db.entities.Album
import pushkar.chorus.music.db.entities.LocalItem
import pushkar.chorus.music.db.entities.Song
import pushkar.chorus.music.db.entities.SpeedDialItem
import pushkar.chorus.music.extensions.filterVideoSongs
import pushkar.chorus.music.extensions.toEnum
import pushkar.chorus.music.models.SimilarRecommendation
import pushkar.chorus.music.utils.SyncUtils
import pushkar.chorus.music.utils.dataStore
import pushkar.chorus.music.utils.get
import pushkar.chorus.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

data class DailyDiscoverItem(
    val seed: Song,
    val recommendation: YTItem,
    val relatedEndpoint: BrowseEndpoint?
)

data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    @pushkar.chorus.music.di.PlayerCache private val playerCache: androidx.media3.datasource.cache.SimpleCache,
    @pushkar.chorus.music.di.DownloadCache private val downloadCache: androidx.media3.datasource.cache.SimpleCache
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val isRandomizing = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val dailyDiscover = MutableStateFlow<List<DailyDiscoverItem>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val aiRecommendedPlaylist = database.playlistsByNameAsc()
        .map { playlists -> playlists.find { it.playlist.name == "Recommended by AI" } }
        .flatMapLatest { playlist -> 
            if (playlist != null && playlist.songCount > 0) {
                database.playlistSongs(playlist.playlist.id).map { playlistSongs -> 
                    playlist to playlistSongs.map { it.song }
                }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())
    
    val downloadedSongs = database.downloadedSongs(pushkar.chorus.music.constants.SongSortType.CREATE_DATE, true).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
    val cachedSongs: StateFlow<List<Song>> = _cachedSongs
    
    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val cachedIds = playerCache.keys.toSet()
                val downloadedIds = downloadCache.keys.toSet()
                val pureCacheIds = cachedIds.subtract(downloadedIds)

                val songs = if (pureCacheIds.isNotEmpty()) {
                    database.getSongsByIds(pureCacheIds.toList())
                } else {
                    emptyList()
                }

                val completeSongs = songs.filter {
                    val contentLength = it.format?.contentLength
                    contentLength != null && playerCache.isCached(it.song.id, 0, contentLength)
                }
                
                _cachedSongs.value = completeSongs
                    .filter { it.song.dateDownload != null }
                    .sortedByDescending { it.song.dateDownload }

                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    val localSongs = database.localSongs().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val speedDialItems: StateFlow<List<YTItem>> = database.speedDialDao.getAll().map { items ->
        items.map { it.toYTItem() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun getRandomItem(): YTItem? {
        try {
            isRandomizing.value = true
            
            kotlinx.coroutines.delay(1000)

            val userSongs = mutableListOf<YTItem>()
            val otherSources = mutableListOf<YTItem>()

            quickPicks.value?.let { songs ->
                userSongs.addAll(songs.map { song ->
                    SongItem(
                        id = song.id,
                        title = song.title,
                        artists = song.artists.map { Artist(name = it.name, id = it.id) },
                        thumbnail = song.thumbnailUrl ?: "",
                        explicit = false
                    )
                })
            }

            keepListening.value?.let { items ->
                items.forEach { item ->
                    when (item) {
                        is Song -> userSongs.add(SongItem(
                            id = item.id,
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = item.thumbnailUrl ?: "",
                            explicit = false
                        ))
                        is Album -> otherSources.add(AlbumItem(
                            browseId = item.id,
                            playlistId = item.album.playlistId ?: "",
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            year = item.album.year,
                            thumbnail = item.thumbnailUrl ?: ""
                        ))
                        else -> {}
                    }
                }
            }

            otherSources.addAll(allYtItems.value)

            
            val item = if (userSongs.isNotEmpty() && (otherSources.isEmpty() || Random.nextFloat() < 0.8f)) {
                userSongs.distinctBy { it.id }.shuffled().firstOrNull()
            } else {
                otherSources.distinctBy { it.id }.shuffled().firstOrNull()
            } ?: userSongs.firstOrNull() ?: otherSources.firstOrNull()

            return item
        } finally {
            isRandomizing.value = false
        }
    }

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    fun togglePin(item: YTItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val speedDialItem = SpeedDialItem.fromYTItem(item)
            val isPinned = database.speedDialDao.isPinned(speedDialItem.id).first()
            if (isPinned) {
                database.speedDialDao.delete(speedDialItem.id)
            } else {
                database.speedDialDao.insert(speedDialItem)
            }
        }
    }
    
    private var lastProcessedCookie: String? = null
    
    private var isProcessingAccountData = false

    private suspend fun getDailyDiscover() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val likedSongs = database.likedSongsByCreateDateAsc().first()
        if (likedSongs.isEmpty()) return

        val seeds = likedSongs.shuffled().distinctBy { it.id }.take(5)

        
        val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())

        kotlinx.coroutines.coroutineScope {
            seeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            val recommendations = page.songs
                                .filter { item ->
                                    if (hideVideoSongs && item.isVideoSong) return@filter false
                                    if (item.explicit) return@filter false
                                    true
                                }
                                .shuffled()

                            
                            val recommendation = recommendations.firstOrNull { rec ->
                                rec.id != seed.id
                            }

                            if (recommendation != null) {
                                items.add(
                                    DailyDiscoverItem(
                                        seed = seed,
                                        recommendation = recommendation,
                                        relatedEndpoint = endpoint
                                    )
                                )
                            }
                        }
                    }
                }
            }.forEach { it.join() }
        }

        
        dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }.shuffled()
    }

    private suspend fun getQuickPicks(usedIds: MutableSet<String> = mutableSetOf()) {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        
        val fromTimeStampCurrent = System.currentTimeMillis() - 86400000L * 7 * 4 
        val allTime = database.mostPlayedSongs(0, limit = 50, offset = 0).first()
        val current = database.mostPlayedSongs(fromTimeStampCurrent, limit = 50, offset = 0).first()
        
        val bestForYou = (allTime + current)
            .distinctBy { it.id }
            .filterVideoSongs(hideVideoSongs)
            .filter { it.id !in usedIds }
            .shuffled()
            .take(20)
            .map { it.copy(song = it.song.copy(thumbnailUrl = it.song.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544"))) }

        quickPicks.value = bestForYou
        usedIds.addAll(bestForYou.map { it.id })
    }

    private suspend fun getCommunityPlaylists() {
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 4
        val artistSeeds = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
        val songSeeds = database.mostPlayedSongs(fromTimeStamp, limit = 5).first()
            .shuffled().take(2)

        val candidatePlaylists = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            artistSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    YouTube.artist(seed.id).onSuccess { page ->
                        page.sections.forEach { section ->
                            section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" &&
                                    playlist.author?.name != "YouTube" &&
                                    playlist.author?.name != "Playlist" &&
                                    playlist.author?.name != seed.artist.name &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }

            songSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            page.playlists.forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" &&
                                    playlist.author?.name != "YouTube" &&
                                    playlist.author?.name != "Playlist" &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
        }

        val uniqueCandidates = candidatePlaylists.distinctBy { it.id }.shuffled().take(5)

        val playlists = java.util.Collections.synchronizedList(mutableListOf<CommunityPlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            uniqueCandidates.map { playlist ->
                launch(Dispatchers.IO) {
                    YouTube.playlist(playlist.id).onSuccess { page ->
                        val songs = page.songs.take(10)
                        if (songs.isNotEmpty()) {
                            
                            val songCountText = page.playlist.songCountText ?: playlist.songCountText
                            val updatedPlaylist = playlist.copy(songCountText = songCountText)
                            playlists.add(CommunityPlaylistItem(updatedPlaylist, songs))
                        }
                    }
                }
            }.forEach { it.join() }
        }

        communityPlaylists.value = playlists.shuffled()
    }

    private suspend fun loadLocalDataPhase() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val usedIds = mutableSetOf<String>()

        val fromTimeStampKL = System.currentTimeMillis() - 86400000L * 7 * 2
        val klSongs = database.mostPlayedSongs(fromTimeStampKL, limit = 20, offset = 0).first()
            .filterVideoSongs(hideVideoSongs)
            .take(10)
            
        val recentSong = database.events().first().firstOrNull()?.song
        val youWillLikeIt = mutableListOf<Song>()
        if (recentSong != null) {
            val endpoint = YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint
            if (endpoint != null) {
                YouTube.related(endpoint).onSuccess { page ->
                    page.songs.take(15).forEach { ytSong ->
                        database.song(ytSong.id).first()?.let { localSong ->
                            if ((!hideVideoSongs || !localSong.song.isVideo) && klSongs.none { it.id == localSong.id }) {
                                youWillLikeIt.add(localSong)
                            }
                        }
                    }
                }
            }
        }
        
        val speedDialExtraItems = (klSongs + youWillLikeIt).distinctBy { it.id }.take(27)
        usedIds.addAll(speedDialExtraItems.map { it.id })
        
        database.speedDialDao.clearAll()
        speedDialExtraItems.forEach {
            val item = SongItem(
                id = it.id,
                title = it.title,
                artists = it.artists.map { Artist(name = it.name, id = it.id) },
                thumbnail = it.thumbnailUrl ?: "",
                explicit = false
            )
            database.speedDialDao.insert(SpeedDialItem.fromYTItem(item))
        }

        getQuickPicks(usedIds)

        val forgotten = database.forgottenFavorites().first()
            .filterVideoSongs(hideVideoSongs)
            .filter { it.id !in usedIds }
            .shuffled().take(20)
        forgottenFavorites.value = forgotten
        usedIds.addAll(forgotten.map { it.id })

        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 50, offset = 0).first()
            .filterVideoSongs(hideVideoSongs)
            .filter { it.id !in usedIds }
            .shuffled().take(10)
        usedIds.addAll(keepListeningSongs.map { it.id })

        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 20, offset = 0).first()
            .filter { it.album.thumbnailUrl != null && it.id !in usedIds }
            .shuffled().take(5)
        usedIds.addAll(keepListeningAlbums.map { it.id })

        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp).first()
            .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null && it.id !in usedIds }
            .shuffled().take(5)
        usedIds.addAll(keepListeningArtists.map { it.id })

        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
    }

    
    private suspend fun loadSimilarRecommendations() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2

        coroutineScope {
            val artistDeferreds = database.mostPlayedArtists(fromTimeStamp, limit = 15).first()
                .filter { it.artist.isYouTubeArtist }
                .shuffled().take(4)
                .map { artist ->
                    async(Dispatchers.IO) {
                        val items = mutableListOf<YTItem>()
                        YouTube.artist(artist.id).onSuccess { page ->
                            page.sections.takeLast(3).forEach { section -> items += section.items }
                        }
                        SimilarRecommendation(
                            title = artist,
                            items = items
                                .distinctBy { item -> item.id }
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .shuffled()
                                .take(12)
                                .ifEmpty { return@async null }
                        )
                    }
                }

            val songDeferreds = database.mostPlayedSongs(fromTimeStamp, limit = 15).first()
                .filter { it.album != null }
                .shuffled().take(3)
                .map { song ->
                    async(Dispatchers.IO) {
                        val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                            ?: return@async null
                        val page = YouTube.related(endpoint).getOrNull() ?: return@async null
                        SimilarRecommendation(
                            title = song,
                            items = (page.songs.shuffled().take(10) +
                                    page.albums.shuffled().take(5) +
                                    page.artists.shuffled().take(3) +
                                    page.playlists.shuffled().take(3))
                                .distinctBy { it.id }
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .shuffled()
                                .ifEmpty { return@async null }
                        )
                    }
                }

            val albumDeferreds = database.mostPlayedAlbums(fromTimeStamp, limit = 10).first()
                .filter { it.album.thumbnailUrl != null }
                .shuffled().take(2)
                .map { album ->
                    async(Dispatchers.IO) {
                        val items = mutableListOf<YTItem>()
                        YouTube.album(album.id).onSuccess { page ->
                            page.otherVersions.let { items += it }
                        }
                        album.artists.firstOrNull()?.id?.let { artistId ->
                            YouTube.artist(artistId).onSuccess { page ->
                                page.sections.lastOrNull()?.items?.let { items += it }
                            }
                        }
                        SimilarRecommendation(
                            title = album,
                            items = items
                                .distinctBy { it.id }
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .shuffled()
                                .take(10)
                                .ifEmpty { return@async null }
                        )
                    }
                }

            val results = (artistDeferreds + songDeferreds + albumDeferreds).awaitAll()
            similarRecommendations.value = results.filterNotNull().shuffled()
        }
    }

    
    private suspend fun loadNetworkDataPhase() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

        coroutineScope {
            launch(Dispatchers.IO) { getDailyDiscover() }
            launch(Dispatchers.IO) { getCommunityPlaylists() }
            launch(Dispatchers.IO) { loadSimilarRecommendations() }
            launch(Dispatchers.IO) {
                YouTube.home().onSuccess { page ->
                    homePage.value = page.copy(
                        sections = page.sections.mapNotNull { section ->
                            val filteredItems = section.items
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .filterYoutubeShorts(hideYoutubeShorts)
                            if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                        }
                    )
                }.onFailure { reportException(it) }
            }
            launch(Dispatchers.IO) {
                YouTube.explore().onSuccess { page ->
                    explorePage.value = page.copy(
                        newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit)
                    )
                }.onFailure { reportException(it) }
            }
            if (YouTube.cookie != null) {
                launch(Dispatchers.IO) { loadAccountPlaylists() }
            }
        }

        
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty()
    }

    private suspend fun load() {
        isLoading.value = true

        
        loadLocalDataPhase()
        isLoading.value = false

        
        loadNetworkDataPhase()
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            var currentContinuation = continuation
            var hasNewItems = false

            while (currentContinuation != null && !hasNewItems) {
                val nextSections = YouTube.home(currentContinuation).getOrNull() ?: break
                currentContinuation = nextSections.continuation

                val newSections = nextSections.sections.mapNotNull { section ->
                    val filteredItems = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts)
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                }

                if (newSections.isNotEmpty()) {
                    hasNewItems = true
                }

                homePage.value = nextSections.copy(
                    chips = homePage.value?.chips,
                    continuation = currentContinuation,
                    sections = homePage.value?.sections.orEmpty() + newSections
                )
            }
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val nextSections = YouTube.home(params = chip.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                }
            )
            selectedChip.value = chip
        }
    }

    private suspend fun loadAccountPlaylists() {
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
            accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "SE" }
                .filterYoutubeShorts(hideYoutubeShorts)
        }.onFailure {
            reportException(it)
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isRefreshing.value = true
                load()
            } finally {
                isRefreshing.value = false
            }
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }
    }

    init {

        
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()

            load()
        }

        
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }

        
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .collect { cookie ->
                    
                    if (isProcessingAccountData) return@collect

                    
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true

                    try {
                        if (cookie != null && cookie.isNotEmpty()) {

                            
                            YouTube.cookie = cookie

                            
                            YouTube.accountInfo().onSuccess { info ->
                                accountName.value = info.name
                                accountImageUrl.value = info.thumbnailUrl
                            }.onFailure {
                                reportException(it)
                            }
                        } else {
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            accountPlaylists.value = null
                        }
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }

        
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[HideYoutubeShortsKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    if (YouTube.cookie != null && accountPlaylists.value != null) {
                        loadAccountPlaylists()
                    }
                }
        }
    }
}
