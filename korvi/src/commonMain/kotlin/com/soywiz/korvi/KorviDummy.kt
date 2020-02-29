package com.soywiz.korvi

import com.soywiz.klock.*
import com.soywiz.korau.sound.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*

class DummyKorviVideo(val totalFrames: Long, val timePerFrame: TimeSpan) : KorviVideo() {
    companion object {
        operator fun invoke(time: TimeSpan, fps: Number = 24) : KorviVideo {
            val timePerFrame = 1.seconds * (1 / fps.toDouble())
            return DummyKorviVideo((time / timePerFrame).toLong(), timePerFrame)
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
            val frame = currentFrame++
            val currentTime = timePerFrame * frame.toDouble()
            val width = 320
            val height = 240
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
                    font = Context2d.Font("Arial", 32.0),
                    halign = Context2d.HorizontalAlign.CENTER,
                    valign = Context2d.VerticalAlign.MIDDLE
                )
            }
            return KorviVideoFrame(data.toBMP32(), frame, timePerFrame * frame.toDouble(), timePerFrame)
        }
    }

    inner class DummyKorviAudioStream : DummyBaseStream<KorviAudioFrame>() {
        override suspend fun readFrame(): KorviAudioFrame? {
            val frame = currentFrame++
            val data = AudioData(44100, AudioSamples(2, (44100 * timePerFrame.seconds).toInt()))
            return KorviAudioFrame(data, frame, timePerFrame * frame.toDouble(), timePerFrame)
        }
    }
}
