package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.player.ExternalPlayerBridge
import java.util.prefs.Preferences

/**
 * simple settings manager using Java Preferences API
 */
object SettingsManager {
    private val prefs = Preferences.userNodeForPackage(SettingsManager::class.java)
    
    private const val KEY_DEFAULT_PLAYER = "default_player"
    private const val KEY_THEME = "theme"
    
    // Default Player
    fun getDefaultPlayer(): ExternalPlayerBridge.Player {
        val name = prefs.get(KEY_DEFAULT_PLAYER, ExternalPlayerBridge.Player.MPV.name)
        return try {
            ExternalPlayerBridge.Player.valueOf(name)
        } catch (e: Exception) {
            ExternalPlayerBridge.Player.MPV
        }
    }
    
    fun setDefaultPlayer(player: ExternalPlayerBridge.Player) {
        prefs.put(KEY_DEFAULT_PLAYER, player.name)
    }
    
    // Theme (future proofing)
    fun isDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_THEME, true)
    }
    
    fun setDarkTheme(isDark: Boolean) {
        prefs.putBoolean(KEY_THEME, isDark)
    }
}
