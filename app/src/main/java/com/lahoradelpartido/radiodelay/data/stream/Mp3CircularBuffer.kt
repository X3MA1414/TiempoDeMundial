package com.lahoradelpartido.radiodelay.data.stream

import com.lahoradelpartido.radiodelay.domain.model.Delay
import java.io.Closeable
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Thread-safe time-indexed circular buffer of complete MP3 frames. */
class Mp3CircularBuffer(
    private val maxDurationUs: Long = DEFAULT_MAX_DURATION_US,
) {
    private val lock = ReentrantLock()
    private val newFrameCondition = lock.newCondition()
    private val frameOrder = ArrayDeque<Long>()
    private val framesBySequence = LinkedHashMap<Long, Mp3Frame>()

    private var nextSequence = 0L
    private var nextTimestampUs = 0L

    private val mutableAvailableDurationSeconds = MutableStateFlow(0)
    val availableDurationSeconds: StateFlow<Int> = mutableAvailableDurationSeconds.asStateFlow()

    fun append(parsedFrame: Mp3FrameParser.ParsedMp3Frame) {
        lock.withLock {
            val frame = Mp3Frame(
                sequence = nextSequence++,
                startTimeUs = nextTimestampUs,
                durationUs = parsedFrame.durationUs,
                bytes = parsedFrame.bytes,
            )
            nextTimestampUs = frame.endTimeUs
            frameOrder.addLast(frame.sequence)
            framesBySequence[frame.sequence] = frame
            trimLocked()
            updateAvailableDurationLocked()
            newFrameCondition.signalAll()
        }
    }

    fun openReader(delay: Delay): Reader {
        lock.withLock {
            while (frameOrder.isEmpty()) {
                newFrameCondition.await(READ_WAIT_MILLIS, TimeUnit.MILLISECONDS)
            }
            return Reader(chooseSequenceLocked(delay))
        }
    }

    fun clear() {
        lock.withLock {
            frameOrder.clear()
            framesBySequence.clear()
            nextSequence = 0L
            nextTimestampUs = 0L
            mutableAvailableDurationSeconds.value = 0
            newFrameCondition.signalAll()
        }
    }

    private fun chooseSequenceLocked(delay: Delay): Long {
        val newest = frameOrder.lastOrNull()?.let(framesBySequence::get)
            ?: return nextSequence
        val targetUs = (newest.endTimeUs - delay.totalSeconds * 1_000_000L)
            .coerceAtLeast(0L)

        var chosen = frameOrder.firstOrNull() ?: nextSequence
        for (sequence in frameOrder) {
            val frame = framesBySequence[sequence] ?: continue
            if (frame.startTimeUs > targetUs) break
            chosen = sequence
        }
        return chosen
    }

    private fun trimLocked() {
        val newestEnd = frameOrder.lastOrNull()
            ?.let(framesBySequence::get)
            ?.endTimeUs
            ?: return

        while (frameOrder.size > 1) {
            val oldestSequence = frameOrder.first()
            val oldest = framesBySequence[oldestSequence] ?: break
            if (newestEnd - oldest.startTimeUs <= maxDurationUs) break
            frameOrder.removeFirst()
            framesBySequence.remove(oldestSequence)
        }
    }

    private fun updateAvailableDurationLocked() {
        val oldest = frameOrder.firstOrNull()?.let(framesBySequence::get)
        val newest = frameOrder.lastOrNull()?.let(framesBySequence::get)
        val seconds = if (oldest == null || newest == null) {
            0
        } else {
            ((newest.endTimeUs - oldest.startTimeUs) / 1_000_000L).toInt()
        }
        mutableAvailableDurationSeconds.value = seconds.coerceAtMost(Delay.MAX_SECONDS)
    }

    inner class Reader internal constructor(
        initialSequence: Long,
    ) : Closeable {
        private var sequence = initialSequence
        private var byteOffset = 0
        @Volatile private var readerClosed = false

        fun read(target: ByteArray, targetOffset: Int, requestedLength: Int): Int {
            require(targetOffset >= 0 && requestedLength >= 0)
            require(targetOffset + requestedLength <= target.size)
            if (requestedLength == 0) return 0

            lock.withLock {
                while (!readerClosed) {
                    val oldestSequence = frameOrder.firstOrNull()
                    if (oldestSequence != null && sequence < oldestSequence) {
                        sequence = oldestSequence
                        byteOffset = 0
                    }

                    val frame = framesBySequence[sequence]
                    if (frame != null) {
                        val remaining = frame.bytes.size - byteOffset
                        val bytesToCopy = minOf(remaining, requestedLength)
                        frame.bytes.copyInto(
                            destination = target,
                            destinationOffset = targetOffset,
                            startIndex = byteOffset,
                            endIndex = byteOffset + bytesToCopy,
                        )
                        byteOffset += bytesToCopy
                        if (byteOffset >= frame.bytes.size) {
                            sequence += 1
                            byteOffset = 0
                        }
                        return bytesToCopy
                    }

                    newFrameCondition.await(READ_WAIT_MILLIS, TimeUnit.MILLISECONDS)
                }
                return END_OF_STREAM
            }
        }

        override fun close() {
            readerClosed = true
            lock.withLock { newFrameCondition.signalAll() }
        }
    }

    companion object {
        // Seven minutes give a safety margin over the selectable 5:59 maximum.
        const val DEFAULT_MAX_DURATION_US = 420_000_000L
        const val END_OF_STREAM = -1
        private const val READ_WAIT_MILLIS = 500L
    }
}
