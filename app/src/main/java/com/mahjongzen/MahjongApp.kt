package com.mahjongzen

import android.app.Application
import com.mahjongzen.audio.SoundManager
import com.mahjongzen.data.GameRepository
import com.mahjongzen.data.db.AppDatabase
import com.mahjongzen.data.settings.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Simple manual dependency container (no DI framework needed for an app of this size). */
class AppContainer(app: Application) {
    /** Scope that outlives screens – used for saving so writes are never cancelled. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database = AppDatabase.create(app)
    val settings = SettingsRepository(app)
    val games = GameRepository(database)
    val sound = SoundManager(app, appScope)

    /** Database writes are queued and executed strictly in order on one IO coroutine. */
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        appScope.launch(Dispatchers.IO) {
            for (w in writes) runCatching { w() }
        }
    }

    fun write(block: suspend () -> Unit) {
        writes.trySend(block)
    }

    /** Runs [block] after all pending writes, so reads always see the latest saved state. */
    suspend fun <T> read(block: suspend () -> T): T {
        val result = CompletableDeferred<T>()
        writes.send {
            try { result.complete(block()) } catch (e: Throwable) { result.completeExceptionally(e) }
        }
        return result.await()
    }
}

class MahjongApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.sound.init()
        container.appScope.launch {
            container.settings.settings.collect { s ->
                container.sound.effectsEnabled = s.soundOn
                container.sound.effectsVolume = s.soundVolume
                container.sound.setMusic(s.musicOn, s.musicVolume)
            }
        }
    }
}
