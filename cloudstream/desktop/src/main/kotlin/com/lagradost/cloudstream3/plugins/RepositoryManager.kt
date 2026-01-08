package com.lagradost.cloudstream3.plugins

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

object RepositoryManager {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .build()
    private val mapper = jacksonObjectMapper().apply {
        // Ignore unknown properties in JSON (like apiVersion, repositoryUrl, etc.)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    // Shortcode mapping
    private val PREDEFINED_REPOSITORIES = mapOf(
        "cs-main" to "https://codeberg.org/cloudstream/cloudstream-extensions/raw/branch/builds/repo.json",
        "cs-english" to "https://codeberg.org/cloudstream/cloudstream-extensions-multilingual/raw/branch/builds/repo.json",
        "cs-hexated" to "https://codeberg.org/Hexated/cloudstream-extensions-hexated/raw/branch/builds/repo.json",
        "megarepo" to "https://raw.githubusercontent.com/self-similarity/MegaRepo/builds/repo.json"
    )

    data class Repository(
        @JsonProperty("name") val name: String,
        @JsonProperty("description") val description: String?,
        @JsonProperty("manifestVersion") val manifestVersion: Int?,
        @JsonProperty("pluginUrl") val pluginUrl: String?,
        @JsonProperty("pluginLists") val pluginLists: List<String>? = null
    ) {
        // Internal use, set manually after fetching
        var url: String = ""
    }

    data class SitePlugin(
        @JsonProperty("internalName") val internalName: String,
        @JsonProperty("version") val version: Int,
        @JsonProperty("url") val url: String,
        @JsonProperty("iconUrl") val iconUrl: String?,
        @JsonProperty("authors") val authors: List<String>?,
        @JsonProperty("description") val description: String?,
        @JsonProperty("tvTypes") val tvTypes: List<String>?
    )

    private val repositories = mutableListOf<Repository>()
    
    // Persistence file
    private fun getReposFile(): File {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "CloudStream")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "repositories.json")
    }
    
    init {
        // Load persisted repos on startup
        loadRepositories()
    }
    
    private fun saveRepositories() {
        try {
            val file = getReposFile()
            // Create a simplified list for JSON (just the URLs, we can re-fetch on load)
            val repoUrls = repositories.map { it.url }
            file.writeText(mapper.writeValueAsString(repoUrls))
            println("Saved ${repoUrls.size} repositories to ${file.absolutePath}")
        } catch (e: Exception) {
            println("Failed to save repositories: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun loadRepositories() {
        try {
            val file = getReposFile()
            if (!file.exists()) {
                println("No saved repositories found")
                return
            }
            val repoUrls = mapper.readValue<List<String>>(file.readText())
            println("Loading ${repoUrls.size} repositories from ${file.absolutePath}")
            
            // Re-fetch each repo to get full metadata
            repoUrls.forEach { url ->
                try {
                    val repo = fetchRepository(url)
                    repositories.add(repo)
                    println("Loaded repository: ${repo.name}")
                } catch (e: Exception) {
                    println("Failed to load repository $url: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Failed to load repositories: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getRepositories(): List<Repository> = repositories.toList()

    fun addRepository(shortcodeOrUrl: String) {
        var url = PREDEFINED_REPOSITORIES[shortcodeOrUrl] ?: shortcodeOrUrl
        
        // Handle cloudstreamrepo:// scheme
        if (url.startsWith("cloudstreamrepo://")) {
            url = url.replace("cloudstreamrepo://", "https://")
        }
        
        // Basic validation
        if (!url.startsWith("http")) {
            throw IllegalArgumentException("Invalid URL or unknown shortcode: $shortcodeOrUrl")
        }
        
        // Check if already added
        if (repositories.any { it.url == url }) {
            println("Repository already added: $url")
            return
        }

        try {
            val repoData = fetchRepository(url)
            repositories.add(repoData)
            saveRepositories() // Persist after adding
            println("Added repository: ${repoData.name}")
        } catch (e: Exception) {
            println("Failed to add repository: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun fetchRepository(url: String): Repository {
        println("Fetching repository from: $url")
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            println("Response code: ${response.code}")
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            
            val body = response.body?.string() ?: throw IOException("Empty body")
            println("Response body length: ${body.length}")
            val repo = mapper.readValue<Repository>(body)
            
            // Normalize plugin URL logic
            var rawPluginUrl = repo.pluginUrl
            if (rawPluginUrl == null && !repo.pluginLists.isNullOrEmpty()) {
                rawPluginUrl = repo.pluginLists[0]
            }

            val finalPluginUrl = if (rawPluginUrl != null && !rawPluginUrl.startsWith("http")) {
                 url.substringBeforeLast("/") + "/" + rawPluginUrl
            } else {
                rawPluginUrl ?: (url.substringBeforeLast("/") + "/plugins.json")
            }
            
            repo.url = url
            println("Loaded repository: ${repo.name}, pluginUrl: $finalPluginUrl")
            return repo
        }
    }

    fun fetchPlugins(repo: Repository): List<SitePlugin> {
        var rawPluginUrl = repo.pluginUrl
        if (rawPluginUrl == null && !repo.pluginLists.isNullOrEmpty()) {
            rawPluginUrl = repo.pluginLists[0]
        }
        
        val url = if (rawPluginUrl != null && !rawPluginUrl.startsWith("http")) {
            repo.url.substringBeforeLast("/") + "/" + rawPluginUrl
        } else {
            rawPluginUrl ?: (repo.url.substringBeforeLast("/") + "/plugins.json")
        }
        
        println("Fetching plugins from: $url")
        val request = Request.Builder().url(url).build()
        
        try {
            client.newCall(request).execute().use { response ->
                println("Plugins response code: ${response.code}")
                if (!response.isSuccessful) {
                    val error = "Failed to fetch plugins: HTTP ${response.code}"
                    println(error)
                    throw IOException(error)
                }
                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    println("Empty plugins response")
                    return emptyList()
                }
                println("Plugins response body length: ${body.length}")
                val plugins = mapper.readValue<List<SitePlugin>>(body)
                println("Parsed ${plugins.size} plugins from ${repo.name}")
                return plugins
            }
        } catch (e: Exception) {
            println("Error fetching plugins from ${repo.name}: ${e.message}")
            e.printStackTrace()
            throw e // Re-throw so UI can handle
        }
    }
}
