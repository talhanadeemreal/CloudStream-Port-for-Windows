package com.lagradost.cloudstream3.plugins

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvSeriesSearchResponse
import com.lagradost.cloudstream3.MovieSearchResponse
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * Plugin loader for CloudStream extensions
 */
object PluginLoader {
    
    data class PluginInfo(
        val name: String,
        val version: String,
        val file: File,
        val provider: MainAPI? = null
    )
    
    private val loadedPlugins = mutableListOf<PluginInfo>()
    
    /**
     * Get extensions directory from AppData
     */
    private fun getExtensionsDir(): File {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "CloudStream/extensions")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Load all JAR files from extensions directory
     */
    fun loadAllPlugins(): List<PluginInfo> {
        loadedPlugins.clear()
        
        val extensionsDir = getExtensionsDir()
        val jarFiles = extensionsDir.listFiles { file ->
            file.extension.equals("jar", ignoreCase = true)
        } ?: emptyArray()
        
        for (jarFile in jarFiles) {
            try {
                val provider = loadProviderFromJar(jarFile)
                
                loadedPlugins.add(
                    PluginInfo(
                        name = provider?.name ?: jarFile.nameWithoutExtension,
                        version = "1.0.0",
                        file = jarFile,
                        provider = provider
                    )
                )
            } catch (e: Exception) {
                println("Failed to load plugin: ${jarFile.name} - ${e.message}")
                e.printStackTrace()
            }
        }
        
        return loadedPlugins.toList()
    }
    
    private fun loadProviderFromJar(file: File): MainAPI? {
        val urls = arrayOf(file.toURI().toURL())
        val classLoader = URLClassLoader(urls, PluginLoader::class.java.classLoader)
        
        val jarFile = JarFile(file)
        val entries = jarFile.entries()
        
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory || !entry.name.endsWith(".class")) continue
            
            // Convert path to class name (e.g., com/example/MyClass.class -> com.example.MyClass)
            val className = entry.name.substring(0, entry.name.length - 6).replace('/', '.')
            
            try {
                val clazz = classLoader.loadClass(className)
                if (MainAPI::class.java.isAssignableFrom(clazz) && !clazz.isInterface && !java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) {
                    // Found a MainAPI implementation!
                    return clazz.getDeclaredConstructor().newInstance() as MainAPI
                }
            } catch (e: Throwable) {
                // Ignore classes that fail to load (might depend on Android classes)
            }
        }
        return null
    }
    
    /**
     * Search across all loaded providers
     */
    suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        
        for (plugin in loadedPlugins) {
            plugin.provider?.let { api ->
                try {
                    val response = api.search(query)
                    if (response != null) {
                        results.addAll(response)
                    }
                } catch (e: Exception) {
                    println("Error searching in ${api.name}: ${e.message}")
                }
            }
        }
        return results
    }
    
    /**
     * Get all loaded plugins
     */
    fun getLoadedPlugins(): List<PluginInfo> = loadedPlugins.toList()
    
    /**
     * Delete a plugin
     */
    fun deletePlugin(plugin: PluginInfo) {
        val file = plugin.file
        if (file.exists()) {
            // Try to force close ClassLoader if possible, but for now just try delete
            // Note: On Windows, open JARs might be locked. 
            // In a real app, we'd need a robust "delete on exit" or custom ClassLoader management.
            loadedPlugins.remove(plugin)
            val deleted = file.delete()
            if (!deleted) {
                file.deleteOnExit()
                // If we can't delete it immediately, we should at least remove it from the list
                // and maybe rename it to .tmp_del to create a "pending" state?
                try {
                    val temp = File(file.parent, file.name + ".del")
                    file.renameTo(temp)
                    temp.deleteOnExit()
                } catch (e: Exception) {
                    println("Could not mark ${file.name} for deletion")
                }
            }
        }
    }
    
    /**
     * Get extensions directory path for display
     */
    fun getExtensionsPath(): String = getExtensionsDir().absolutePath
}
