package com.soywiz.korvi

import com.soywiz.klock.*
import com.soywiz.korau.sound.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.stream.*
import com.soywiz.korvi.internal.*

fun KorviContainer(stream: AsyncStream): KorviContainer = korviInternal.createContainer(stream)

open class KorviContainer() : BaseKorviSeekable {
    open val video: List<KorviVideoStream> = listOf()
    open val audio: List<KorviAudioStream> = listOf()
    val streams: List<BaseKorviStream<out KorviFrame>> by lazy { video + audio }
    final override suspend fun getTotalFrames(): Long? = streams.mapNotNull { it.getTotalFrames() }.max()
    final override suspend fun getDuration(): TimeSpan? = streams.mapNotNull { it.getDuration() }.max()
    final override suspend fun seek(frame: Long): Unit = run { for (v in streams) v.seek(frame) }
    final override suspend fun seek(time: TimeSpan): Unit = run { for (v in streams) v.seek(time) }
    override suspend fun close() = Unit
}

interface BaseKorviSeekable : AsyncCloseable {
    suspend fun getTotalFrames(): Long? = null
    suspend fun getDuration(): TimeSpan? = null
    suspend fun seek(frame: Long): Unit = TODO()
    suspend fun seek(time: TimeSpan): Unit = TODO()
    override suspend fun close() = Unit
}

interface BaseKorviStream<T : KorviFrame> : BaseKorviSeekable {
    suspend fun readFrame(): T? = TODO()
}

typealias KorviVideoStream = BaseKorviStream<KorviVideoFrame>
typealias KorviAudioStream = BaseKorviStream<KorviAudioFrame>

interface KorviFrame {
    val frame: Long
    val position: TimeSpan
    val duration: TimeSpan
    val end: TimeSpan get() = position + duration
}
data class KorviAudioFrame(val data: AudioData, override val frame: Long, override val position: TimeSpan, override val duration: TimeSpan) : KorviFrame
data class KorviVideoFrame(val data: Bitmap32, override val frame: Long, override val position: TimeSpan, override val duration: TimeSpan) : KorviFrame
