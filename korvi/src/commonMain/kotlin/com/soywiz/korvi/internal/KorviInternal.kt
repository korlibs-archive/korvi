package com.soywiz.korvi.internal

import com.soywiz.klock.*
import com.soywiz.korio.stream.*
import com.soywiz.korvi.*

internal expect val korviInternal: KorviInternal

internal open class KorviInternal {
    open fun createContainer(stream: AsyncStream): KorviVideo = DummyKorviVideo(3.minutes)
}
