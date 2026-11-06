package com.mahjongzen.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Sfx { SELECT, MATCH, COMBO, INVALID, HINT, SHUFFLE, UNDO, WIN }

/**
 * Plays the procedurally generated sound effects (SoundPool) and background music (MediaPlayer).
 * Sounds are synthesised once into the cache directory on first start.
 */
class SoundManager(private val context: Context, private val scope: CoroutineScope) {

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
    private val ids = java.util.concurrent.ConcurrentHashMap<Sfx, Int>()
    @Volatile private var ready = false

    private var player: MediaPlayer? = null
    private var musicFile: File? = null
    private var musicWanted = false
    private var musicVolume = 0.35f
    private var appInForeground = true

    var effectsEnabled = true
    var effectsVolume = 0.7f

    fun init() {
        scope.launch(Dispatchers.Default) {
            val dir = File(context.cacheDir, "sfx_v1").apply { mkdirs() }
            val generators = mapOf(
                Sfx.SELECT to Synth::select, Sfx.MATCH to Synth::match, Sfx.COMBO to Synth::combo,
                Sfx.INVALID to Synth::invalid, Sfx.HINT to Synth::hint, Sfx.SHUFFLE to Synth::shuffle,
                Sfx.UNDO to Synth::undo, Sfx.WIN to Synth::win,
            )
            for ((sfx, gen) in generators) {
                val f = File(dir, "${sfx.name.lowercase()}.wav")
                if (!f.exists() || f.length() < 100) Synth.writeWav(f, gen())
                ids[sfx] = pool.load(f.absolutePath, 1)
            }
            val m = File(dir, "music.wav")
            if (!m.exists() || m.length() < 100) Synth.writeWav(m, Synth.music(), Synth.MUSIC_RATE)
            musicFile = m
            ready = true
            withContext(Dispatchers.Main) { updateMusic() }
        }
    }

    fun play(sfx: Sfx, pitch: Float = 1f) {
        if (!effectsEnabled || !ready) return
        val id = ids[sfx] ?: return
        val v = effectsVolume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, pitch.coerceIn(0.5f, 2f))
    }

    fun setMusic(enabled: Boolean, volume: Float) {
        musicWanted = enabled
        musicVolume = volume
        updateMusic()
    }

    fun onAppForeground(foreground: Boolean) {
        appInForeground = foreground
        updateMusic()
    }

    private fun updateMusic() {
        val file = musicFile
        val shouldPlay = musicWanted && appInForeground && file != null
        if (!shouldPlay) {
            player?.let { if (it.isPlaying) it.pause() }
            return
        }
        val p = player ?: runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(file!!.absolutePath)
                isLooping = true
                prepare()
            }
        }.getOrNull()?.also { player = it } ?: return
        p.setVolume(musicVolume, musicVolume)
        if (!p.isPlaying) p.start()
    }
}
