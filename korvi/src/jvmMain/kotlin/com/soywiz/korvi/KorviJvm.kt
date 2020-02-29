package com.soywiz.korvi

import com.soywiz.klock.*
import com.soywiz.korim.awt.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*

object KorviJvm {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            //val frame = FrameGrab.getFrameFromFile(File("C:/tmp/test.mp4"), 0)
            //println(frame)

            val container = KorviContainer(rootLocalVfs["C:/tmp/dw11222.mp4"].open())
            val duration = container.getDuration()!!
            println(duration)
            println((duration * 0.5).seconds)
            //container.seek((duration * 0.5))
            container.seek(121.seconds)
            awtShowImageAndWait(container.video.first().readFrame()!!.data)

            //val videoDecoder = JCodecUtil.createVideoDecoder(videoTrack.meta.codec, videoTrack.nextFrame().data)
            //val picture = videoDecoder.decodeFrame(videoTrack.nextFrame().data, pic)
        }
    }
}
