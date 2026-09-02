package com.lahoradelpartido.radiodelay.playback
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lahoradelpartido.radiodelay.RadioDelayApplication
import com.lahoradelpartido.radiodelay.domain.model.Delay

/**
 * Foreground media service.
 *
 * Media3 owns the media-style notification and foreground promotion. The session is
 * registered explicitly because commands from the app arrive as service intents rather
 * than through a MediaController.
 */
@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var engine: DelayedPlaybackEngine

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")

        val container = (application as RadioDelayApplication).container
        container.stateStore.update { it.copy(serviceActive = true) }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_AFTER_REBUFFER_MS,
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(),
                    true,
                )
                setHandleAudioBecomingNoisy(true)
                setWakeMode(C.WAKE_MODE_LOCAL)
                addListener(servicePlayerListener)
            }

        engine = DelayedPlaybackEngine(
            player = player,
            circularBuffer = container.circularBuffer,
            streamClient = container.streamClient,
            delayPreferences = container.delayPreferences,
            emissionPreferences = container.emissionPreferences,
            stateStore = container.stateStore,
        )

        mediaSession = MediaSession.Builder(this, player).build()

        // The UI controls this service with explicit intents instead of a MediaController.
        // Registering the session here lets Media3 observe BUFFERING/READY immediately,
        // publish the media notification and promote this service to foreground.
        addSession(mediaSession)
        Log.i(TAG, "MediaSession registered")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand action=${intent?.action}")

        return when (intent?.action) {
            ACTION_PLAY -> {
                // MediaSessionService.onStartCommand is @CallSuper, but it must not receive
                // our private app action. Passing null executes its lifecycle bookkeeping
                // without attempting to interpret ACTION_PLAY as a Media3 notification action.
                super.onStartCommand(null, flags, startId)
                engine.play()
                START_STICKY
            }

            ACTION_PAUSE -> {
                super.onStartCommand(null, flags, startId)
                engine.pause()
                START_STICKY
            }

            ACTION_SET_DELAY -> {
                super.onStartCommand(null, flags, startId)
                engine.setDelay(
                    Delay.ofSeconds(intent.getIntExtra(EXTRA_DELAY_SECONDS, 0)),
                )
                START_STICKY
            }

            ACTION_SET_EMISSION -> {
                super.onStartCommand(null, flags, startId)
                engine.applySelectedEmission()
                START_STICKY
            }

            // Media buttons, notification actions and controller intents still belong
            // to MediaSessionService and must be delegated with their original intent.
            else -> super.onStartCommand(intent, flags, startId)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "onTaskRemoved playbackOngoing=${isPlaybackOngoing()}")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.w(
            TAG,
            "onDestroy playWhenReady=${if (::player.isInitialized) player.playWhenReady else false} " +
                    "state=${if (::player.isInitialized) player.playbackState else Player.STATE_IDLE}",
        )

        val preserveBuffer = ::player.isInitialized &&
                player.playWhenReady &&
                player.playbackState != Player.STATE_IDLE

        val container = (application as RadioDelayApplication).container
        container.stateStore.update {
            it.copy(
                serviceActive = false,
                isPlaying = false,
                isBuffering = false,
            )
        }

        if (::player.isInitialized) {
            player.removeListener(servicePlayerListener)
        }
        if (::mediaSession.isInitialized) {
            mediaSession.release()
        }
        if (::engine.isInitialized) {
            // Preserve the in-memory history only when Android tears the service down
            // during active playback. Normal paused shutdowns still clear stale audio.
            engine.release(clearBuffer = !preserveBuffer)
        }
        if (::player.isInitialized) {
            player.release()
        }
        super.onDestroy()
    }

    private val servicePlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(TAG, "playerState=${playbackState.asLogName()}")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            Log.d(TAG, "playWhenReady=$playWhenReady reason=$reason")
        }
    }

    private fun Int.asLogName(): String = when (this) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> toString()
    }

    companion object {
        const val ACTION_PLAY = "com.lahoradelpartido.radiodelay.action.PLAY"
        const val ACTION_PAUSE = "com.lahoradelpartido.radiodelay.action.PAUSE"
        const val ACTION_SET_DELAY = "com.lahoradelpartido.radiodelay.action.SET_DELAY"
        const val ACTION_SET_EMISSION = "com.lahoradelpartido.radiodelay.action.SET_EMISSION"
        const val EXTRA_DELAY_SECONDS = "extra_delay_seconds"

        private const val TAG = "RadioPlaybackService"
        private const val MIN_BUFFER_MS = 2_500
        private const val MAX_BUFFER_MS = 60_000
        private const val BUFFER_FOR_PLAYBACK_MS = 300
        private const val BUFFER_AFTER_REBUFFER_MS = 600
    }
}