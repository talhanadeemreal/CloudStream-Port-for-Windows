# CloudStream Windows Desktop

A native Windows desktop port of [CloudStream](https://github.com/recloudstream/cloudstream) - a streaming application for movies and TV shows, built with Kotlin Multiplatform and Compose for Desktop.

## 🎯 Project Status

**Current Phase: 9/10 Complete** ✅

This is a functional desktop application with the following working features:
- ✅ Modern Material 3 UI with navigation
- ✅ Plugin system (loads `.jar` extensions)
- ✅ Repository management (add/browse extension repos)
- ✅ Extension downloader with DEX-to-JAR conversion
- ✅ Search functionality across loaded providers
- ✅ Details page with external player integration
- ✅ Settings persistence
- ✅ Image loading with Coil
- 🚧 Media download manager (Phase 10 - planned)

## 📋 Features

### Extension System
- **Plugin Loader**: Dynamically loads CloudStream extensions from `%AppData%/CloudStream/extensions/`
- **Repository Browser**: Add repositories using shortcodes (`cs-main`, `cs-english`) or full URLs
- **Smart Downloads**: Automatically converts `.cs3` (DEX) files to `.jar` using dex2jar
- **Persistence**: Repositories saved to `%AppData%/CloudStream/repositories.json`

### Media Playback
- **External Player Support**: 
  - MPV (recommended)
  - VLC Media Player
  - System Default Player
- **Direct Link Playback**: Enter video URLs manually

### User Interface
- **Material 3 Design**: Modern, responsive UI
- **Image Loading**: Async poster/thumbnail loading with Coil
- **Navigation**: Home, Search, Extensions, Settings
- **Dark Theme**: Optimized for extended viewing

## 🚀 Quick Start

### Prerequisites
- **JDK 17+** (for building)
- **Windows 10/11** (x64)
- **MPV or VLC** (optional, for video playback)

### Building from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/cloudstream-windows.git
   cd cloudstream-windows/cloudstream
   ```

2. **Build the MSI installer**
   ```bash
   ./gradlew :desktop:packageMsi
   ```

3. **Install the application**
   - Installer location: `desktop/build/compose/binaries/main/msi/CloudStream-1.0.0.msi`
   - Run the MSI and follow installation prompts

### Running from IDE

```bash
./gradlew :desktop:run
```

## 📁 Project Structure

```
cloudstream/
├── library/          # Shared Kotlin code (MainAPI, models, utilities)
├── desktop/          # Desktop-specific implementation
│   └── src/main/kotlin/com/lagradost/cloudstream3/
│       ├── Main.kt                 # Application entry point
│       ├── ui/screens/             # Compose UI screens
│       ├── plugins/                # Plugin system
│       │   ├── PluginLoader.kt     # JAR class loading
│       │   ├── RepositoryManager.kt # Repo fetching/persistence
│       │   └── PluginDownloader.kt  # Extension downloader
│       ├── player/                 # External player bridge
│       └── utils/                  # Settings, helpers
├── gradle/           # Build configuration
└── build.gradle.kts  # Root build script
```

## 🔧 Development

### Tech Stack
- **Language**: Kotlin 2.2.21
- **UI Framework**: Compose for Desktop (JetBrains)
- **HTTP Client**: OkHttp 4.x
- **JSON**: Jackson (kotlin module)
- **Image Loading**: Coil 3.0
- **Packaging**: JPackage (MSI)

### Adding Extensions

1. **From Repository** (Recommended):
   - Go to Extensions → Browse
   - Click "Add Repo"
   - Enter shortcode: `cs-main` or `megarepo`
   - Browse and download plugins

2. **Manual Installation**:
   - Place `.jar` files in `%AppData%/CloudStream/extensions/`
   - Restart application

### Configuration Files

- **Extensions**: `%AppData%/CloudStream/extensions/*.jar`
- **Repositories**: `%AppData%/CloudStream/repositories.json`
- **Settings**: Java Preferences API (`HKEY_CURRENT_USER\Software\JavaSoft\Prefs\cloudstream3`)

## 🗺️ Roadmap

See [ROADMAP.md](ROADMAP.md) for detailed phase breakdown.

**Completed Phases (1-9):**
- Core UI and navigation
- Plugin system with real JAR loading
- Repository ecosystem
- External player integration
- Settings persistence

**Planned (Phase 10):**
- Multi-threaded download manager
- HLS video download support
- Downloads library UI
- SQLDelight integration

## 🐛 Known Issues

1. **Plugin Compatibility**: Some Android-specific plugins may fail to load due to missing Android APIs
2. **File Locking**: Deleting extensions may require app restart on Windows
3. **MPV Detection**: Auto-detection works for standard install locations only

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/YourFeature`)
3. Commit your changes (`git commit -m 'Add some feature'`)
4. Push to the branch (`git push origin feature/YourFeature`)
5. Open a Pull Request

See [BUILD.md](BUILD.md) for detailed build instructions.

## 📄 License

This project is licensed under the same license as the original CloudStream project.

## 🙏 Acknowledgments

- [CloudStream Android](https://github.com/recloudstream/cloudstream) - Original project
- [JetBrains Compose](https://github.com/JetBrains/compose-jb) - Desktop UI framework
- [dex2jar](https://github.com/pxb1988/dex2jar) - DEX to JAR conversion

## 📞 Support

For issues, please use the [GitHub Issues](https://github.com/YOUR_USERNAME/cloudstream-windows/issues) page.
