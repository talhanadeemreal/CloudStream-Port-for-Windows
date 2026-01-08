package com.lagradost.cloudstream3.player

import java.io.File

/**
 * External player detection and launcher
 */
object ExternalPlayerBridge {
    
    enum class Player {
        MPV, VLC, SYSTEM_DEFAULT
    }
    
    /**
     * Detect available players
     */
    fun detectPlayers(): List<Player> {
        val available = mutableListOf<Player>()
        
        if (isMpvAvailable()) available.add(Player.MPV)
        if (isVlcAvailable()) available.add(Player.VLC)
        available.add(Player.SYSTEM_DEFAULT)
        
        return available
    }
    
    private fun isMpvAvailable(): Boolean {
        return findExecutable("mpv.exe") != null || findExecutable("mpv") != null
    }
    
    private fun isVlcAvailable(): Boolean {
        return findExecutable("vlc.exe") != null
    }
    
    private fun findExecutable(name: String): File? {
        // Check common installation paths
        val commonPaths = listOf(
            "C:\\Program Files\\VideoLAN\\VLC\\",
            "C:\\Program Files (x86)\\VideoLAN\\VLC\\",
            "C:\\Program Files\\mpv\\",
            System.getenv("LOCALAPPDATA") + "\\Microsoft\\WinGet\\Packages\\"
        )
        
        for (path in commonPaths) {
            val file = File(path, name)
            if (file.exists()) return file
        }
        
        // Check PATH
        val pathEnv = System.getenv("PATH") ?: return null
        for (dir in pathEnv.split(";")) {
            val file = File(dir, name)
            if (file.exists()) return file
        }
        
        return null
    }
    
    /**
     * Launch external player with URL
     */
    fun launchPlayer(
        player: Player,
        url: String,
        title: String = "CloudStream",
        headers: Map<String, String> = emptyMap()
    ): Boolean {
        return try {
            val command = when (player) {
                Player.MPV -> buildMpvCommand(url, title, headers)
                Player.VLC -> buildVlcCommand(url, title, headers)
                Player.SYSTEM_DEFAULT -> buildSystemCommand(url)
            }
            
            if (command.isEmpty()) return false
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.start()
            
            println("Launched ${player.name}: ${command.joinToString(" ")}")
            true
        } catch (e: Exception) {
            println("Failed to launch player: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    private fun buildMpvCommand(
        url: String,
        title: String,
        headers: Map<String, String>
    ): List<String> {
        val mpvPath = findExecutable("mpv.exe") ?: findExecutable("mpv") ?: return emptyList()
        
        val command = mutableListOf(
            mpvPath.absolutePath,
            url,
            "--force-window=immediate",
            "--title=$title"
        )
        
        // Add headers if provided
        if (headers.isNotEmpty()) {
            val headerString = headers.entries.joinToString(",") { "${it.key}: ${it.value}" }
            command.add("--http-header-fields=$headerString")
        }
        
        return command
    }
    
    private fun buildVlcCommand(
        url: String,
        title: String,
        headers: Map<String, String>
    ): List<String> {
        val vlcPath = findExecutable("vlc.exe") ?: return emptyList()
        
        val command = mutableListOf(
            vlcPath.absolutePath
        )
        
        // Don't add URL directly for YouTube/special URLs - VLC might not handle them
        // For direct streams, add the URL
        if (url.startsWith("http://") || url.startsWith("https://")) {
            command.add(url)
            command.add("--play-and-exit")  // Close after playback
        } else {
            // Local file
            command.add(url)
        }
        
        command.add("--meta-title=$title")
        
        // VLC header support is limited, add User-Agent if provided
        headers["User-Agent"]?.let { ua ->
            command.add(":http-user-agent=$ua")
        }
        
        headers["Referer"]?.let { referer ->
            command.add(":http-referrer=$referer")
        }
        
        return command
    }
    
    private fun buildSystemCommand(url: String): List<String> {
        return when {
            System.getProperty("os.name").contains("Windows", ignoreCase = true) -> {
                listOf("cmd", "/c", "start", "\"\"", url)
            }
            else -> emptyList()
        }
    }
}
