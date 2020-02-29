package com.soywiz.korvi.internal

internal actual val korviInternal: KorviInternal = JvmKorviInternal()

internal class JvmKorviInternal : KorviInternal() {
}
