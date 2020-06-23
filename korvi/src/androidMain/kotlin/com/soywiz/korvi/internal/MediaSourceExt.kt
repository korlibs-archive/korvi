package com.soywiz.korvi.internal

import android.content.Context
import android.media.MediaDataSource
import android.os.Build
import com.soywiz.korio.android.withAndroidContext
import com.soywiz.korio.file.VfsFile
import kotlinx.coroutines.runBlocking

fun VfsFile.toMediaDataSource(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    object : MediaDataSource() {
        val vfsFile = this@toMediaDataSource

        val stream = runBlocking { withAndroidContext(context) { vfsFile.openRead() } }

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            return runBlocking {
                withAndroidContext(context) {
                    stream.position = position
                    stream.read(buffer, offset, size)
                }
            }
        }

        override fun getSize(): Long = runBlocking {
            withAndroidContext(context) {
                stream.getLength()
            }
        }

        override fun close() {
            runBlocking {  withAndroidContext(context) {stream.close() } }

        }
    }
} else {
    TODO("VERSION.SDK_INT < M")
}
