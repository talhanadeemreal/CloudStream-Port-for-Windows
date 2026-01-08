# Build Guide

## System Requirements

### Hardware
- **RAM**: 4GB minimum, 8GB recommended
- **Disk Space**: 1GB for build tools + dependencies
- **CPU**: x64 processor

### Software
- **JDK**: OpenJDK 17 or later ([AdoptOpenJDK](https://adoptopenjdk.net/) recommended)
- **Git**: For cloning the repository
- **Windows**: 10 or 11 (x64)

## Build Environment Setup

### 1. Install JDK 17+

Download and install JDK 17 from [Adoptium](https://adoptium.net/):
```bash
# Verify installation
java -version
javac -version
```

Both should show version 17.x.x or higher.

### 2. Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/cloudstream-windows.git
cd cloudstream-windows/cloudstream
```

### 3. Verify Gradle

The project uses Gradle wrapper, so no separate Gradle installation needed:
```bash
./gradlew --version
```

## Building the Application

### MSI Installer (Production)

```bash
./gradlew :desktop:packageMsi --no-configuration-cache
```

**Output**: `desktop/build/compose/binaries/main/msi/CloudStream-1.0.0.msi`

**Build Time**: ~2-3 minutes on first build, ~1 minute on subsequent builds

### Running from Source (Development)

```bash
./gradlew :desktop:run
```

This starts the application without creating an installer.

### Clean Build

```bash
./gradlew clean
./gradlew :desktop:packageMsi
```

## Project Modules

### Library Module (`library/`)
Shared Kotlin Multiplatform code:
- `MainAPI` interface for providers
- Data models (`SearchResponse`, `LoadResponse`, etc.)
- Utility functions

Build library only:
```bash
./gradlew :library:build
```

### Desktop Module (`desktop/`)
Platform-specific desktop implementation:
- Compose UI screens
- Plugin loader
- External player bridge
- Platform utilities

Build desktop JAR:
```bash
./gradlew :desktop:jar
```

## Dependencies

### Key Dependencies (managed by Gradle)
- Compose for Desktop: `org.jetbrains.compose`
- Kotlin Coroutines: `org.jetbrains.kotlinx:kotlinx-coroutines-core`
- OkHttp: `com.squareup.okhttp3:okhttp`
- Jackson: `com.fasterxml.jackson.module:jackson-module-kotlin`
- Coil: `io.coil-kt.coil3:coil-compose`
- dex2jar: `com.github.pxb1988:dex2jar`

All dependencies are automatically downloaded during build.

## Gradle Tasks Reference

### Desktop Tasks
```bash
# Compile Kotlin code
./gradlew :desktop:compileKotlin

# Create runtime image
./gradlew :desktop:createRuntimeImage

# Package as MSI
./gradlew :desktop:packageMsi

# Run application
./gradlew :desktop:run
```

### Common Tasks
```bash
# Build all modules
./gradlew build

# Clean all build outputs
./gradlew clean

# Show dependencies
./gradlew :desktop:dependencies
```

## IDE Setup

### IntelliJ IDEA (Recommended)

1. **Open Project**: File → Open → Select `cloudstream/build.gradle.kts`
2. **Wait for Indexing**: Let Gradle sync complete
3. **Run Configuration**:
   - Main class: `com.lagradost.cloudstream3.MainKt`
   - Module: `cloudstream.desktop.main`
   - Working directory: `$PROJECT_DIR$/desktop`

### VS Code

1. Install extensions:
   - Kotlin Language Support
   - Gradle for Java

2. Open folder: `cloudstream/`

3. Use terminal for Gradle commands

## Troubleshooting

### Build Fails with "Out of Memory"

Increase Gradle heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

### "Could not find tools.jar"

Ensure you're using JDK (not JRE):
```bash
echo %JAVA_HOME%
# Should point to JDK directory
```

### MSI Packaging Fails

Check WiX Toolset installation (auto-downloaded by Gradle):
```bash
./gradlew :desktop:checkRuntime
```

### Kotlin Version Mismatch

Clear Gradle cache:
```bash
./gradlew clean --no-daemon
```

## Development Workflow

### Making Changes

1. **Edit Code**: Modify files in `desktop/src/main/kotlin/`
2. **Test**: `./gradlew :desktop:run`
3. **Build**: `./gradlew :desktop:packageMsi`
4. **Install & Test**: Run the MSI installer

### Adding Dependencies

Edit `desktop/build.gradle.kts`:
```kotlin
dependencies {
    implementation("group:artifact:version")
}
```

Sync: `./gradlew --refresh-dependencies`

### Hot Reload

Compose Desktop supports hot reload in development:
- Run: `./gradlew :desktop:run`
- Modify `@Composable` functions
- Changes appear automatically (limited support)

## Build Artifacts

After successful build, you'll find:

```
desktop/build/
├── compose/binaries/main/
│   ├── app/
│   │   └── CloudStream/         # Unpacked application
│   └── msi/
│       └── CloudStream-1.0.0.msi # Installer
├── libs/
│   └── desktop.jar              # Application JAR
└── tmp/
    └── compileKotlin/           # Compiled classes
```

## Performance Tips

### Faster Builds
```bash
# Skip tests
./gradlew :desktop:packageMsi -x test

# Use build cache
./gradlew :desktop:packageMsi --build-cache

# Parallel builds
./gradlew :desktop:packageMsi --parallel
```

### Gradle Daemon
Keep daemon running between builds:
```bash
# Start daemon
./gradlew --daemon

# Stop daemon (if needed)
./gradlew --stop
```

## Testing

Currently manual testing only. To test:

1. Run application: `./gradlew :desktop:run`
2. Add repository: Extensions → Browse → Add Repo → `cs-main`
3. Download plugin: View Plugins → Download
4. Search: Search tab → Enter query
5. Play: Details → Enter URL → Select Player

## Next Steps

- See [ROADMAP.md](ROADMAP.md) for planned features
- See [DEVELOPMENT.md](DEVELOPMENT.md) for architecture details
- Check [Issues](https://github.com/YOUR_USERNAME/cloudstream-windows/issues) for bugs
