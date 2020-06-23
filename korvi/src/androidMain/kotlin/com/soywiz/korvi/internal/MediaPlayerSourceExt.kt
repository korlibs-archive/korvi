package com.soywiz.korvi.internal

import android.content.Context
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.os.Build
import com.soywiz.korio.android.androidContext
import com.soywiz.korio.android.withAndroidContext
import com.soywiz.korio.file.VfsFile
import kotlinx.coroutines.runBlocking
import java.io.FileDescriptor

// @TODO: Use String or FileDescriptor whenever possible since MediaDataSource requires
fun createMediaPlayerFromSource(source: VfsFile, context: Context): MediaPlayer {
    return createMediaPlayerFromSourceAny(source.toMediaDataSource(context))
}

suspend fun createMediaPlayerFromSource(source: VfsFile): MediaPlayer = createMediaPlayerFromSource(source, androidContext())
fun createMediaPlayerFromSource(source: MediaDataSource): MediaPlayer = createMediaPlayerFromSourceAny(source)
fun createMediaPlayerFromSource(path: String): MediaPlayer = createMediaPlayerFromSourceAny(path)
fun createMediaPlayerFromSource(fd: FileDescriptor): MediaPlayer = createMediaPlayerFromSourceAny(fd)

private fun createMediaPlayerFromSourceAny(source: Any?): MediaPlayer {
    val mediaPlayer = MediaPlayer()
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && source is MediaDataSource -> {
            mediaPlayer.setDataSource(source)
        }
        source is FileDescriptor -> {
            mediaPlayer.setDataSource(source)
        }
        source is String -> {
            mediaPlayer.setDataSource(source)
        }
        else -> {
            error("Requires Android M for video playback")
        }
    }

    //mediaPlayer.start()
    //val surface = createSurface(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
    //mediaPlayer.setSurface(surface.androidSurface)
    //mediaPlayer.pause()
    //mediaPlayer.seekTo(0)
    return mediaPlayer
}

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
