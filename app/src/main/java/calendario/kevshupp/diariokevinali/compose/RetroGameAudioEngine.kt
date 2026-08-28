package calendario.kevshupp.diariokevinali.compose

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object RetroGameAudioEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var soundPool: SoundPool? = null
    private var rawJumpId: Int = 0
    private var rawPointId: Int = 0
    private var rawHeartId: Int = 0
    private var rawDieId: Int = 0
    private var isInitialized = false

    @Volatile
    private var isBgmPlaying = false
    private var bgmTrack: AudioTrack? = null
    private var bgmThread: Thread? = null
    private var currentThemeName = ""

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val sp = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            val res = context.resources
            val pkg = context.packageName

            val jId = res.getIdentifier("flappy_jump", "raw", pkg)
            if (jId != 0) rawJumpId = sp.load(context, jId, 1)

            val pId = res.getIdentifier("flappy_point", "raw", pkg)
            if (pId != 0) rawPointId = sp.load(context, pId, 1)

            val hId = res.getIdentifier("flappy_heart", "raw", pkg)
            if (hId != 0) rawHeartId = sp.load(context, hId, 1)

            val dId = res.getIdentifier("flappy_die", "raw", pkg)
            if (dId != 0) rawDieId = sp.load(context, dId, 1)

            soundPool = sp
        } catch (_: Exception) {}
    }

    fun release() {
        stopBgm()
        try {
            soundPool?.release()
            soundPool = null
            isInitialized = false
        } catch (_: Exception) {}
    }

    @Synchronized
    fun startBgm(theme: String = "FLAPPY", enabled: Boolean = true) {
        if (!enabled) {
            stopBgm()
            return
        }
        if (isBgmPlaying && currentThemeName == theme) return
        stopBgm()

        isBgmPlaying = true
        currentThemeName = theme

        bgmThread = Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val minBufSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufSize, 4096)
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                synchronized(this) {
                    bgmTrack = track
                }
                track.play()

                val (leadNotes, bassNotes, noteDurationMs) = when (theme) {
                    "SNAKE" -> getSnakeMelody()
                    else -> getFlappyMelody()
                }

                val samplesPerNote = (noteDurationMs * sampleRate) / 1000
                var noteIdx = 0

                var leadPhase = 0.0
                var bassPhase = 0.0

                val chunk = ShortArray(samplesPerNote)

                while (isBgmPlaying) {
                    val leadFreq = leadNotes[noteIdx % leadNotes.size]
                    val bassFreq = bassNotes[noteIdx % bassNotes.size]

                    for (i in 0 until samplesPerNote) {
                        if (!isBgmPlaying) break
                        val leadInc = if (leadFreq > 0) (2.0 * PI * leadFreq / sampleRate) else 0.0
                        val bassInc = if (bassFreq > 0) (2.0 * PI * bassFreq / sampleRate) else 0.0

                        leadPhase += leadInc
                        bassPhase += bassInc

                        // Lead: Onda cuadrada Chiptune
                        val leadNorm = (leadPhase / (2.0 * PI)) % 1.0
                        val leadVal = if (leadFreq > 0) {
                            if (leadNorm < 0.25) 0.16 else -0.16
                        } else 0.0

                        // Bass: Onda triangular cálida
                        val bassNorm = (bassPhase / (2.0 * PI)) % 1.0
                        val bassVal = if (bassFreq > 0) {
                            (if (bassNorm < 0.5) (4.0 * bassNorm - 1.0) else (3.0 - 4.0 * bassNorm)) * 0.18
                        } else 0.0

                        val env = (1.0 - (i.toDouble() / samplesPerNote) * 0.25).coerceIn(0.0, 1.0)
                        val mixed = (leadVal + bassVal) * env
                        chunk[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
                    }

                    if (isBgmPlaying) {
                        track.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                        noteIdx++
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {}
                synchronized(this) {
                    if (bgmTrack == track) bgmTrack = null
                }
            }
        }
        bgmThread?.priority = Thread.MIN_PRIORITY
        bgmThread?.start()
    }

    @Synchronized
    fun stopBgm() {
        if (!isBgmPlaying && bgmThread == null) return
        isBgmPlaying = false
        currentThemeName = ""
        try {
            bgmTrack?.pause()
            bgmTrack?.flush()
            bgmThread?.interrupt()
        } catch (_: Exception) {}
        bgmThread = null
    }

    fun playJump(enabled: Boolean) {
        if (!enabled) return
        if (rawJumpId != 0) {
            soundPool?.play(rawJumpId, 0.9f, 0.9f, 1, 0, 1.0f)
            return
        }
        scope.launch {
            playToneSweep(startFreq = 380.0, endFreq = 720.0, durationMs = 70, waveType = "SQUARE")
        }
    }

    fun playPoint(enabled: Boolean) {
        if (!enabled) return
        if (rawPointId != 0) {
            soundPool?.play(rawPointId, 0.9f, 0.9f, 1, 0, 1.0f)
            return
        }
        scope.launch {
            playTone(988.0, 50, "SQUARE")
            delay(20)
            playTone(1318.0, 80, "SQUARE")
        }
    }

    fun playHeart(enabled: Boolean) {
        if (!enabled) return
        if (rawHeartId != 0) {
            soundPool?.play(rawHeartId, 1.0f, 1.0f, 1, 0, 1.0f)
            return
        }
        scope.launch {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            for (freq in notes) {
                playTone(freq, 40, "TRIANGLE")
                delay(25)
            }
        }
    }

    fun playDie(enabled: Boolean) {
        if (!enabled) return
        if (rawDieId != 0) {
            soundPool?.play(rawDieId, 1.0f, 1.0f, 1, 0, 1.0f)
            return
        }
        scope.launch {
            playToneSweep(startFreq = 480.0, endFreq = 120.0, durationMs = 180, waveType = "NOISE")
        }
    }

    private fun playTone(freq: Double, durationMs: Int, waveType: String) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val sampleVal: Double = when (waveType) {
                    "SQUARE" -> if (sin(2 * PI * freq * t) >= 0) 0.4 else -0.4
                    "TRIANGLE" -> (2.0 / PI) * Math.asin(sin(2 * PI * freq * t)) * 0.5
                    else -> sin(2 * PI * freq * t) * 0.4
                }
                val envelope = (1.0 - (i.toDouble() / numSamples))
                buffer[i] = (sampleVal * envelope * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            track.setNotificationMarkerPosition(numSamples)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack?) {}
                override fun onMarkerReached(t: AudioTrack?) {
                    t?.release()
                }
            })
        } catch (_: Exception) {}
    }

    private fun playToneSweep(startFreq: Double, endFreq: Double, durationMs: Int, waveType: String) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2 * PI * currentFreq / sampleRate

                val sampleVal: Double = if (waveType == "NOISE") {
                    val rnd = (Random.nextDouble() * 2.0 - 1.0) * 0.3
                    val sine = sin(phase) * 0.3
                    rnd + sine
                } else {
                    if (sin(phase) >= 0) 0.45 else -0.45
                }
                val envelope = (1.0 - progress)
                buffer[i] = (sampleVal * envelope * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            track.setNotificationMarkerPosition(numSamples)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack?) {}
                override fun onMarkerReached(t: AudioTrack?) {
                    t?.release()
                }
            })
        } catch (_: Exception) {}
    }

    // Melodía Flappy Thor (Alegre, aérea, 4 secciones)
    private fun getFlappyMelody(): Triple<DoubleArray, DoubleArray, Int> {
        val leadNotes = doubleArrayOf(
            523.25, 659.25, 783.99, 1046.50,  783.99, 659.25, 523.25, 659.25,
            587.33, 698.46, 880.00, 1174.66,  880.00, 698.46, 587.33, 783.99,
            659.25, 783.99, 987.77, 1318.51,  987.77, 783.99, 659.25, 880.00,
            783.99, 698.46, 659.25, 587.33,   523.25, 0.0,    523.25, 783.99,

            880.00, 880.00, 0.0,    880.00,   1046.50, 880.00, 783.99, 659.25,
            698.46, 698.46, 0.0,    698.46,   880.00,  698.46, 659.25, 587.33,
            523.25, 587.33, 659.25, 698.46,   783.99,  880.00, 987.77, 1046.50,
            1174.66, 1046.50, 987.77, 880.00, 783.99,  0.0,    783.99, 987.77,

            1046.50, 0.0, 1046.50, 783.99,   659.25, 783.99, 1046.50, 1318.51,
            1174.66, 0.0, 1174.66, 880.00,   698.46, 880.00, 1174.66, 1396.91,
            1318.51, 1174.66, 1046.50, 987.77, 880.00, 783.99, 659.25, 587.33,
            523.25, 659.25, 783.99, 1046.50,  1046.50, 0.0,   783.99, 1046.50,

            880.00, 987.77, 1046.50, 880.00,  783.99, 659.25, 523.25, 659.25,
            587.33, 659.25, 698.46, 587.33,   659.25, 783.99, 880.00, 987.77,
            1046.50, 783.99, 659.25, 523.25,  587.33, 659.25, 587.33, 493.88,
            523.25, 523.25, 0.0,    523.25,   523.25, 0.0,    0.0,    0.0
        )

        val bassNotes = doubleArrayOf(
            130.81, 130.81, 196.00, 196.00, 130.81, 196.00, 130.81, 196.00,
            146.83, 146.83, 220.00, 220.00, 146.83, 220.00, 146.83, 220.00,
            164.81, 164.81, 246.94, 246.94, 164.81, 246.94, 164.81, 220.00,
            196.00, 174.61, 164.81, 146.83, 130.81, 0.0,    130.81, 196.00,

            110.00, 110.00, 164.81, 220.00, 110.00, 220.00, 164.81, 130.81,
            87.31,  87.31,  130.81, 174.61, 87.31,  174.61, 130.81, 110.00,
            130.81, 146.83, 164.81, 174.61, 196.00, 220.00, 246.94, 261.63,
            146.83, 146.83, 196.00, 196.00, 196.00, 0.0,    196.00, 246.94,

            130.81, 130.81, 196.00, 196.00, 130.81, 196.00, 261.63, 196.00,
            146.83, 146.83, 220.00, 220.00, 146.83, 220.00, 293.66, 220.00,
            164.81, 146.83, 130.81, 123.47, 110.00, 98.00,  82.41,  73.42,
            130.81, 164.81, 196.00, 261.63, 261.63, 0.0,    196.00, 261.63,

            110.00, 123.47, 130.81, 110.00, 98.00,  82.41,  65.41,  82.41,
            73.42,  82.41,  87.31,  73.42,  82.41,  98.00,  110.00, 123.47,
            130.81, 98.00,  82.41,  65.41,  73.42,  82.41,  73.42,  61.74,
            65.41,  65.41,  0.0,    65.41,  65.41,  0.0,    0.0,    0.0
        )
        return Triple(leadNotes, bassNotes, 125)
    }

    // Melodía Snake (Estilo Arcade Retro / Tetris & Pacman feel en escala menor armónica)
    private fun getSnakeMelody(): Triple<DoubleArray, DoubleArray, Int> {
        val leadNotes = doubleArrayOf(
            // Tema A: Rápido, misterioso arcade
            659.25, 493.88, 523.25, 587.33, 523.25, 493.88, 440.00, 440.00,
            523.25, 659.25, 587.33, 523.25, 493.88, 523.25, 587.33, 659.25,
            523.25, 440.00, 440.00, 0.0,    587.33, 698.46, 880.00, 783.99,
            698.46, 659.25, 523.25, 659.25, 587.33, 523.25, 493.88, 493.88,

            // Tema B: Tensión y persecución
            523.25, 587.33, 659.25, 523.25, 440.00, 440.00, 0.0,    0.0,
            659.25, 523.25, 587.33, 493.88, 523.25, 440.00, 415.30, 493.88,
            659.25, 523.25, 587.33, 493.88, 523.25, 659.25, 880.00, 830.61,
            880.00, 783.99, 698.46, 659.25, 587.33, 523.25, 493.88, 440.00
        )

        val bassNotes = doubleArrayOf(
            220.00, 220.00, 220.00, 220.00, 220.00, 220.00, 220.00, 220.00,
            174.61, 174.61, 174.61, 174.61, 164.81, 164.81, 164.81, 164.81,
            220.00, 220.00, 220.00, 220.00, 146.83, 146.83, 146.83, 146.83,
            174.61, 174.61, 174.61, 174.61, 164.81, 164.81, 164.81, 164.81,

            220.00, 220.00, 220.00, 220.00, 220.00, 220.00, 220.00, 220.00,
            220.00, 220.00, 196.00, 196.00, 174.61, 174.61, 164.81, 164.81,
            220.00, 220.00, 196.00, 196.00, 174.61, 174.61, 164.81, 164.81,
            220.00, 196.00, 174.61, 164.81, 146.83, 130.81, 123.47, 110.00
        )
        return Triple(leadNotes, bassNotes, 140)
    }
}
