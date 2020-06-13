package com.soywiz.korvi.internal

import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hrMilliseconds
import com.soywiz.klock.hr.hrSeconds
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.HtmlImage
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.UrlVfs
import com.soywiz.korvi.KorviVideo
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.events.Event
import kotlin.browser.document
import kotlin.browser.window

internal actual val korviInternal: KorviInternal = JsKorviInternal()

internal class JsKorviInternal : KorviInternal() {
    override suspend fun createHighLevel(file: VfsFile): KorviVideo {
        val final = file.getUnderlyingUnscapedFile()
        val vfs = final.vfs
        when (vfs) {
            is UrlVfs -> return KorviVideoJs(final.file.absolutePath)
            else -> error("Unsupported playing video from: ${final}")
        }
    }
}

class KorviVideoJs(val url: String) : KorviVideo() {
    var canvas = document.createElement("canvas").unsafeCast<HTMLCanvasElement>()
    var video = document.createElement("video").unsafeCast<HTMLVideoElement>()
    init {
        video.src = url
    }

    override val running: Boolean get() = !video.paused
    override val elapsedTimeHr: HRTimeSpan get() = video.currentTime.hrSeconds

    override suspend fun getTotalFrames(): Long? = null
    override suspend fun getDuration(): HRTimeSpan? = video.duration.hrSeconds

    val videoComplete = { e: Event? ->
        onComplete(Unit)
    }

    val videoFrame = { e: Event? ->
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
        ctx.drawImage(video, 0.0, 0.0)
        val out = Bitmap32(video.videoWidth, video.videoHeight)
        HtmlImage.renderHtmlCanvasIntoBitmap(canvas.asDynamic(), out)
        onVideoFrame(Frame(out, video.currentTime.hrSeconds, 40.hrMilliseconds))
    }

    override suspend fun play() {
        removeListeners()
        addListeners()
        video.play()
    }

    override suspend fun seek(frame: Long) {
        super.seek(frame)
    }

    override suspend fun seek(time: HRTimeSpan) {
        video.fastSeek(time.secondsDouble)
    }

    lateinit var animationFrame: (Double) -> Unit
    init {
        animationFrame = {
            videoFrame(null)
            animationHandle = window.requestAnimationFrame(animationFrame)
        }
    }

    private fun addListeners() {
        animationFrame(0.0)
        video.addEventListener("ended", videoComplete)
    }

    private var animationHandle = -1
    private fun removeListeners() {
        window.cancelAnimationFrame(animationHandle)
        video.removeEventListener("ended", videoComplete)
    }

    override suspend fun stop() {
        removeListeners()
        video.pause()
    }

    override suspend fun close() {
        stop()
    }
}
