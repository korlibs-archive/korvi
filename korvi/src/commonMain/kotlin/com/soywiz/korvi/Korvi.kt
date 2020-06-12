package com.soywiz.korvi

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.*
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.hr.timeSpan
import com.soywiz.korau.sound.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.stream.*
import com.soywiz.korvi.internal.*
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext

suspend fun KorviVideo(file: VfsFile): KorviVideo = korviInternal.createHighLevel(file)
fun KorviVideoLL(stream: AsyncStream): KorviVideoLL = korviInternal.createContainer(stream)

open class KorviVideo {
    class Frame(
        var data: Bitmap32,
        val duration: HRTimeSpan
    )
    val onVideoFrame = Signal<Frame>()
    val onComplete = Signal<Unit>()
    open suspend fun getTotalFrames(): Long? = null
    open suspend fun getDuration(): HRTimeSpan? = null
    open suspend fun play(): Unit = Unit
    open suspend fun seek(frame: Long): Unit = Unit
    open suspend fun seek(time: HRTimeSpan): Unit = Unit
    open suspend fun stop(): Unit = Unit
}

open class KorviVideoFromLL(val ll: KorviVideoLL) : KorviVideo() {
    override suspend fun getTotalFrames(): Long? = ll.getTotalFrames()
    override suspend fun getDuration(): HRTimeSpan? = ll.getDuration()

    override suspend fun seek(frame: Long) = ll.seek(frame)
    override suspend fun seek(time: HRTimeSpan) = ll.seek(time)

    override suspend fun stop() {
        job?.cancel()
        audioStream?.stop()
        audioStream?.dispose()
    }

    var job: Job? = null
    var audioStream: PlatformAudioOutput? = null

    override suspend fun play() {
        if (job != null) return

        audioStream = nativeSoundProvider.createAudioStream()

        job = launchImmediately(coroutineContext) {
            launchImmediately(job!!) {
                do {
                    var frames = 0
                    ll.video.fastForEach { stream ->
                        val frame = stream.readFrame()
                        if (frame != null) {
                            frames++
                            onVideoFrame(Frame(frame.data, frame.duration))
                            delay(frame.duration.timeSpan)
                        }
                    }
                } while (frames > 0)
                onComplete(Unit)
            }
            launchImmediately(job!!) {
                do {
                    var frames = 0
                    ll.audio.fastForEach { stream ->
                        val frame = stream.readFrame()
                        if (frame != null) {
                            frames++
                            audioStream!!.add(frame.data)
                        }
                        // 44100 - 1 second, 4410 - 100 milliseconds, 441 - 10 milliseconds
                        while (audioStream!!.availableSamples > 4410) {
                            delay(10.milliseconds)
                        }
                    }
                } while (frames > 0)
            }
        }
    }
}

open class KorviVideoLL() : BaseKorviSeekable {
    open val video: List<KorviVideoStream> = listOf()
    open val audio: List<KorviAudioStream> = listOf()
    val streams: List<BaseKorviStream<out KorviFrame>> by lazy { video + audio }
    final override suspend fun getTotalFrames(): Long? = streams.mapNotNull { it.getTotalFrames() }.max()
    final override suspend fun getDuration(): HRTimeSpan? = streams.mapNotNull { it.getDuration() }.max()
    final override suspend fun seek(frame: Long): Unit = run { for (v in streams) v.seek(frame) }
    final override suspend fun seek(time: HRTimeSpan): Unit = run { for (v in streams) v.seek(time) }
    override suspend fun close() = Unit
}

interface BaseKorviSeekable : AsyncCloseable {
    suspend fun getTotalFrames(): Long? = null
    suspend fun getDuration(): HRTimeSpan? = null
    suspend fun seek(frame: Long): Unit = TODO()
    suspend fun seek(time: HRTimeSpan): Unit = TODO()
    override suspend fun close() = Unit
}
suspend fun BaseKorviSeekable.seek(time: TimeSpan): Unit = seek(time.hr)

interface BaseKorviStream<T : KorviFrame> : BaseKorviSeekable {
    suspend fun readFrame(): T? = TODO()
}

typealias KorviVideoStream = BaseKorviStream<KorviVideoFrame>
typealias KorviAudioStream = BaseKorviStream<KorviAudioFrame>

interface KorviFrame {
    val frame: Long
    val position: HRTimeSpan
    val duration: HRTimeSpan
    val end: HRTimeSpan get() = position + duration
}
data class KorviAudioFrame(val data: AudioData, override val frame: Long, override val position: HRTimeSpan, override val duration: HRTimeSpan) : KorviFrame
data class KorviVideoFrame(val data: Bitmap32, override val frame: Long, override val position: HRTimeSpan, override val duration: HRTimeSpan) : KorviFrame
