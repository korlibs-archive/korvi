package com.soywiz.korvi.internal

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.os.Build
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hrMilliseconds
import com.soywiz.klock.hr.hrNanoseconds
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.AndroidNativeImage
import com.soywiz.korio.android.androidContext
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.AndroidResourcesVfs
import com.soywiz.korio.file.std.LocalVfs
import com.soywiz.korvi.KorviVideo
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext


internal actual val korviInternal: KorviInternal = AndroidKorviInternal()

internal class AndroidKorviInternal : KorviInternal() {
    override suspend fun createHighLevel(file: VfsFile): KorviVideo {
        //val final = file.getUnderlyingUnscapedFile()
        //val vfs = final.vfs
        return AndroidKorviVideo(file, androidContext(), coroutineContext)
    }
}

class AndroidKorviVideo(val file: VfsFile, val androidContext: Context, val coroutineContext: CoroutineContext) : KorviVideo() {
    //val realPath = path.trimStart('/')

    init {
        println("TRYING TO OPEN VIDEO '$file'")
    }

    val player = VideoPlayer(file, androidContext) { image, player ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // plane #0 is always Y, plane #1 is always U (Cb), and plane #2 is always V (Cr)
            if (image.format != ImageFormat.YUV_420_888) error("Only supported YUV_420 formats")

            val py = image.planes[0]
            val pu = image.planes[1]
            val pv = image.planes[2]

            val _y = py.buffer
            val _u = pu.buffer
            val _v = pv.buffer

            val bmp = Bitmap32(image.width, image.height)

            println("width: ${image.width}")
            println("height: ${image.height}")
            println("y: $_y, ${py.rowStride}, ${py.pixelStride}")
            println("u: $_u, ${pu.rowStride}, ${pu.pixelStride}")
            println("v: $_v, ${pv.rowStride}, ${pv.pixelStride}")

            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val cy = _y.get(_y.position() + y * py.rowStride + x).toInt() and 0xFF
                    //val cu = _u.get(_u.position() + y * pu.rowStride + x).toInt() and 0xFF
                    //val cv = _v.get(_v.position() + y * pv.rowStride + x).toInt() and 0xFF
                    //bmp[x, y] = RGBA(cy, cu, cv, 0xFF)
                    bmp[x, y] = RGBA(cy, cy, cy, 0xFF)
                }
            }

            launchImmediately(coroutineContext) {
                onVideoFrame(KorviVideo.Frame(bmp, image.timestamp.toDouble().hrNanoseconds, 40.hrMilliseconds))
            }
        } else {
            TODO("VERSION.SDK_INT < KITKAT")
        }
    }

    override var running: Boolean = false
    override val elapsedTimeHr: HRTimeSpan get() = 0.hrMilliseconds


    override suspend fun getTotalFrames(): Long? = null
    override suspend fun getDuration(): HRTimeSpan? = null

    override suspend fun play() {
        thread {
            try {
                running = true
                player.play()
            } finally {
                running = false
            }
        }
    }

    override suspend fun seek(frame: Long) {
        TODO()
    }

    override suspend fun seek(time: HRTimeSpan) {
        TODO()
    }

    override suspend fun stop() {
        if (running) {
            player.requestStop()
        }
    }

    override suspend fun close() {
        stop()
    }
}
