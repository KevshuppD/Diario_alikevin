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
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object RetroGameAudioEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var soundPool: SoundPool? = null
    private var rawJumpId: Int = 0
    private var rawPointId: Int = 0
    private var rawHeartId: Int = 0
    private var rawDieId: Int = 0
    private var rouletteTickId: Int = 0
    private var rouletteWinId: Int = 0
    private var rouletteLoseId: Int = 0
    private var rouletteAgainId: Int = 0
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

            loadRouletteSounds(context, sp)

            soundPool = sp
        } catch (_: Exception) {}
    }

    // --- SONIDOS DE LA RULETA (se sintetizan una vez a WAV en caché y se cargan en SoundPool: latencia mínima por tick) ---
    private fun loadRouletteSounds(context: Context, sp: SoundPool) {
        val sr = 22050
        // Tick: clic corto y seco de la lengüeta
        val tick = synth(sr, 0.025) { t, _ ->
            (if (sin(2 * PI * 1900.0 * t) >= 0) 0.5 else -0.5) * exp(-t / 0.006)
        }
        // Premio: arpegio ascendente Do-Mi-Sol-Do + acorde final
        val winNotes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val win = synth(sr, 0.75) { t, _ ->
            val idx = (t / 0.09).toInt()
            if (idx < winNotes.size) {
                val lt = t - idx * 0.09
                (if ((winNotes[idx] * lt) % 1.0 < 0.25) 0.35 else -0.35) * exp(-lt / 0.08)
            } else {
                val lt = t - winNotes.size * 0.09
                val chord = sin(2 * PI * 523.25 * lt) + sin(2 * PI * 659.25 * lt) + sin(2 * PI * 783.99 * lt) + sin(2 * PI * 1046.50 * lt)
                chord * 0.12 * exp(-lt / 0.18)
            }
        }
        // Nada: "wah wah wah waaah" descendente
        val loseNotes = doubleArrayOf(392.00, 369.99, 349.23, 329.63)
        val lose = synth(sr, 1.3) { t, _ ->
            val idx = minOf((t / 0.25).toInt(), loseNotes.size - 1)
            val lt = t - idx * 0.25
            val last = idx == loseNotes.size - 1
            val f = loseNotes[idx] * (if (last) 1.0 + 0.02 * sin(2 * PI * 6.0 * lt) else 1.0)
            val env = if (last) exp(-lt / 0.35) else exp(-lt / 0.12)
            val tri = (2.0 / PI) * Math.asin(sin(2 * PI * f * t))
            tri * 0.45 * env
        }
        // Otra vez: "bloop" ascendente doble
        val again = synth(sr, 0.36) { t, _ ->
            val lt = t % 0.18
            val f = 440.0 + 900.0 * (lt / 0.18)
            (if ((f * lt) % 1.0 < 0.5) 0.3 else -0.3) * exp(-lt / 0.09)
        }
        rouletteAgainId = loadWav(context, sp, "roulette_again.wav", again, sr)
        rouletteTickId = loadWav(context, sp, "roulette_tick.wav", tick, sr)
        rouletteWinId = loadWav(context, sp, "roulette_win.wav", win, sr)
        rouletteLoseId = loadWav(context, sp, "roulette_lose.wav", lose, sr)
    }

    private inline fun synth(sr: Int, seconds: Double, f: (Double, Int) -> Double): ShortArray {
        val n = (sr * seconds).toInt()
        return ShortArray(n) { i -> (f(i.toDouble() / sr, i).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun loadWav(context: Context, sp: SoundPool, name: String, pcm: ShortArray, sr: Int): Int {
        return try {
            val file = java.io.File(context.cacheDir, name)
            val dataLen = pcm.size * 2
            val buf = java.nio.ByteBuffer.allocate(44 + dataLen).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            buf.put("RIFF".toByteArray()); buf.putInt(36 + dataLen); buf.put("WAVE".toByteArray())
            buf.put("fmt ".toByteArray()); buf.putInt(16); buf.putShort(1); buf.putShort(1)
            buf.putInt(sr); buf.putInt(sr * 2); buf.putShort(2); buf.putShort(16)
            buf.put("data".toByteArray()); buf.putInt(dataLen)
            for (v in pcm) buf.putShort(v)
            file.writeBytes(buf.array())
            sp.load(file.absolutePath, 1)
        } catch (_: Exception) { 0 }
    }

    fun playRouletteTick(enabled: Boolean) {
        if (enabled && rouletteTickId != 0) soundPool?.play(rouletteTickId, 0.6f, 0.6f, 1, 0, 1.0f)
    }

    fun playRouletteWin(enabled: Boolean) {
        if (enabled && rouletteWinId != 0) soundPool?.play(rouletteWinId, 1.0f, 1.0f, 2, 0, 1.0f)
    }

    fun playRouletteAgain(enabled: Boolean) {
        if (enabled && rouletteAgainId != 0) soundPool?.play(rouletteAgainId, 1.0f, 1.0f, 2, 0, 1.0f)
    }

    fun playRouletteLose(enabled: Boolean) {
        if (enabled && rouletteLoseId != 0) soundPool?.play(rouletteLoseId, 1.0f, 1.0f, 2, 0, 1.0f)
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

                val song = when (theme) {
                    "SNAKE" -> SNAKE_SONG
                    else -> FLAPPY_SONG
                }

                val samplesPerStep = (song.stepMs * sampleRate) / 1000
                val chunk = ShortArray(samplesPerStep)
                val attackSamples = sampleRate / 200 // 5 ms
                val releaseSamples = samplesPerStep / 6

                var step = 0
                var leadPhase = 0.0
                var bassPhase = 0.0
                var leadHeld = 0 // muestras desde el inicio de la nota actual (envolvente y vibrato)
                var bassHeld = 0
                var drumType = '.'
                var drumT = 0
                var kickPhase = 0.0
                var noiseSeed = 12345
                var lastNoise = 0.0

                while (isBgmPlaying) {
                    val next = (step + 1) % song.steps
                    val leadFreq = song.leadFreq[step]
                    val bassFreq = song.bassFreq[step]
                    if (song.leadAttack[step]) leadHeld = 0
                    if (song.bassAttack[step]) bassHeld = 0
                    // Cortar la nota antes del siguiente ataque o silencio para que las notas repetidas se distingan
                    val leadReleases = song.leadAttack[next] || song.leadFreq[next] == 0.0
                    val bassReleases = song.bassAttack[next] || song.bassFreq[next] == 0.0
                    val drum = song.drums[step]
                    if (drum != '.') { drumType = drum; drumT = 0; kickPhase = 0.0 }

                    for (i in 0 until samplesPerStep) {
                        if (!isBgmPlaying) break
                        val tail = samplesPerStep - i
                        var mixed = 0.0

                        // Lead: onda cuadrada 25% con vibrato suave en notas largas
                        if (leadFreq > 0) {
                            val heldSec = leadHeld.toDouble() / sampleRate
                            val vib = if (heldSec > 0.25) 1.0 + 0.006 * sin(2.0 * PI * 5.5 * heldSec) else 1.0
                            leadPhase += leadFreq * vib / sampleRate
                            if (leadPhase >= 1.0) leadPhase -= 1.0
                            var env = 0.6 + 0.4 * exp(-heldSec / 0.18)
                            if (leadHeld < attackSamples) env *= leadHeld.toDouble() / attackSamples
                            if (leadReleases && tail < releaseSamples) env *= tail.toDouble() / releaseSamples
                            mixed += (if (leadPhase < 0.25) 0.13 else -0.13) * env
                        }
                        leadHeld++

                        // Bajo: onda triangular cálida
                        if (bassFreq > 0) {
                            bassPhase += bassFreq / sampleRate
                            if (bassPhase >= 1.0) bassPhase -= 1.0
                            var env = 0.75 + 0.25 * exp(-bassHeld.toDouble() / sampleRate / 0.12)
                            if (bassHeld < attackSamples) env *= bassHeld.toDouble() / attackSamples
                            if (bassReleases && tail < releaseSamples) env *= tail.toDouble() / releaseSamples
                            val tri = if (bassPhase < 0.5) (4.0 * bassPhase - 1.0) else (3.0 - 4.0 * bassPhase)
                            mixed += tri * 0.17 * env
                        }
                        bassHeld++

                        // Batería: K = bombo, S = caja, H = charles (ruido LCG, sin asignaciones)
                        if (drumType != '.') {
                            val t = drumT.toDouble() / sampleRate
                            noiseSeed = noiseSeed * 1103515245 + 12345
                            val noise = ((noiseSeed ushr 16) and 0x7FFF) / 16384.0 - 1.0
                            when (drumType) {
                                'K' -> {
                                    kickPhase += (45.0 + 110.0 * exp(-t / 0.03)) / sampleRate
                                    mixed += sin(2.0 * PI * kickPhase) * 0.22 * exp(-t / 0.07)
                                }
                                'S' -> mixed += (noise * 0.09 + sin(2.0 * PI * 185.0 * t) * 0.05) * exp(-t / 0.06)
                                'H' -> mixed += (noise - lastNoise) * 0.03 * exp(-t / 0.015)
                            }
                            lastNoise = noise
                            drumT++
                            if (t > 0.3) drumType = '.'
                        }

                        chunk[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
                    }

                    if (isBgmPlaying) {
                        track.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                        step = next
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
}

// ==========================================================================
// CANCIONES DE FONDO (constantes a nivel superior: se parsean una sola vez)
// Notación: "NOTA:duración" en semicorcheas (16 por compás), "r" = silencio.
// Batería: un carácter por semicorchea (K bombo, S caja, H charles, . nada).
// ==========================================================================

private class ChiptuneSong(val stepMs: Int, lead: String, bass: String, val drums: String) {
    val leadFreq: DoubleArray
    val leadAttack: BooleanArray
    val bassFreq: DoubleArray
    val bassAttack: BooleanArray
    val steps: Int

    init {
        val (lf, la) = parseVoice(lead)
        val (bf, ba) = parseVoice(bass)
        leadFreq = lf; leadAttack = la; bassFreq = bf; bassAttack = ba
        steps = minOf(lf.size, bf.size, drums.length)
    }

    private fun parseVoice(text: String): Pair<DoubleArray, BooleanArray> {
        val freqs = ArrayList<Double>()
        val attacks = ArrayList<Boolean>()
        for (token in text.trim().split(Regex("\\s+"))) {
            val (note, len) = token.split(":")
            val f = noteToFreq(note)
            repeat(len.toInt()) { i ->
                freqs.add(f)
                attacks.add(i == 0 && f > 0)
            }
        }
        return freqs.toDoubleArray() to attacks.toBooleanArray()
    }

    private fun noteToFreq(note: String): Double {
        if (note == "r") return 0.0
        var semis = when (note[0]) {
            'C' -> 0; 'D' -> 2; 'E' -> 4; 'F' -> 5; 'G' -> 7; 'A' -> 9; else -> 11
        }
        var idx = 1
        if (note[idx] == '#') { semis++; idx++ } else if (note[idx] == 'b') { semis--; idx++ }
        val midi = 12 * (note.substring(idx).toInt() + 1) + semis
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
    }
}

// Flappy Pet: Do mayor alegre (~70 s): intro, tema A, A', puente B, pausa tranquila y final
private val FLAPPY_SONG = ChiptuneSong(
    stepMs = 115,
    lead =
        "C5:2 E5:2 G5:2 C6:2 G5:2 E5:2 C5:4 B4:2 D5:2 G5:2 B5:2 A5:2 G5:2 F5:2 D5:2 " +
        "E5:4 G5:2 E5:2 C5:4 E5:4 A5:4 G5:2 E5:2 C5:6 r:2 " +
        "F5:2 F5:2 A5:2 F5:2 C6:4 A5:4 G5:6 F5:2 E5:4 D5:4 " +
        "E5:4 G5:2 E5:2 C6:4 B5:2 A5:2 A5:4 E5:2 A5:2 C6:6 r:2 " +
        "F5:2 A5:2 C6:4 D6:2 C6:2 B5:4 C6:12 r:4 " +
        "E5:4 G5:2 E5:2 C5:4 E5:4 A5:4 G5:2 E5:2 C5:6 r:2 " +
        "F5:2 F5:2 A5:2 F5:2 C6:4 A5:4 G5:6 F5:2 E5:4 D5:4 " +
        "G5:2 A5:2 G5:2 E5:2 C5:4 E5:2 G5:2 A5:2 B5:2 C6:2 A5:2 E5:4 r:4 " +
        "F5:2 G5:2 A5:4 B5:2 A5:2 G5:4 C5:2 E5:2 G5:2 C6:2 C6:6 r:2 " +
        "A5:8 C6:4 A5:4 B5:8 D6:4 B5:4 " +
        "G5:4 B5:4 E6:6 r:2 E6:2 D6:2 C6:2 B5:2 A5:8 " +
        "F5:2 A5:2 C6:2 F6:2 E6:4 C6:4 D6:2 B5:2 G5:2 B5:2 D6:8 " +
        "F6:4 E6:2 D6:2 A5:4 F5:4 G5:2 A5:2 B5:2 D6:2 G6:4 r:4 " +
        "E5:8 A5:8 F5:8 C6:8 " +
        "G5:8 E5:4 G5:4 D5:2 G5:2 B5:2 D6:2 F6:2 D6:2 B5:2 G5:2 " +
        "E5:4 G5:2 E5:2 C5:4 E5:4 A5:4 G5:2 E5:2 C5:6 r:2 " +
        "F5:2 F5:2 A5:2 F5:2 C6:4 A5:4 G5:6 F5:2 E5:4 D5:4 " +
        "E5:4 G5:2 E5:2 C6:4 B5:2 A5:2 A5:4 E5:2 A5:2 C6:6 r:2 " +
        "F5:2 A5:2 C6:4 D6:2 C6:2 B5:4 C6:2 G5:2 E5:2 G5:2 C6:6 r:2 ",
    bass =
        "C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 G2:2 D3:2 G3:2 D3:2 G2:2 D3:2 G3:2 D3:2 " +
        "C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 A2:2 E3:2 A3:2 E3:2 A2:2 E3:2 A3:2 E3:2 " +
        "F2:2 C3:2 F3:2 C3:2 F2:2 C3:2 F3:2 C3:2 G2:2 D3:2 G3:2 D3:2 G2:2 D3:2 G3:2 D3:2 " +
        "C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 A2:2 E3:2 A3:2 E3:2 A2:2 E3:2 A3:2 E3:2 " +
        "F2:2 C3:2 F3:2 C3:2 G2:2 D3:2 G3:2 D3:2 C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 " +
        "C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 A2:2 E3:2 A3:2 E3:2 A2:2 E3:2 A3:2 E3:2 " +
        "F2:2 C3:2 F3:2 C3:2 F2:2 C3:2 F3:2 C3:2 G2:2 D3:2 G3:2 D3:2 G2:2 D3:2 G3:2 D3:2 " +
        "C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 A2:2 E3:2 A3:2 E3:2 A2:2 E3:2 A3:2 E3:2 " +
        "F2:2 C3:2 F3:2 C3:2 G2:2 D3:2 G3:2 D3:2 C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 " +
        "F2:2 C3:2 F3:2 C3:2 F2:2 C3:2 F3:2 C3:2 G2:2 D3:2 G3:2 D3:2 G2:2 D3:2 G3:2 D3:2 " +
        "E2:2 B2:2 E3:2 B2:2 E2:2 B2:2 E3:2 B2:2 A2:2 E3:2 A3:2 E3:2 A2:2 E3:2 A3:2 E3:2 " +
        "F2:2 C3:2 F3:2 C3:2 F2:2 C3:2 F3:2 C3:2 G2:2 D3:2 G3:2 D3:2 G2:2 D3:2 G3:2 D3:2 " +
        "D3:2 A3:2 D4:2 A3:2 D3:2 A3:2 D4:2 A3:2 G2:2 D3:2 G3:2 D3:2 G2:2 D3:2 G3:2 D3:2 " +
        "A2:8 E3:8 F2:8 C3:8 " +
        "C3:8 G3:8 G2:8 D3:8 " +
        "C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 A2:2 E3:2 A3:2 E3:2 A2:2 E3:2 A3:2 E3:2 " +
        "F2:2 C3:2 F3:2 C3:2 F2:2 C3:2 F3:2 C3:2 G2:2 D3:2 G3:2 D3:2 G2:2 D3:2 G3:2 D3:2 " +
        "C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 A2:2 E3:2 A3:2 E3:2 A2:2 E3:2 A3:2 E3:2 " +
        "F2:2 C3:2 F3:2 C3:2 G2:2 D3:2 G3:2 D3:2 C3:2 G3:2 C4:2 G3:2 C3:2 G3:2 C4:2 G3:2 ",
    drums =
        "K...H...K...H...K...H...K...H...K.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HH" +
        "K.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HH" +
        "K.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HH" +
        "K.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HH" +
        "K.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.HHS.H.K.HHS.HSK.HHS.H.K.HHS.HS" +
        "K.HHS.H.K.HHS.HSK.HHS.H.K.HHS.HSK.HHS.H.K.HHS.HSK.HHS.H.K.HHS.HS" +
        "K.HHS.H.K.HHS.HSK.HHS.H.K.HHS.HSH...H...H...H...H...H...H...H..." +
        "H...H...H...H...H...H...H...H...K.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HH" +
        "K.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HH" +
        "K.H.S.H.K.KHS.HHK.H.S.H.K.KHS.HH"
)

// Snake: La menor armónica estilo arcade (~64 s): intro, tema A, A', persecución B, quiebre y vuelta
private val SNAKE_SONG = ChiptuneSong(
    stepMs = 105,
    lead =
        "A4:2 r:2 A4:2 r:2 A4:2 C5:2 E5:2 A5:2 G#5:2 r:2 G#5:2 r:2 B5:2 G#5:2 E5:2 B4:2 " +
        "A5:2 E5:2 A5:2 B5:2 C6:2 B5:2 A5:2 E5:2 C6:2 B5:2 A5:2 G#5:2 A5:4 E5:4 " +
        "F5:2 A5:2 C6:2 A5:2 F5:2 A5:2 C6:2 D6:2 B5:4 G5:2 D5:2 G5:4 B5:4 " +
        "A5:2 C6:2 E6:2 C6:2 A5:2 C6:2 E6:2 A6:2 G#6:2 E6:2 B5:2 E6:2 G#6:4 r:4 " +
        "D6:2 F6:2 A6:2 F6:2 E6:2 G#6:2 B6:2 G#6:2 A6:6 E6:2 A5:4 r:4 " +
        "A5:2 E5:2 A5:2 B5:2 C6:2 B5:2 A5:2 E5:2 C6:2 B5:2 A5:2 G#5:2 A5:4 E5:4 " +
        "F5:2 A5:2 C6:2 A5:2 F5:2 A5:2 C6:2 D6:2 B5:4 G5:2 D5:2 G5:4 B5:4 " +
        "E6:2 D6:2 C6:2 B5:2 A5:2 B5:2 C6:2 E6:2 D6:2 C6:2 B5:2 G#5:2 E5:4 G#5:4 " +
        "F5:2 A5:2 D6:2 F6:2 E6:4 D6:2 B5:2 A5:4 E5:2 A5:2 C6:2 E6:2 A6:4 " +
        "C6:2 r:2 C6:2 A5:2 C6:2 D6:2 C6:2 A5:2 B5:2 r:2 B5:2 G5:2 B5:2 D6:2 B5:2 G5:2 " +
        "G5:2 B5:2 E6:2 B5:2 G6:4 E6:4 E6:2 C6:2 A5:2 C6:2 E6:4 A6:4 " +
        "F6:4 E6:2 D6:2 C6:4 A5:4 G6:4 F6:2 E6:2 D6:4 B5:4 " +
        "G#5:2 B5:2 E6:2 G#6:2 B6:4 G#6:4 E6:2 F6:2 E6:2 D6:2 E6:2 B5:2 G#5:2 E5:2 " +
        "A5:12 r:4 C6:12 r:4 " +
        "B5:12 r:4 G#5:2 A5:2 B5:2 C6:2 D6:2 E6:2 F6:2 G#6:2 " +
        "A5:2 E5:2 A5:2 B5:2 C6:2 B5:2 A5:2 E5:2 C6:2 B5:2 A5:2 G#5:2 A5:4 E5:4 " +
        "F5:2 A5:2 C6:2 A5:2 F5:2 A5:2 C6:2 D6:2 B5:4 G5:2 D5:2 G5:4 B5:4 " +
        "A5:2 C6:2 E6:2 C6:2 A5:2 C6:2 E6:2 A6:2 G#6:2 E6:2 B5:2 E6:2 G#6:4 r:4 " +
        "D6:2 F6:2 A6:2 F6:2 E6:2 G#6:2 B6:2 G#6:2 A6:6 E6:2 A5:4 r:4 ",
    bass =
        "A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 " +
        "A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 " +
        "F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 " +
        "A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 " +
        "D3:2 D4:2 D3:2 D4:2 E2:2 E3:2 E2:2 E3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 " +
        "A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 " +
        "F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 " +
        "A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 " +
        "D3:2 D4:2 D3:2 D4:2 E2:2 E3:2 E2:2 E3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 " +
        "F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 " +
        "E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 " +
        "F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 " +
        "E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 " +
        "A2:8 E3:8 F2:8 C3:8 " +
        "G2:8 D3:8 E2:8 B2:8 " +
        "A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 " +
        "F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 F2:2 F3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 G2:2 G3:2 " +
        "A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 E2:2 E3:2 " +
        "D3:2 D4:2 D3:2 D4:2 E2:2 E3:2 E2:2 E3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 A2:2 A3:2 ",
    drums =
        "K...K...K...K.K.K...K...K...K.K.K.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HK" +
        "K.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HK" +
        "K.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HK" +
        "K.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HK" +
        "K.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.HKS.HHK.HKS.SSK.HKS.HHK.HKS.SS" +
        "K.HKS.HHK.HKS.SSK.HKS.HHK.HKS.SSK.HKS.HHK.HKS.SSK.HKS.HHK.HKS.SS" +
        "K.HKS.HHK.HKS.SSK.HKS.HHK.HKS.SSK...H...K...H...K...H...K...H..." +
        "K...H...K...H...K...H...K...H...K.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HK" +
        "K.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HK" +
        "K.H.S.HHK.H.S.HKK.H.S.HHK.H.S.HK"
)
