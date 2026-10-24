package zone.ien.hig.utils

import kotlinx.coroutines.android.awaitFrame as platformAwaitFrame

suspend fun awaitFrame() {
    platformAwaitFrame()
}
