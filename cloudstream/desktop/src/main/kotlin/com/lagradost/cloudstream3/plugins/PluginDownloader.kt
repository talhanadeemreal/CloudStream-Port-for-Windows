package com.lagradost.cloudstream3.plugins

import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.DexFileReader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PluginDownloader {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * Download and install a plugin
     * @param url Download URL (.jar or .cs3)
     * @param fileName Target filename (without extension if possible, or we detect it)
     * @return Installed file
     */
    suspend fun downloadPlugin(url: String, fileName: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) throw IOException("Failed to download: $response")
        val body = response.body ?: throw IOException("Empty body")
        
        // Determine extension
        val isCs3 = url.endsWith(".cs3") || url.endsWith(".dex")
        val targetExtension = if (isCs3) "cs3" else "jar"
        val finalFileName = if (fileName.endsWith(".$targetExtension") || fileName.endsWith(".jar")) fileName.removeSuffix(".cs3").removeSuffix(".jar") + ".$targetExtension" else "$fileName.$targetExtension"
        
        // Save to temp file
        val tempFile = File.createTempFile("plugin_download", ".$targetExtension")
        Files.copy(body.byteStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        
        val extensionsDir = File(PluginLoader.getExtensionsPath())
        if (!extensionsDir.exists()) extensionsDir.mkdirs()
        
        if (isCs3) {
            // Check if it's actually a JAR file disguised as .cs3
            val isActuallyJar = try {
                val bytes = tempFile.readBytes()
                // Check for JAR/ZIP magic bytes: 50 4B (PK)
                bytes.size >= 2 && bytes[0].toInt() == 0x50 && bytes[1].toInt() == 0x4B
            } catch (e: Exception) {
                false
            }
            
            val jarFileName = finalFileName.replace(".cs3", ".jar")
            val jarFile = File(extensionsDir, jarFileName)
            
            if (isActuallyJar) {
                // It's already a JAR, just rename it
                println("File is already a JAR, skipping conversion")
                Files.move(tempFile.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                return@withContext jarFile
            } else {
                // Try to convert DEX to JAR
                try {
                    println("Converting .cs3 to .jar: ${tempFile.absolutePath} -> ${jarFile.absolutePath}")
                    
                    val reader = DexFileReader(tempFile)
                    val writer = Dex2jar.from(reader)
                    writer.to(jarFile.toPath())
                    
                    return@withContext jarFile
                } catch (e: Exception) {
                    println("Dex2Jar conversion failed: ${e.message}")
                    e.printStackTrace()
                    
                    // Last resort: try renaming it anyway (maybe it's compatible)
                    try {
                        Files.move(tempFile.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        println("Conversion failed, but saved file anyway for debugging")
                        return@withContext jarFile
                    } catch (moveError: Exception) {
                        throw IOException("Failed to convert .cs3 to .jar: ${e.message}", e)
                    }
                } finally {
                    if (tempFile.exists()) tempFile.delete()
                }
            }
        } else {
            // Just move the JAR
            val targetFile = File(extensionsDir, finalFileName)
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return@withContext targetFile
        }
    }
}
