package pushkar.chorus.music.utils

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import timber.log.Timber

class ArtworkQualityInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        if (data is String) {
            val upgradedUrl = upgradeArtworkQuality(data)
            if (upgradedUrl != data) {
                Timber.d("ArtworkQualityInterceptor: Upgraded resolution for $data -> $upgradedUrl")
                val newRequest = request.newBuilder()
                    .data(upgradedUrl)
                    .build()
                return chain.withRequest(newRequest).proceed()
            }
        }
        return chain.proceed()
    }

    companion object {
        fun upgradeArtworkQuality(url: String): String {
            if (url.isBlank()) return url

            if (url.contains("googleusercontent.com") || url.contains("yt3.ggpht.com")) {
                var updated = url
                if (Regex("([=\\-])[whs]\\d+").containsMatchIn(updated)) {
                    updated = updated
                        .replace(Regex("([=\\-])w\\d+"), "$1w1920")
                        .replace(Regex("([=\\-])h\\d+"), "$1h1920")
                        .replace(Regex("([=\\-])s\\d+"), "$1s1920")
                } else if (updated.contains("=w") || updated.contains("=s")) {
                    updated = updated.replace(Regex("=w\\d+-h\\d+.*$"), "=w1920-h1920-l90-rj")
                }
                return updated
            }

            if (url.contains("i.ytimg.com") || url.contains("img.youtube.com")) {
                return url.replace(
                    Regex("(default|mqdefault|hqdefault|sddefault|maxresdefault)\\.jpg"),
                    "maxresdefault.jpg"
                )
            }

            if (url.contains("mzstatic.com")) {
                return url
                    .replace(Regex("/\\d+x\\d+bb"), "/1920x1920bb")
                    .replace(Regex("/\\d+x\\d+w"), "/1920x1920w")
                    .replace(Regex("/\\d+x\\d+bf"), "/1920x1920bf")
            }

            return url
        }
    }
}
