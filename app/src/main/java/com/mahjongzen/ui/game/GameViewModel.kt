package com.mahjongzen.ui.game

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mahjongzen.AppContainer
import com.mahjongzen.game.engine.GameEngine
import com.mahjongzen.game.engine.GameState
import com.mahjongzen.game.engine.Seeds
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.Layout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** One-shot events for sounds, haptics and animations. */
sealed interface GameEvent {
    data class Selected(val tile: Int) : GameEvent
    data class Invalid(val tile: Int) : GameEvent
    data class Matched(val a: Int, val b: Int, val faceA: Int, val faceB: Int, val points: Int, val combo: Int) : GameEvent
    data class Hint(val a: Int, val b: Int) : GameEvent
    data object NoHintsLeft : GameEvent
    data class Shuffled(val oldFaces: List<Int>, val newFaces: List<Int>, val solvable: Boolean) : GameEvent
    data object Undone : GameEvent
    data object Won : GameEvent
    data object Blocked : GameEvent
}

data class GameUiState(
    val loading: Boolean = true,
    val game: GameState? = null,
    val layout: Layout? = null,
    val free: BooleanArray = BooleanArray(0),
    val selected: Int = -1,
    val hint: Pair<Int, Int>? = null,
    val availablePairs: Int = 0,
    val hintsRemaining: Int? = null,
    val paused: Boolean = false,
    val blocked: Boolean = false,
    val bestScore: Int? = null,
    val newRecord: Boolean = false,
    val elapsedSeconds: Long = 0,
    /** true while the shuffle animation runs – all input is locked. */
    val shuffling: Boolean = false,
) {
    val won: Boolean get() = game?.won == true
}

class GameViewModel(
    private val container: AppContainer,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repo = container.games
    private var engine: GameEngine? = null
    private var engineKey: Triple<LayoutType, Difficulty, Long>? = null

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    // ---- timer ----
    private var foreground = true
    private var elapsedMs = 0L
    private var lastTick = SystemClock.elapsedRealtime()
    private var lastAutoSave = 0L
    private var reduceMotion = false

    init {
        val mode = savedStateHandle.get<String>(ARG_MODE) ?: MODE_CONTINUE
        val layout = savedStateHandle.get<String>(ARG_LAYOUT)
        val difficulty = savedStateHandle.get<String>(ARG_DIFFICULTY)
        val seed = savedStateHandle.get<Long>(ARG_SEED) ?: 0L
        viewModelScope.launch { start(mode, layout, difficulty, seed) }
        viewModelScope.launch { timerLoop() }
        viewModelScope.launch { container.settings.settings.collect { reduceMotion = it.reduceMotion } }
    }

    // ------------------------------------------------------------ start / setup

    private suspend fun start(mode: String, layoutName: String?, difficultyName: String?, seedArg: Long) {
        val settings = container.settings.settings.first()
        val state: GameState = when (mode) {
            MODE_CONTINUE -> container.read { repo.load() }?.takeIf { !it.won }
                ?: generate(LayoutType.TURTLE, settings.defaultDifficulty, Seeds.random(), false)
            MODE_DAILY -> {
                val daily = Seeds.daily(LocalDate.now())
                val saved = container.read { repo.load() }
                if (saved != null && saved.daily && saved.seed == daily.seed && !saved.won) saved
                else {
                    abandonSaved()
                    generate(daily.layout, daily.difficulty, daily.seed, true)
                }
            }
            else -> {
                abandonSaved()
                val type = layoutName?.let { n -> LayoutType.entries.firstOrNull { it.name == n } } ?: LayoutType.TURTLE
                val diff = difficultyName?.let { n -> Difficulty.entries.firstOrNull { it.name == n } } ?: settings.defaultDifficulty
                generate(type, diff, if (seedArg > 0) seedArg else Seeds.random(), false)
            }
        }
        // After process death the screen is recreated with the same arguments:
        // from now on it must continue the saved game instead of starting a new one.
        savedStateHandle[ARG_MODE] = MODE_CONTINUE
        setGame(state)
    }

    private suspend fun generate(type: LayoutType, difficulty: Difficulty, seed: Long, daily: Boolean): GameState =
        withContext(Dispatchers.Default) {
            val e = GameEngine.create(type, difficulty, seed)
            engine = e
            engineKey = Triple(type, difficulty, seed)
            GameEngine.newState(type, difficulty, seed, daily, e.layout)
        }

    /** A game in progress that gets replaced counts as played (lost). */
    private suspend fun abandonSaved() = container.read {
        val saved = repo.load()
        if (saved != null && !saved.won && saved.moves > 0) repo.record(saved, won = false)
        repo.clearSaved()
    }

    private suspend fun setGame(state: GameState) {
        val key = Triple(state.layoutType, state.difficulty, state.seed)
        val e = engine?.takeIf { engineKey == key } ?: withContext(Dispatchers.Default) { GameEngine.forState(state) }
        engine = e
        engineKey = key
        elapsedMs = state.elapsedMs
        lastTick = SystemClock.elapsedRealtime()
        val best = repo.bestScore(state.layoutType, state.difficulty)
        _ui.value = derive(state, e).copy(loading = false, bestScore = best, paused = false, newRecord = false)
        save(state)
        if (_ui.value.blocked) _events.tryEmit(GameEvent.Blocked)
    }

    private fun derive(state: GameState, e: GameEngine, selected: Int = -1, hint: Pair<Int, Int>? = null): GameUiState {
        val pairs = e.countAvailablePairs(state)
        return _ui.value.copy(
            game = state,
            layout = e.layout,
            free = e.freeMask(state),
            selected = selected,
            hint = hint,
            availablePairs = pairs,
            hintsRemaining = e.hintsRemaining(state),
            blocked = !state.won && state.remaining > 0 && pairs == 0,
            elapsedSeconds = state.elapsedMs / 1000,
        )
    }

    // ------------------------------------------------------------ input

    fun onTileTapped(i: Int) {
        val ui = _ui.value
        val s = ui.game ?: return
        val e = engine ?: return
        if (ui.loading || ui.paused || ui.shuffling || s.won || i !in ui.free.indices || s.faces[i] < 0) return
        if (!ui.free[i]) {
            _events.tryEmit(GameEvent.Invalid(i))
            return
        }
        val sel = ui.selected
        when {
            sel == i -> _ui.update { it.copy(selected = -1) }
            sel >= 0 && e.canMatch(s, sel, i) -> doMatch(s, e, sel, i)
            else -> {
                _ui.update { it.copy(selected = i, hint = null) }
                _events.tryEmit(GameEvent.Selected(i))
            }
        }
    }

    private fun doMatch(s: GameState, e: GameEngine, a: Int, b: Int) {
        val now = currentElapsed()
        val outcome = e.match(s.copy(elapsedMs = now), a, b, now) ?: return
        val next = outcome.state
        _ui.value = derive(next, e)
        _events.tryEmit(GameEvent.Matched(a, b, s.faces[a], s.faces[b], outcome.points, outcome.combo))
        when {
            next.won -> onWon(next)
            _ui.value.blocked -> {
                save(next)
                _events.tryEmit(GameEvent.Blocked)
            }
            else -> save(next)
        }
    }

    private fun onWon(state: GameState) {
        val best = _ui.value.bestScore
        val record = best == null || state.score > best
        _ui.update { it.copy(newRecord = record, bestScore = if (record) state.score else best) }
        container.write {
            repo.record(state, won = true)
            repo.clearSaved()
        }
        _events.tryEmit(GameEvent.Won)
    }

    fun hint() {
        val s = _ui.value.game ?: return
        val e = engine ?: return
        if (_ui.value.paused || _ui.value.shuffling || s.won) return
        val outcome = e.hint(s.copy(elapsedMs = currentElapsed()))
        if (outcome == null) {
            _events.tryEmit(GameEvent.NoHintsLeft)
            return
        }
        _ui.value = derive(outcome.state, e, hint = outcome.pair)
        _events.tryEmit(GameEvent.Hint(outcome.pair.first, outcome.pair.second))
        save(outcome.state)
    }

    fun shuffle() {
        val s = _ui.value.game ?: return
        val e = engine ?: return
        if (s.won || _ui.value.loading || _ui.value.shuffling) return
        _ui.update { it.copy(shuffling = true, selected = -1, hint = null) } // button disabled immediately
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.Default) { e.shuffle(s.copy(elapsedMs = currentElapsed())) }
            // The new arrangement is saved right away – closing the app mid-animation is safe.
            save(outcome.state)
            _ui.value = derive(outcome.state, e).copy(shuffling = true)
            _events.tryEmit(GameEvent.Shuffled(s.faces, outcome.state.faces, outcome.solvable))
            delay(ShuffleTiming.total(outcome.state.remaining, reduceMotion) / 1_000_000L)
            _ui.update { it.copy(shuffling = false) }
            if (_ui.value.blocked) _events.tryEmit(GameEvent.Blocked)
        }
    }

    fun undo() {
        val s = _ui.value.game ?: return
        val e = engine ?: return
        if (_ui.value.shuffling) return
        val next = e.undo(s.copy(elapsedMs = currentElapsed())) ?: return
        _ui.value = derive(next, e)
        _events.tryEmit(GameEvent.Undone)
        save(next)
    }

    /** Same board again from the start. */
    fun restart() {
        val s = _ui.value.game ?: return
        val e = engine ?: return
        _ui.update { it.copy(loading = true, paused = false) }
        viewModelScope.launch {
            if (!s.won && s.moves > 0) container.write { repo.record(s.copy(elapsedMs = currentElapsed()), won = false) }
            val fresh = withContext(Dispatchers.Default) { e.restart(s) }
            setGame(fresh)
        }
    }

    /** New random board with the same layout and difficulty. */
    fun newGame() {
        val s = _ui.value.game ?: return
        _ui.update { it.copy(loading = true, paused = false) }
        viewModelScope.launch {
            if (!s.won && s.moves > 0) container.write { repo.record(s.copy(elapsedMs = currentElapsed()), won = false) }
            setGame(generate(s.layoutType, s.difficulty, Seeds.random(), false))
        }
    }

    fun pause() {
        if (_ui.value.game?.won == true) return
        persistElapsed()
        _ui.update { it.copy(paused = true, selected = -1) }
    }

    fun resume() {
        lastTick = SystemClock.elapsedRealtime()
        _ui.update { it.copy(paused = false) }
    }

    fun setForeground(value: Boolean) {
        if (foreground == value) return
        foreground = value
        lastTick = SystemClock.elapsedRealtime()
        if (!value) persistElapsed()
    }

    // ------------------------------------------------------------ timer & saving

    private fun timerRunning(): Boolean {
        val ui = _ui.value
        return foreground && !ui.loading && !ui.paused && !ui.blocked && ui.game?.won == false
    }

    private fun currentElapsed(): Long {
        val now = SystemClock.elapsedRealtime()
        if (timerRunning()) elapsedMs += now - lastTick
        lastTick = now
        return elapsedMs
    }

    private suspend fun timerLoop() {
        while (viewModelScope.isActive) {
            delay(250)
            val t = currentElapsed()
            val secs = t / 1000
            val ui = _ui.value
            val g = ui.game ?: continue
            if (secs != ui.elapsedSeconds && !g.won) {
                _ui.update { it.copy(elapsedSeconds = secs, game = it.game?.copy(elapsedMs = t)) }
            }
            if (timerRunning() && t - lastAutoSave > 15_000) {
                lastAutoSave = t
                save(_ui.value.game ?: continue)
            }
        }
    }

    private fun persistElapsed() {
        val g = _ui.value.game ?: return
        if (g.won) return
        val updated = g.copy(elapsedMs = currentElapsed())
        _ui.update { it.copy(game = updated) }
        save(updated)
    }

    private fun save(state: GameState) {
        if (state.won) return
        container.write { repo.save(state) }
    }

    override fun onCleared() {
        persistElapsed()
        super.onCleared()
    }

    companion object {
        const val ARG_MODE = "mode"
        const val ARG_LAYOUT = "layout"
        const val ARG_DIFFICULTY = "difficulty"
        const val ARG_SEED = "seed"
        const val MODE_CONTINUE = "continue"
        const val MODE_NEW = "new"
        const val MODE_DAILY = "daily"

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(container, createSavedStateHandle()) }
        }
    }
}
