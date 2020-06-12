package com.soywiz.korvi

import com.soywiz.klock.*
import com.soywiz.korau.sound.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.vector.*

class DummyKorviVideo(
    val totalFrames: Long,
    val timePerFrame: TimeSpan,
    val width: Int = 320,
    val height: Int = 240
) : KorviVideo() {
    companion object {
        operator fun invoke(
            time: TimeSpan = 60.seconds,
            fps: Number = 24,
            width: Int = 320,
            height: Int = 240
        ) : KorviVideo {
            val timePerFrame = 1.seconds * (1 / fps.toDouble())
            return DummyKorviVideo((time / timePerFrame).toLong(), timePerFrame, width, height)
        }
    }
    override val video: List<KorviVideoStream> = listOf(DummyKorviVideoStream())
    override val audio: List<KorviAudioStream> = listOf(DummyKorviAudioStream())

    override suspend fun close() {
    }

    open inner class DummyBaseStream<TFrame : KorviFrame> : BaseKorviStream<TFrame> {
        var currentFrame = 0L

        override suspend fun getTotalFrames(): Long? = this@DummyKorviVideo.totalFrames
        override suspend fun getDuration(): TimeSpan? = timePerFrame * this@DummyKorviVideo.totalFrames.toDouble()
        override suspend fun seek(frame: Long) = run { currentFrame = frame }
        override suspend fun seek(time: TimeSpan) = run { seek((time / timePerFrame).toLong()) }
    }

    inner class DummyKorviVideoStream : DummyBaseStream<KorviVideoFrame>() {
        override suspend fun readFrame(): KorviVideoFrame? {
            if (currentFrame >= totalFrames) return null
            val frame = currentFrame++
            val currentTime = timePerFrame * frame.toDouble()
            val data = NativeImage(width, height)
            data.context2d {
                fill(Colors.DARKGREEN) {
                    fillRect(0, 0, width, height)
                }
                fillText(
                    currentTime.toTimeString(),
                    width * 0.5,
                    height * 0.5,
                    color = Colors.WHITE,
                    font = SystemFont("Arial"),
                    fontSize = 32.0,
                    halign = HorizontalAlign.CENTER,
                    valign = VerticalAlign.MIDDLE
                )
            }
            return KorviVideoFrame(data.toBMP32(), frame, timePerFrame * frame.toDouble(), timePerFrame)
        }
    }

    inner class DummyKorviAudioStream : DummyBaseStream<KorviAudioFrame>() {
        override suspend fun readFrame(): KorviAudioFrame? {
            if (currentFrame >= totalFrames) return null
            val frame = currentFrame++
            val data = AudioData(44100, AudioSamples(2, (44100 * timePerFrame.seconds).toInt()))
            return KorviAudioFrame(data, frame, timePerFrame * frame.toDouble(), timePerFrame)
        }
    }
}
