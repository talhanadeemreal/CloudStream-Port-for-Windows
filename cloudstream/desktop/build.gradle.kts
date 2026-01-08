import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

val javaTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())

// Note: jvmToolchain removed to allow build with available JDK
// Note: Using JVM 11 target due to Rhino and NewPipeExtractor requirements

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// Configure source sets to recognize jvmMain structure
sourceSets {
    main {
        java {
            srcDirs("src/jvmMain/kotlin")
        }
        resources {
            srcDirs("src/jvmMain/resources")
        }
    }
}


dependencies {
    // Core library module
    implementation(project(":library"))
    
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${libs.versions.kotlinxCoroutinesCore.get()}")
    
    // SQLDelight for Desktop
    implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
    
    // Image Loading - Coil for Compose
    implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc01")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc01")
    
    // LibVLC for media playback
    implementation("uk.co.caprica:vlcj:4.8.2")
    implementation("uk.co.caprica:vlcj-natives:4.8.1")
    
    // JSON Serialization
    implementation(libs.jackson.module.kotlin)
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Image loading for Compose Desktop
    implementation("io.coil-kt.coil3:coil:3.0.4")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
    
    // DNS-over-HTTPS
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    
    // DEX to JAR conversion
    implementation("com.github.pxb1988:dex2jar:2.1")
    
    // File system utilities
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

compose.desktop {
    application {
        mainClass = "com.lagradost.cloudstream3.MainKt"
        
        jvmArgs += listOf(
            "-Dfile.encoding=UTF-8",
            "-Xmx2G" // Max heap size for large extension libraries
        )
        
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "CloudStream"
            packageVersion = "1.0.0"
            description = "CloudStream - Free and Open Source Streaming Platform"
            copyright = "© 2026 CloudStream Community"
            vendor = "CloudStream Community"
            licenseFile.set(project.file("../LICENSE"))
            
            windows {
                // Application icon
                // iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                
                // Installer configuration
                menuGroup = "CloudStream"
                perUserInstall = true
                dirChooser = true
                shortcut = true
                upgradeUuid = "A5E2F8C1-3B4D-4E6F-8A9C-1D2E3F4A5B6C"
                
                // File associations for .cs3 files
                // Note: This requires Compose Desktop 1.7.0+ for full support
                // Workaround: Use WiX configuration files
            }
            
            // Include LibVLC natives and runtime
            modules(
                "java.sql",
                "java.naming",
                "java.desktop",
                "java.prefs",
                "jdk.unsupported"
            )
            
            // Application resources
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
        }
    }
}
