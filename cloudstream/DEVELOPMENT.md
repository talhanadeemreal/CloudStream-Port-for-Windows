# Development Guide

## Architecture Overview

### Application Structure

```
Main.kt (Entry Point)
    ↓
Window (Compose Desktop)
    ↓
NavigationState → Screens
    ↓
    ├── HomeScreen
    ├── SearchScreen → PluginLoader.search()
    ├── DetailsScreen → ExternalPlayerBridge
    ├── ExtensionsScreen → RepositoryManager + PluginDownloader
    └── SettingsScreen → SettingsManager
```

### Module Organization

#### Library Module (`library/`)
Shared Kotlin Multiplatform code:

```kotlin
// Core interfaces
interface MainAPI {
    val name: String
    suspend fun search(query: String): List<SearchResponse>?
    suspend fun load(url: String): LoadResponse?
}

// Data models
data class SearchResponse(val name: String, val url: String, ...)
data class LoadResponse(...)
```

**Purpose**: Define contracts that plugins must implement.

#### Desktop Module (`desktop/`)
Platform-specific implementation:

```
src/main/kotlin/com/lagradost/cloudstream3/
├── Main.kt                    # Entry point, navigation
├── ui/
│   └── screens/              # Compose screens
│       ├── HomeScreen.kt
│       ├── SearchScreen.kt
│       ├── DetailsScreen.kt
│       ├── ExtensionsScreen.kt
│       └── SettingsScreen.kt
├── plugins/
│   ├── PluginLoader.kt       # JAR class loading
│   ├── RepositoryManager.kt  # Repo fetching/persistence
│   └── PluginDownloader.kt   # Extension downloads
├── player/
│   └── ExternalPlayerBridge.kt # MPV/VLC launching
└── utils/
    └── SettingsManager.kt    # Java Preferences API
```

---

## Key Components Deep Dive

### 1. Main.kt - Application Entry

```kotlin
fun main() = application {
    var navigationState by remember { mutableStateOf(NavigationState.Home) }
    
    Window(...) {
        MaterialTheme {
            Row {
                NavigationRail { ... }
                when (navigationState) {
                    NavigationState.Home -> HomeScreen()
                    NavigationState.Search -> SearchScreen(onResultClick = { ... })
                    // ...
                }
            }
        }
    }
    
    // Load plugins on startup
    LaunchedEffect(Unit) {
        PluginLoader.loadAllPlugins()
    }
}
```

**Key Concepts**:
- `navigationState`: Top-level navigation state
- `LaunchedEffect`: Executes once on app startup
- Compose `@Composable` functions for UI

---

### 2. PluginLoader.kt - Plugin System Core

```kotlin
object PluginLoader {
    data class PluginInfo(
        val name: String,
        val version: String,
        val file: File,
        val provider: MainAPI? = null
    )
    
    private val loadedPlugins = mutableListOf<PluginInfo>()
    
    fun loadAllPlugins(): List<PluginInfo> {
        val extensionsDir = getExtensionsDir()
        val jarFiles = extensionsDir.listFiles { it.extension == "jar" }
        
        jarFiles.forEach { file ->
            val provider = loadProviderFromJar(file)
            loadedPlugins.add(PluginInfo(..., provider))
        }
        
        return loadedPlugins
    }
    
    private fun loadProviderFromJar(file: File): MainAPI? {
        val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()))
        val jarFile = JarFile(file)
        
        jarFile.entries().asSequence().forEach { entry ->
            if (entry.name.endsWith(".class")) {
                val className = entry.name.replace("/", ".").removeSuffix(".class")
                try {
                    val clazz = classLoader.loadClass(className)
                    if (MainAPI::class.java.isAssignableFrom(clazz)) {
                        return clazz.getDeclaredConstructor().newInstance() as MainAPI
                    }
                } catch (e: Throwable) {
                    // Ignore classes that depend on Android APIs
                }
            }
        }
        return null
    }
}
```

**How It Works**:
1. Scan `%AppData%/CloudStream/extensions/` for `.jar` files
2. Create `URLClassLoader` for each JAR
3. Iterate through all `.class` files in JAR
4. Check if class implements `MainAPI`
5. Instantiate and return provider
6. Handle errors for Android-dependent classes

**Limitations**:
- Classes using Android `Context` will fail to load
- No dependency resolution between plugins

---

### 3. RepositoryManager.kt - Repository System

```kotlin
object RepositoryManager {
    data class Repository(
        val name: String,
        val description: String?,
        val pluginUrl: String?,
        val pluginLists: List<String>? = null
    ) {
        var url: String = ""  // Set after fetching
    }
    
    private val repositories = mutableListOf<Repository>()
    private val mapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
    
    init {
        loadRepositories()  // Load from %AppData%/CloudStream/repositories.json
    }
    
    fun addRepository(shortcodeOrUrl: String) {
        val url = PREDEFINED_REPOSITORIES[shortcodeOrUrl] ?: shortcodeOrUrl
        
        // Handle cloudstreamrepo:// scheme
        if (url.startsWith("cloudstreamrepo://")) {
            url = url.replace("cloudstreamrepo://", "https://")
        }
        
        val repo = fetchRepository(url)
        repositories.add(repo)
        saveRepositories()  // Persist to JSON
    }
    
    private fun fetchRepository(url: String): Repository {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty body")
        
        val repo = mapper.readValue<Repository>(body)
        repo.url = url
        return repo
    }
}
```

**Features**:
- **Shortcode Mapping**: `cs-main` → full GitHub URL
- **Persistence**: Save/load from JSON file
- **Flexible Parsing**: Ignores unknown JSON fields
- **User-Agent**: Bypasses Codeberg 403 blocks

**JSON Structure** (repo.json):
```json
{
  "name": "CloudStream Extensions",
  "description": "Official extensions",
  "manifestVersion": 1,
  "pluginUrl": "plugins.json"
}
```

---

### 4. PluginDownloader.kt - Extension Downloads

```kotlin
object PluginDownloader {
    suspend fun downloadPlugin(url: String, fileName: String): File = withContext(Dispatchers.IO) {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        val tempFile = File.createTempFile("plugin", ".cs3")
        Files.copy(response.body!!.byteStream(), tempFile.toPath(), REPLACE_EXISTING)
        
        // Check if it's actually a JAR (PK magic bytes)
        val isJar = tempFile.readBytes().let { 
            it.size >= 2 && it[0].toInt() == 0x50 && it[1].toInt() == 0x4B 
        }
        
        if (isJar) {
            // Just rename .cs3 to .jar
            Files.move(tempFile.toPath(), jarFile.toPath(), REPLACE_EXISTING)
        } else {
            // Convert DEX to JAR
            val reader = DexFileReader(tempFile)
            Dex2jar.from(reader).to(jarFile.toPath())
        }
        
        PluginLoader.loadAllPlugins()  // Hot-reload
        return jarFile
    }
}
```

**Smart Conversion**:
1. Download file to temp location
2. Check magic bytes (first 2 bytes: `PK` = ZIP/JAR)
3. If JAR: Skip conversion, just rename
4. If DEX: Use dex2jar to convert
5. Save to extensions directory
6. Reload plugins

---

### 5. ExternalPlayerBridge.kt - Player Integration

```kotlin
object ExternalPlayerBridge {
    enum class Player { MPV, VLC, SYSTEM_DEFAULT }
    
    fun detectPlayers(): List<Player> {
        val players = mutableListOf<Player>()
        
        // Check common install locations
        if (File("C:/Program Files/mpv/mpv.exe").exists()) {
            players.add(Player.MPV)
        }
        if (File("C:/Program Files/VideoLAN/VLC/vlc.exe").exists()) {
            players.add(Player.VLC)
        }
        
        players.add(Player.SYSTEM_DEFAULT)
        return players
    }
    
    fun launchPlayer(player: Player, url: String) {
        val command = when (player) {
            Player.MPV -> arrayOf("mpv", url)
            Player.VLC -> arrayOf("vlc", url)
            Player.SYSTEM_DEFAULT -> arrayOf("cmd", "/c", "start", url)
        }
        
        ProcessBuilder(*command).start()
    }
}
```

**Player Detection**:
- Checks standard Windows install paths
- Falls back to system default (uses `start` command)

**Usage**:
```kotlin
ExternalPlayerBridge.launchPlayer(Player.MPV, "http://example.com/video.mp4")
```

---

### 6. SettingsManager.kt - Preferences

```kotlin
object SettingsManager {
    private val prefs = Preferences.userNodeForPackage(SettingsManager::class.java)
    
    fun getDefaultPlayer(): String = prefs.get("default_player", "MPV")
    fun setDefaultPlayer(player: String) = prefs.put("default_player", player)
}
```

**Storage Location**:
- Windows Registry: `HKEY_CURRENT_USER\Software\JavaSoft\Prefs\cloudstream3`

---

## UI Patterns

### Compose Best Practices

```kotlin
@Composable
fun MyScreen() {
    // State
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // Layout
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search...") }
        )
        
        // Action
        Button(onClick = {
            scope.launch {
                val results = PluginLoader.search(searchQuery)
                // Update state
            }
        }) {
            Text("Search")
        }
    }
}
```

**Key Concepts**:
- `remember`: Preserves state across recompositions
- `rememberCoroutineScope`: Launch suspend functions
- `Modifier`: Styling and layout

---

## Data Flow

### Search Flow Example

```
User Input (SearchScreen)
    ↓
SearchScreen: scope.launch { PluginLoader.search(query) }
    ↓
PluginLoader: Iterate loadedPlugins
    ↓
MainAPI.search(query) for each provider
    ↓
HTTP requests to provider sites
    ↓
Parse HTML/JSON responses
    ↓
Return List<SearchResponse>
    ↓
SearchScreen: Update state, display results
    ↓
User clicks result → Navigate to DetailsScreen
```

### Download Flow Example

```
User clicks "Download" (ExtensionsScreen)
    ↓
scope.launch { PluginDownloader.downloadPlugin(url, name) }
    ↓
HTTP GET request to plugin URL
    ↓
Save to temp file
    ↓
Check file type (magic bytes)
    ↓
If JAR: Rename to .jar
If DEX: Dex2jar.from().to() conversion
    ↓
Move to %AppData%/CloudStream/extensions/
    ↓
PluginLoader.loadAllPlugins() (hot-reload)
    ↓
UI updates to show new plugin
```

---

## Error Handling

### Network Errors

```kotlin
try {
    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
        throw IOException("HTTP ${response.code}")
    }
} catch (e: IOException) {
    println("Network error: ${e.message}")
    // Show error dialog in UI
}
```

### Plugin Loading Errors

```kotlin
try {
    val provider = clazz.getDeclaredConstructor().newInstance() as MainAPI
} catch (e: NoClassDefFoundError) {
    // Android class dependency - ignore
} catch (e: Exception) {
    println("Failed to load provider: ${e.message}")
}
```

### UI Error Display

```kotlin
var showErrorDialog by remember { mutableStateOf(false) }
var errorMessage by remember { mutableStateOf("") }

// In coroutine:
try {
    doSomething()
} catch (e: Exception) {
    errorMessage = e.message ?: "Unknown error"
    showErrorDialog = true
}

// In UI:
if (showErrorDialog) {
    AlertDialog(
        onDismissRequest = { showErrorDialog = false },
        title = { Text("Error") },
        text = { Text(errorMessage) },
        confirmButton = { Button(onClick = { showErrorDialog = false }) { Text("OK") } }
    )
}
```

---

## Testing Strategies

### Manual Testing Checklist

1. **Build Test**:
   ```bash
   ./gradlew clean
   ./gradlew :desktop:packageMsi
   ```
   - Exit code 0?
   - MSI created?

2. **Plugin Loading**:
   - Place test JAR in `%AppData%/CloudStream/extensions/`
   - Run app
   - Check Extensions → Installed tab
   - Verify plugin name appears

3. **Repository System**:
   - Extensions → Browse → Add Repo → `cs-main`
   - Click "View Plugins"
   - Verify list appears
   - Download a plugin
   - Check Installed tab

4. **Search**:
   - Search tab → Enter query
   - Verify results from loaded providers
   - Click result → Details screen loads

5. **Player**:
   - Details → Enter URL
   - Select MPV/VLC
   - Click Play
   - Player launches?

### Debug Logging

All components use `println()` for logging:

```kotlin
println("Fetching repository from: $url")
println("Response code: ${response.code}")
println("Parsed ${plugins.size} plugins")
```

**View Logs**:
- Run from terminal: `./gradlew :desktop:run`
- Logs appear in console output

---

## Common Tasks

### Adding a New Screen

1. Create `MyNewScreen.kt` in `ui/screens/`:
```kotlin
@Composable
fun MyNewScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("My New Screen", style = MaterialTheme.typography.headlineMedium)
    }
}
```

2. Add to navigation in `Main.kt`:
```kotlin
enum class NavigationState {
    Home, Search, Extensions, Settings, MyNew
}

when (navigationState) {
    NavigationState.MyNew -> MyNewScreen()
}
```

3. Add navigation rail item:
```kotlin
NavigationRailItem(
    selected = navigationState == NavigationState.MyNew,
    onClick = { navigationState = NavigationState.MyNew },
    icon = { Icon(Icons.Default.Star, "My New") }
)
```

### Adding a New Dependency

1. Find dependency on [Maven Central](https://search.maven.org/)

2. Add to `gradle/libs.versions.toml`:
```toml
[versions]
mylib = "1.0.0"

[libraries]
mylib = { module = "com.example:mylib", version.ref = "mylib" }
```

3. Add to `desktop/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.mylib)
}
```

4. Sync: `./gradlew --refresh-dependencies`

### Debugging ClassNotFoundException

1. Check JAR exists in extensions directory
2. Verify JAR structure with `jar tf myplug.jar`
3. Confirm class implements `MainAPI`
4. Check for Android dependencies (will cause silent failure)
5. Add debug logging in `loadProviderFromJar()`

---

## Performance Optimization

### Current Bottlenecks

1. **Plugin Loading**: Synchronous, blocks UI
   - **Solution**: Load in background with progress indicator

2. **Repository Fetching**: Blocking network calls
   - **Solution**: Use OkHttp async API (`enqueue()`)

3. **Image Loading**: No cache size limit
   - **Solution**: Configure Coil disk cache

### Optimization Tips

```kotlin
// Use IO dispatcher for blocking operations
withContext(Dispatchers.IO) {
    val result = blockingOperation()
}

// Lazy loading for composables
val lazyListState = rememberLazyListState()
LazyColumn(state = lazyListState) {
    items(largeList) { item ->
        // Only rendered items are composed
    }
}

// Memoize expensive computations
val processedData = remember(rawData) {
    expensiveTransformation(rawData)
}
```

---

## Security Considerations

### Plugin Sandboxing

**Current State**: Plugins run with full JVM permissions  
**Risk**: Malicious plugins can access file system, network

**Future**:
- Implement `SecurityManager`
- Restrict file access to extensions directory
- Network access whitelisting

### User Data

**Stored Data**:
- Repository URLs (plaintext)
- Player preferences (plaintext)
- No passwords or sensitive data

---

## Contributing Guidelines

### Code Style

```kotlin
// Class names: PascalCase
class MyClass

// Functions: camelCase
fun myFunction()

// Constants: UPPER_SNAKE_CASE
const val MY_CONSTANT = "value"

// Private properties: camelCase with underscore
private val _myState = mutableStateOf("")
```

### Commit Messages

```
feat: Add HLS download support
fix: Resolve DEX conversion error for .cs3 files
docs: Update README with build instructions
refactor: Extract player detection to separate class
```

### Pull Request Process

1. Fork repository
2. Create feature branch
3. Make changes with clear commits
4. Test build: `./gradlew :desktop:packageMsi`
5. Update documentation if needed
6. Submit PR with description

---

## Additional Resources

- [Compose for Desktop Docs](https://github.com/JetBrains/compose-jb)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Material 3 Guidelines](https://m3.material.io/)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Jackson Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)

---

**Last Updated**: January 8, 2026
