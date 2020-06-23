package com.soywiz.korvi.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.opengl.GLES20
import android.os.Build
import android.view.Surface
import com.soywiz.klock.Frequency
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.hr.hrMilliseconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korio.android.androidContext
import com.soywiz.korio.file.VfsFile
import com.soywiz.korvi.KorviVideo
import java.io.FileDescriptor
import java.lang.reflect.Field
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class AndroidKorviVideoAndroidMediaPlayer private constructor(val file: VfsFile, val androidContext: Context, val coroutineContext: CoroutineContext) : KorviVideo() {
    companion object {
        suspend operator fun invoke(file: VfsFile) = AndroidKorviVideoAndroidMediaPlayer(file, androidContext(), coroutineContext).also { it.init() }
    }

    lateinit var mediaResult: MediaResult
    val player get() = mediaResult.player
    val nativeImage get() = mediaResult.nativeImage

    private suspend fun init() {
        mediaResult = createMediaPlayerFromSource(file)
        player.duration
    }

    override val running: Boolean get() = player.isPlaying
    override val elapsedTimeHr: HRTimeSpan
        get() = super.elapsedTimeHr

    // @TODO: We should try to get this
    val frameRate: Frequency = 25.timesPerSecond

    override suspend fun getTotalFrames(): Long? =
        getDuration()?.let { duration -> (duration / frameRate.timeSpan.hr).toLong() }

    override suspend fun getDuration(): HRTimeSpan? = player.duration.takeIf { it >= 0 }?.hrMilliseconds

    override suspend fun play() {
        player.start()
    }

    override suspend fun seek(frame: Long) {
        seek(frameRate.timeSpan.hr * frame.toDouble())
    }

    override suspend fun seek(time: HRTimeSpan) {
        player.seekTo(time.millisecondsInt)
    }

    override suspend fun stop() {
        player.stop()
    }

    override suspend fun close() {
        stop()
    }
}

class SurfaceNativeImage(width: Int, height: Int, val androidSurface: Surface) : NativeImage(width, height, androidSurface, true) {
    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        val canvas = androidSurface.lockCanvas(Rect(0, 0, width, height))
        try {
            canvas.getBitmap().getPixels(out.ints, offset, width, x, y, width, height)
        } finally {
            androidSurface.unlockCanvasAndPost(canvas)
        }
    }

    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        val canvas = androidSurface.lockCanvas(Rect(0, 0, width, height))
        try {
            canvas.getBitmap().setPixels(out.ints, offset, width, x, y, width, height)
        } finally {
            androidSurface.unlockCanvasAndPost(canvas)
        }
    }

    private fun Canvas.getBitmap(): Bitmap {
        val field: Field = Canvas::class.java.getDeclaredField("mBitmap")
        field.isAccessible = true
        return field[this] as Bitmap
    }
}

data class MediaResult(val player: MediaPlayer, val nativeImage: SurfaceNativeImage)

// @TODO: Use String or FileDescriptor whenever possible since MediaDataSource requires
fun createMediaPlayerFromSource(source: VfsFile, context: Context): MediaResult {
    return createMediaPlayerFromSourceAny(source.toMediaDataSource(context))
}

suspend fun createMediaPlayerFromSource(source: VfsFile): MediaResult = createMediaPlayerFromSource(source, androidContext())
fun createMediaPlayerFromSource(source: MediaDataSource): MediaResult = createMediaPlayerFromSourceAny(source)
fun createMediaPlayerFromSource(path: String): MediaResult = createMediaPlayerFromSourceAny(path)
fun createMediaPlayerFromSource(fd: FileDescriptor): MediaResult = createMediaPlayerFromSourceAny(fd)

private fun createMediaPlayerFromSourceAny(source: Any?): MediaResult {
    val mediaPlayer = MediaPlayer()
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && source is MediaDataSource -> {
            mediaPlayer.setDataSource(source)
        }
        source is FileDescriptor -> {
            mediaPlayer.setDataSource(source)
        }
        source is String -> {
            mediaPlayer.setDataSource(source)
        }
        else -> {
            error("Requires Android M for video playback")
        }
    }

    val texts = IntArray(1)
    GLES20.glGenTextures(1, texts, 0)
    val surfaceTexture = SurfaceTexture(texts[0])
    val surface = Surface(surfaceTexture)
    val surfaceNativeImage = SurfaceNativeImage(mediaPlayer.videoWidth, mediaPlayer.videoHeight, surface)
    mediaPlayer.setSurface(surface)
    mediaPlayer.start()
    return MediaResult(mediaPlayer, surfaceNativeImage)
}
