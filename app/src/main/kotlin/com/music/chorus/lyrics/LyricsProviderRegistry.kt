package pushkar.chorus.music.lyrics

import pushkar.chorus.music.constants.PreferredLyricsProvider

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "LrcRed"          to LrcRedLyricsProvider,
        "BiniLyrics"      to BiniLyricsProvider,
        "YouLyPlus"       to YouLyPlusLyricsProvider,
        "Paxsenix"        to PaxSenixLyricsProvider,
        "BetterLyrics"    to BetterLyricsProvider,
        "Unison"          to UnisonLyricsProvider,
        "SimpMusic"       to SimpMusicLyricsProvider,
        "Musixmatch"      to MusixmatchLyricsProvider,
        "LrcLib"          to LrcLibLyricsProvider,
        "Kugou"           to KuGouLyricsProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTubeMusic"    to YouTubeLyricsProvider,
        "Megalobiz"       to MegalobizLyricsProvider,
        "Genius"          to GeniusLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) return getDefaultProviderOrder()
        return orderString.split(",").map { it.trim() }.filter { it in providerNames }
    }

    fun serializeProviderOrder(providers: List<String>): String =
        providers.filter { it in providerNames }.joinToString(",")

    fun getDefaultProviderOrder(): List<String> = listOf(
        "LrcRed",
        "BiniLyrics",
        "YouLyPlus",
        "Paxsenix",
        "Unison",
        "BetterLyrics",
        "SimpMusic",
        "Musixmatch",
        "LrcLib",
        "Kugou",
        "YouTubeSubtitle",
        "YouTubeMusic",
        "Megalobiz",
        "Genius",
    )

    fun getOrderedProviders(orderString: String): List<LyricsProvider> =
        deserializeProviderOrder(orderString).mapNotNull { getProviderByName(it) }

    fun getProviderNameForEnum(enum: PreferredLyricsProvider): String = when (enum) {
        PreferredLyricsProvider.LRCLIB        -> "LrcLib"
        PreferredLyricsProvider.KUGOU         -> "Kugou"
        PreferredLyricsProvider.BETTER_LYRICS -> "BetterLyrics"
        PreferredLyricsProvider.SIMPMUSIC     -> "SimpMusic"
        PreferredLyricsProvider.YOULYPLUS     -> "YouLyPlus"
        PreferredLyricsProvider.PAXSENIX      -> "Paxsenix"
        PreferredLyricsProvider.UNISON        -> "Unison"
        PreferredLyricsProvider.MUSIXMATCH    -> "Musixmatch"
        PreferredLyricsProvider.LRC_RED       -> "LrcRed"
        PreferredLyricsProvider.BINI_LYRICS   -> "BiniLyrics"
        PreferredLyricsProvider.GENIUS        -> "Genius"
        PreferredLyricsProvider.MEGALOBIZ     -> "Megalobiz"
    }

    fun getDisplayName(name: String): String = when (name) {
        "YouLyPlus"       -> "YouLyPlus"
        "Paxsenix"        -> "PaxSenix"
        "BetterLyrics"    -> "Better Lyrics"
        "Unison"          -> "Unison"
        "SimpMusic"       -> "SimpMusic"
        "LrcLib"          -> "LrcLib"
        "Kugou"           -> "KuGou"
        "YouTubeSubtitle" -> "YouTube Subtitle"
        "YouTubeMusic"    -> "YouTube Music"
        "Musixmatch"      -> "Musixmatch"
        "LrcRed"          -> "lrc.red"
        "BiniLyrics"      -> "BiniLyrics"
        "Genius"          -> "Genius"
        "Megalobiz"       -> "Megalobiz"
        else              -> name
    }
}
