# Project Roadmap & Development History

## 📊 Current Status: Phase 9 Complete

**Total Progress**: 90% (9/10 phases complete)

---

## ✅ Completed Phases

### Phase 1: Foundation & Build System ✅
**Status**: Complete  
**Duration**: Initial setup

**Deliverables**:
- Kotlin Multiplatform project structure
- Gradle build configuration
- MSI packaging with jpackage
- Basic Material 3 theme

**Key Files**:
- `build.gradle.kts` - Root build configuration
- `desktop/build.gradle.kts` - Desktop module configuration
- `gradle/libs.versions.toml` - Dependency management

---

### Phase 2: Core UI Structure ✅
**Status**: Complete

**Features**:
- Navigation rail with Home, Search, Extensions, Settings
- Screen routing and state management
- Material 3 color scheme
- Responsive layout

**Key Files**:
- `Main.kt` - Application entry point with navigation
- `ui/screens/HomeScreen.kt`
- `ui/screens/SearchScreen.kt`
- `ui/screens/ExtensionsScreen.kt`
- `ui/screens/SettingsScreen.kt`

---

### Phase 3: Plugin System (Basic) ✅
**Status**: Complete

**Features**:
- Extensions directory management (`%AppData%/CloudStream/extensions/`)
- JAR file scanning
- Plugin metadata display
- File system handling

**Key Files**:
- `plugins/PluginLoader.kt` - JAR scanning and listing

---

### Phase 4: Search UI & Mock Data ✅
**Status**: Complete

**Features**:
- Search input with query state
- Grid layout for results
- Result cards with poster placeholders
- Click navigation to details

**Key Files**:
- `ui/screens/SearchScreen.kt` - Search UI with grid

---

### Phase 5: Details Screen & Navigation ✅
**Status**: Complete

**Features**:
- Details page with poster, title, metadata
- Navigation from search results
- Back button functionality
- Conditional navigation rail visibility

**Key Files**:
- `ui/screens/DetailsScreen.kt` - Media details view

---

### Phase 6: External Player Integration ✅
**Status**: Complete

**Features**:
- MPV player detection and launch
- VLC player support
- System default player option
- URL input for direct playback
- Player selection persistence

**Key Files**:
- `player/ExternalPlayerBridge.kt` - Player detection & launch
- `utils/SettingsManager.kt` - Java Preferences API for settings

---

### Phase 7: Image Loading ✅
**Status**: Complete

**Features**:
- Coil 3.0 integration for Compose Desktop
- Async image loading with placeholders
- Error handling for failed loads
- OkHttp network backend

**Dependencies Added**:
- `io.coil-kt.coil3:coil-compose:3.3.0`
- `io.coil-kt.coil3:coil-network-okhttp:3.3.0`

**Updated Files**:
- `SearchScreen.kt` - AsyncImage for posters
- `DetailsScreen.kt` - AsyncImage for detail poster

---

### Phase 8: Real Plugin Integration ✅
**Status**: Complete

**Features**:
- Actual JAR class loading with URLClassLoader
- MainAPI interface implementation detection
- Provider instantiation from plugins
- Search across all loaded providers
- Error handling for Android-dependent classes

**Key Changes**:
- `PluginLoader.kt` - Real `loadProviderFromJar()` implementation
- `SearchScreen.kt` - Integrated `PluginLoader.search()`
- `Main.kt` - Load plugins on app startup

**Technical Details**:
- Uses `URLClassLoader` to load JAR classes
- Iterates through JAR entries to find `MainAPI` implementations
- Handles `NoClassDefFoundError` for Android-specific dependencies

---

### Phase 9: Repository Ecosystem ✅
**Status**: Complete

**Features**:
- Repository management (add/remove/persist)
- Shortcode resolution (`cs-main` → GitHub URL)
- Manifest fetching (`repo.json`, `plugins.json`)
- Plugin browser with download
- DEX-to-JAR conversion for `.cs3` files
- Smart file type detection (JAR vs DEX)
- Hot-reload after installation
- Extension deletion
- Codeberg 403 bypass (User-Agent)
- JSON parsing flexibility (ignores unknown fields)

**Key Files**:
- `plugins/RepositoryManager.kt` - Repo fetching, persistence, JSON parsing
- `plugins/PluginDownloader.kt` - Download & DEX conversion with magic byte detection
- `ui/screens/ExtensionsScreen.kt` - Tabbed UI (Installed/Browse)

**Persistence**:
- Repositories: `%AppData%/CloudStream/repositories.json`
- Extensions: `%AppData%/CloudStream/extensions/*.jar`

**Dependencies Added**:
- `com.fasterxml.jackson.module:jackson-module-kotlin` - JSON parsing
- `com.github.pxb1988:dex2jar:v2.4` - DEX to JAR conversion

**Bug Fixes Applied**:
1. **Codeberg 403**: Added User-Agent header to OkHttpClient
2. **Persistence**: Repositories now auto-load on startup
3. **JSON Parsing**: Configured `FAIL_ON_UNKNOWN_PROPERTIES = false`
4. **Smart Conversion**: Checks PK magic bytes to detect JAR files
5. **Error Handling**: Exceptions propagate to UI with user-friendly dialogs

**Supported Repositories**:
- `cs-main` - Official CloudStream extensions
- `cs-english` - English providers
- `cs-hexated` - Hexated collection
- `megarepo` - MegaRepo with `pluginLists` support

---

## 🚧 Phase 10: Media Download Manager (Planned)

**Status**: Not Started  
**Complexity**: High  
**Estimated Effort**: 3-4 days

### Planned Features

#### 1. Download Manager Core
- Multi-threaded download engine (OkHttp)
- Progress tracking per download
- Pause/resume functionality
- Queue management
- Concurrent download limits

#### 2. HLS Support
- M3U8 playlist parsing
- Segment downloading
- Automatic merging with FFmpeg
- Quality selection

#### 3. UI Components
- Downloads tab in navigation rail
- Download list with progress bars
- Sort options (date, name, status)
- Download button on Details screen
- Download location picker

#### 4. Data Persistence
- SQLDelight database setup
- Downloads table schema:
  ```sql
  CREATE TABLE downloads (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    file_path TEXT,
    total_bytes INTEGER,
    downloaded_bytes INTEGER,
    status TEXT, -- pending, downloading, paused, completed, failed
    created_at INTEGER,
    completed_at INTEGER
  )
  ```

#### 5. Settings Integration
- Default download location
- Concurrent downloads (1-5)
- Cache size limits
- Auto-cleanup options

### Technical Approach

**Download Manager Architecture**:
```kotlin
object DownloadManager {
    private val downloads = mutableStateListOf<Download>()
    private val executor = Executors.newFixedThreadPool(3)
    
    suspend fun startDownload(url: String, title: String)
    suspend fun pauseDownload(id: Int)
    suspend fun resumeDownload(id: Int)
    suspend fun cancelDownload(id: Int)
}
```

**HLS Downloader**:
```kotlin
class HlsDownloader(val url: String) {
    suspend fun parsePlaylist(): List<Segment>
    suspend fun downloadSegments(segments: List<Segment>)
    suspend fun mergeSegments(output: File)
}
```

### Dependencies Needed
- `app.cash.sqldelight:sqldelight-gradle-plugin` - Database
- `org.jetbrains.kotlinx:kotlinx-coroutines-swing` - UI updates
- FFmpeg (external, for HLS merging)

### UI Mockup
```
┌─────────────────────────────────────┐
│ Downloads                           │
├─────────────────────────────────────┤
│ ▶ Movie Name (2024)                │
│   █████████░░░░░░░ 65% (2.1/3.2 GB)│
├─────────────────────────────────────┤
│ ✓ Another Movie                     │
│   Completed - 1.8 GB                │
└─────────────────────────────────────┘
```

---

## 📈 Project Metrics

### Lines of Code (Estimated)
- Main.kt: ~120 lines
- Screens (5 files): ~800 lines
- Plugins (3 files): ~400 lines
- Player Bridge: ~120 lines
- Utils: ~100 lines
- **Total**: ~1,540 lines of Kotlin

### Build Time
- Clean Build: ~2-3 minutes
- Incremental: ~30-45 seconds
- MSI Package: ~1-2 minutes

### Dependencies
- Direct: 15 libraries
- Transitive: ~50 libraries
- Total JAR size: ~40 MB

---

## 🎯 Future Enhancements

### Beyond Phase 10

#### Android Context Mocking
**Problem**: Many plugins use `Context` from Android  
**Solution**: Create mock implementation
```kotlin
class DesktopContext(private val filesDir: File) : Context {
    override fun getFilesDir() = filesDir
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences
    // ... other methods
}
```

#### Auto-Updates
- Check GitHub releases for new builds
- Download and install updates
- Version comparison logic

#### Plugin Metadata Enhancement
- Filter by language, region, quality
- Rating system
- Update notifications

#### Video Player Embedding
- LibVLC integration (native playback)
- Subtitle support
- Playback controls in-app

#### Multi-Profile Support
- Separate plugin sets per profile
- Different repository configurations
- User switching

---

## 🐛 Known Limitations

### Current Issues
1. **Plugin Compatibility**: ~30% of Android plugins won't work due to missing Android APIs
2. **File Locking**: Windows locks JARs while loaded - deletion requires restart
3. **No Video Player**: Relies on external players (MPV/VLC)
4. **No Persistence**: Repositories reload metadata on every startup (could cache)
5. **No Update Mechanism**: Manual updates required

### Performance Considerations
- JAR loading is synchronous (could block UI for large plugins)
- No lazy loading of repository plugins (all fetched at once)
- Image cache not size-limited

---

## 📝 Development Notes

### Key Decisions Made

1. **Java Preferences over SQLDelight** (Phase 6)
   - Simpler for basic settings
   - No database boilerplate
   - Decision: SQLDelight in Phase 10 for downloads

2. **dex2jar for Conversion** (Phase 9)
   - Gradle dependency: `com.github.pxb1988:dex2jar`
   - Magic byte detection to avoid unnecessary conversion
   - Fallback: Save file anyway if conversion fails

3. **OkHttp for Networking** (Phase 9)
   - Synchronous API used (wrapped in `Dispatchers.IO`)
   - Could refactor to async `enqueue()` for better UX

4. **Codeberg as Primary Source** (Phase 9)
   - GitHub repos deprecated by CloudStream team
   - Added User-Agent to bypass 403 blocks

### Lessons Learned

- **Incremental MSI Builds**: Always test packaging after each phase
- **Error Handling**: Silent failures in coroutines need explicit handling
- **Windows File Locks**: JARs remain locked until app closes
- **JSON Flexibility**: Real-world APIs have inconsistent schemas - use lenient parsing

---

## 🚀 Getting Up to Speed

### For New Contributors

1. **Read Documentation**:
   - `README.md` - Project overview
   - `BUILD.md` - Build instructions
   - This file (`ROADMAP.md`) - Development history

2. **Build the Project**:
   ```bash
   ./gradlew :desktop:packageMsi
   ```

3. **Run from Source**:
   ```bash
   ./gradlew :desktop:run
   ```

4. **Test Key Features**:
   - Add repo: Extensions → Browse → `cs-main`
   - Download plugin: View Plugins → Download
   - Search: Use loaded provider
   - Play: Details → Enter URL

5. **Pick a Task**:
   - See [GitHub Issues](https://github.com/YOUR_USERNAME/cloudstream-windows/issues)
   - Start with "good first issue" labels

### Architecture Overview

```
User Input → Compose UI → ViewModel/State → Business Logic → Platform APIs
                ↓                                              ↓
          Material 3 Components                    File System / Network
                                                         ↓
                                              External Tools (MPV/VLC)
```

### Code Style

- **Kotlin**: Official JetBrains style guide
- **Compose**: Prefer `@Composable` functions over classes
- **Coroutines**: Use `Dispatchers.IO` for blocking operations
- **Error Handling**: Log to console + show user-friendly dialogs

---

## 📞 Questions?

- **Issues**: Use GitHub Issues for bugs
- **Discussions**: Use GitHub Discussions for questions
- **Pull Requests**: Always welcome!

**Last Updated**: January 8, 2026  
**Contributors**: 1 (initial development)
