package com.soywiz.korvi.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.view.Surface
import com.soywiz.klock.Frequency
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.hr.hrMilliseconds
import com.soywiz.klock.hr.hrNanoseconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korio.android.androidContext
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.Disposable
import com.soywiz.korvi.KorviVideo
import java.io.FileDescriptor
import java.lang.reflect.Field
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext


class AndroidKorviVideoAndroidMediaPlayer private constructor(val file: VfsFile, val androidContext: Context, val coroutineContext: CoroutineContext) : KorviVideo() {
    companion object {
        suspend operator fun invoke(file: VfsFile) = AndroidKorviVideoAndroidMediaPlayer(file, androidContext(), coroutineContext).also { it.init() }
    }

    lateinit var player: MediaPlayer
    lateinit var nativeImage: SurfaceNativeImage

    private var lastTimeSpan: HRTimeSpan = HRTimeSpan.ZERO
    private suspend fun init() {
        player = createMediaPlayerFromSource(file)
    }

    override fun prepare() {
        //val offsurface = OffscreenSurface(1024, 1024)
        //offsurface.makeCurrentTemporarily {
        println("CREATING SURFACE")
        val info = SurfaceNativeImage.createSurfacePair()
        println("SET SURFACE")
        player.setSurface(info.surface)
        println("PREPARING")
        player.prepare()
        println("CREATE SURFACE FOR VIDEO: ${player.videoWidth},${player.videoHeight}")
        nativeImage = SurfaceNativeImage(player.videoWidth, player.videoHeight, info)
        nativeImage.surfaceTexture.setOnFrameAvailableListener {
            //offsurface.makeCurrentTemporarily {
            run {
                nativeImage.surfaceTexture.updateTexImage()
                println("setOnFrameAvailableListener!")
                lastTimeSpan = it.timestamp.toDouble().hrNanoseconds
                onVideoFrame(Frame(nativeImage.toBMP32(), lastTimeSpan, frameRate.timeSpan.hr))
                //onVideoFrame(Frame(Bitmap32(128, 128, Colors.RED), lastTimeSpan, frameRate.timeSpan.hr))
            }
        }
        //}
    }

    override val running: Boolean get() = player.isPlaying
    override val elapsedTimeHr: HRTimeSpan get() = lastTimeSpan

    // @TODO: We should try to get this
    val frameRate: Frequency = 25.timesPerSecond

    override suspend fun getTotalFrames(): Long? =
        getDuration()?.let { duration -> (duration / frameRate.timeSpan.hr).toLong() }

    override suspend fun getDuration(): HRTimeSpan? = player.duration.takeIf { it >= 0 }?.hrMilliseconds

    override suspend fun play() {
        println("START")
        player.start()
    }

    override suspend fun seek(frame: Long) {
        seek(frameRate.timeSpan.hr * frame.toDouble())
    }

    override suspend fun seek(time: HRTimeSpan) {
        lastTimeSpan = time
        player.seekTo(time.millisecondsInt)
    }

    override suspend fun stop() {
        player.stop()
    }

    override suspend fun close() {
        stop()
    }
}

data class SurfaceTextureInfo(val surface: Surface, val texture: SurfaceTexture, val texId: Int)

class SurfaceNativeImage(width: Int, height: Int, val info: SurfaceTextureInfo) : NativeImage(width, height, info, true), Disposable {
    val surface get() = info.surface
    val surfaceTexture get() = info.texture
    val texId get() = info.texId

    companion object {
        operator fun invoke(width: Int, height: Int): SurfaceNativeImage {
            val info = createSurfacePair()
            return SurfaceNativeImage(width, height, info)
        }

        fun createSurfacePair(): SurfaceTextureInfo {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            //val surfaceTexture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) SurfaceTexture(true) else TODO("VERSION.SDK_INT < O")
            val surfaceTexture = SurfaceTexture(textures[0])
            val surface = Surface(surfaceTexture)
            return SurfaceTextureInfo(surface, surfaceTexture, textures[0])
        }
    }

    override fun dispose() {
        surface.release()
        surfaceTexture.release()
    }

    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        println("readPixelsUnsafe")
        //for (n in 0 until width * height) out[offset + n] = Colors.RED
        /*
        val bmp = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val lock = java.util.concurrent.Semaphore(0)
            println("PixelCopy.request")
            PixelCopy.request(surface, bmp, {
                println("PixelCopy.request : release")
                lock.release()
            }, Handler())
            println("PixelCopy.request : acquire")
            lock.acquire()
            println("PixelCopy.request : completed!")
        }
        bmp.getPixels(out.ints, offset, width, x, y, width, height)
        */
        for (n in 0 until width * height) out[offset + n] = Colors.RED
        /*
        val canvas = surface.lockCanvas(Rect(0, 0, width, height))
        try {
            canvas.getBitmap().getPixels(out.ints, offset, width, x, y, width, height)
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
         */
    }

    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        println("writePixelsUnsafe")
        /*
        val canvas = surface.lockCanvas(Rect(0, 0, width, height))
        try {
            canvas.getBitmap().setPixels(out.ints, offset, width, x, y, width, height)
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
         */
    }

    private fun Canvas.getBitmap(): Bitmap {
        val field: Field = Canvas::class.java.getDeclaredField("mBitmap")
        field.isAccessible = true
        return field[this] as Bitmap
    }
}

data class MediaResult(val player: MediaPlayer, val nativeImage: SurfaceNativeImage)

// @TODO: Use String or FileDescriptor whenever possible since MediaDataSource requires
fun createMediaPlayerFromSource(source: VfsFile, context: Context): MediaPlayer {
    return createMediaPlayerFromSourceAny(source.toMediaDataSource(context))
}

suspend fun createMediaPlayerFromSource(source: VfsFile): MediaPlayer = createMediaPlayerFromSource(source, androidContext())
fun createMediaPlayerFromSource(source: MediaDataSource): MediaPlayer = createMediaPlayerFromSourceAny(source)
fun createMediaPlayerFromSource(path: String): MediaPlayer = createMediaPlayerFromSourceAny(path)
fun createMediaPlayerFromSource(fd: FileDescriptor): MediaPlayer = createMediaPlayerFromSourceAny(fd)

private fun createMediaPlayerFromSourceAny(source: Any?): MediaPlayer {
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

    //mediaPlayer.start()
    //val surface = createSurface(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
    //mediaPlayer.setSurface(surface.androidSurface)
    //mediaPlayer.pause()
    //mediaPlayer.seekTo(0)
    return mediaPlayer
}

