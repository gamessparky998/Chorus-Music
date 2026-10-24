package pushkar.chorus.music.lyrics

import android.content.Context
import kotlinx.coroutines.CancellationException
import pushkar.chorus.music.lyrics.bitchord.*
import java.util.Locale

internal fun List<LyricLine>.toChorusLyrics(): String {
    if (none { it.timeMs > 0 || it.words.isNotEmpty() }) return joinToString("\n") { it.text }
    fun stamp(ms: Long) = String.format(Locale.ROOT, "%02d:%02d.%03d", ms / 60_000, ms / 1000 % 60, ms % 1000)
    fun LyricLine.row(background: Boolean = false): String = buildString {
        append("[${stamp(timeMs.coerceAtLeast(0))}]")
        if (background) append("{bg}")
        if (alignment == LyricAlignment.End) append("{agent:v2}")
        if (words.isEmpty()) append(text) else words.forEachIndexed { index, word ->
            if (index > 0) append(' ')
            append("<${stamp(word.startMs)}>${word.text}")
        }
    }
    return flatMap { line -> listOfNotNull(line.row(), line.background?.row(true)) }.joinToString("\n")
}

object LrcRedLyricsProvider : LyricsProvider {
    override val name: String = "LrcRed"

    override fun isEnabled(context: Context): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = try {
        val lines = LrcRed.lyrics(
            title = title.forLyricsSearch(),
            artist = artist.artistForLyricsSearch(),
            durationMs = duration.toLong() * 1000L
        )
        if (lines.isNullOrEmpty()) Result.failure(NoSuchElementException("No lyrics found on lrc.red"))
        else Result.success(lines.toChorusLyrics())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        Result.failure(e)
    }
}

object BiniLyricsProvider : LyricsProvider {
    override val name: String = "BiniLyrics"

    override fun isEnabled(context: Context): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = try {
        val hit = BiniLyrics.lyrics(
            title = title.forLyricsSearch(),
            artist = artist.artistForLyricsSearch(),
            durationMs = duration.toLong() * 1000L,
            album = album
        )
        val lines = hit?.lines
        if (lines.isNullOrEmpty()) Result.failure(NoSuchElementException("No lyrics found on BiniLyrics"))
        else Result.success(lines.toChorusLyrics())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        Result.failure(e)
    }
}

object GeniusLyricsProvider : LyricsProvider {
    override val name: String = "Genius"

    override fun isEnabled(context: Context): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = try {
        val lines = Genius.lyrics(
            title = title.forLyricsSearch(),
            artist = artist.artistForLyricsSearch()
        )
        if (lines.isNullOrEmpty()) Result.failure(NoSuchElementException("No lyrics found on Genius"))
        else Result.success(lines.toChorusLyrics())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        Result.failure(e)
    }
}

object MegalobizLyricsProvider : LyricsProvider {
    override val name: String = "Megalobiz"

    override fun isEnabled(context: Context): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = try {
        val lines = Megalobiz.lyrics(
            title = title.forLyricsSearch(),
            artist = artist.artistForLyricsSearch()
        )
        if (lines.isNullOrEmpty()) Result.failure(NoSuchElementException("No lyrics found on Megalobiz"))
        else Result.success(lines.toChorusLyrics())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        Result.failure(e)
    }
}
