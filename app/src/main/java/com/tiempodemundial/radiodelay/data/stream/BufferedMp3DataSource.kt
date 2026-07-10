package com.tiempodemundial.radiodelay.data.stream

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.tiempodemundial.radiodelay.domain.model.Delay

/** Media3 adapter exposing one delayed cursor over the circular MP3 buffer. */
@OptIn(UnstableApi::class)
class BufferedMp3DataSource(
    private val circularBuffer: Mp3CircularBuffer,
    private val delay: Delay,
) : BaseDataSource(false) {
    private var reader: Mp3CircularBuffer.Reader? = null
    private var opened = false
    private var currentUri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        currentUri = dataSpec.uri
        reader = circularBuffer.openReader(delay)
        opened = true
        transferStarted(dataSpec)
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val result = reader?.read(buffer, offset, length) ?: Mp3CircularBuffer.END_OF_STREAM
        if (result > 0) bytesTransferred(result)
        return if (result == Mp3CircularBuffer.END_OF_STREAM) C.RESULT_END_OF_INPUT else result
    }

    override fun getUri(): Uri? = currentUri

    override fun close() {
        reader?.close()
        reader = null
        currentUri = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    class Factory(
        private val circularBuffer: Mp3CircularBuffer,
        private val delay: Delay,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = BufferedMp3DataSource(circularBuffer, delay)
    }
}
