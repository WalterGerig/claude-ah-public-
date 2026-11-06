package com.mahjongzen.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mahjongzen.game.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings persisted with Jetpack DataStore. */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    private object Keys {
        val soundOn = booleanPreferencesKey("sound_on")
        val soundVolume = floatPreferencesKey("sound_volume")
        val musicOn = booleanPreferencesKey("music_on")
        val musicVolume = floatPreferencesKey("music_volume")
        val haptics = booleanPreferencesKey("haptics")
        val background = stringPreferencesKey("background")
        val tileStyle = stringPreferencesKey("tile_style")
        val highlightFree = booleanPreferencesKey("highlight_free")
        val showTimer = booleanPreferencesKey("show_timer")
        val defaultDifficulty = stringPreferencesKey("default_difficulty")
        val themeMode = stringPreferencesKey("theme_mode")
        val largeTiles = booleanPreferencesKey("large_tiles")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            soundOn = p[Keys.soundOn] ?: d.soundOn,
            soundVolume = p[Keys.soundVolume] ?: d.soundVolume,
            musicOn = p[Keys.musicOn] ?: d.musicOn,
            musicVolume = p[Keys.musicVolume] ?: d.musicVolume,
            haptics = p[Keys.haptics] ?: d.haptics,
            background = enumOr(p[Keys.background], d.background),
            tileStyle = enumOr(p[Keys.tileStyle], d.tileStyle),
            highlightFree = p[Keys.highlightFree] ?: d.highlightFree,
            showTimer = p[Keys.showTimer] ?: d.showTimer,
            defaultDifficulty = enumOr(p[Keys.defaultDifficulty], d.defaultDifficulty),
            themeMode = enumOr(p[Keys.themeMode], d.themeMode),
            largeTiles = p[Keys.largeTiles] ?: d.largeTiles,
            reduceMotion = p[Keys.reduceMotion] ?: d.reduceMotion,
        )
    }

    suspend fun setSoundOn(v: Boolean) = store.edit { it[Keys.soundOn] = v }
    suspend fun setSoundVolume(v: Float) = store.edit { it[Keys.soundVolume] = v }
    suspend fun setMusicOn(v: Boolean) = store.edit { it[Keys.musicOn] = v }
    suspend fun setMusicVolume(v: Float) = store.edit { it[Keys.musicVolume] = v }
    suspend fun setHaptics(v: Boolean) = store.edit { it[Keys.haptics] = v }
    suspend fun setBackground(v: TableBackground) = store.edit { it[Keys.background] = v.name }
    suspend fun setTileStyle(v: TileStyle) = store.edit { it[Keys.tileStyle] = v.name }
    suspend fun setHighlightFree(v: Boolean) = store.edit { it[Keys.highlightFree] = v }
    suspend fun setShowTimer(v: Boolean) = store.edit { it[Keys.showTimer] = v }
    suspend fun setDefaultDifficulty(v: Difficulty) = store.edit { it[Keys.defaultDifficulty] = v.name }
    suspend fun setThemeMode(v: ThemeMode) = store.edit { it[Keys.themeMode] = v.name }
    suspend fun setLargeTiles(v: Boolean) = store.edit { it[Keys.largeTiles] = v }
    suspend fun setReduceMotion(v: Boolean) = store.edit { it[Keys.reduceMotion] = v }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}
