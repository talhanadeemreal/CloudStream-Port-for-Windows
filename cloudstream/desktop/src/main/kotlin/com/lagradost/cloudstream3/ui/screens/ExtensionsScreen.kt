package com.lagradost.cloudstream3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.plugins.PluginLoader
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.plugins.PluginDownloader
import kotlinx.coroutines.launch

@Composable
fun ExtensionsScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Installed", "Browse")
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTabIndex) {
            0 -> InstalledExtensions()
            1 -> BrowseExtensions()
        }
    }
}

@Composable
fun InstalledExtensions() {
    val scope = rememberCoroutineScope()
    var plugins by remember { mutableStateOf<List<PluginLoader.PluginInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Load plugins on startup
    LaunchedEffect(Unit) {
        isLoading = true
        plugins = PluginLoader.loadAllPlugins()
        isLoading = false
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Installed Extensions",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        plugins = PluginLoader.loadAllPlugins()
                        isLoading = false
                    }
                }
            ) {
                Text("Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show extensions directory path
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Folder, "Folder")
                Column {
                    Text(
                        "Extensions Directory:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        PluginLoader.getExtensionsPath(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (plugins.isEmpty()) {
            Text(
                text = "No extensions found. Place .jar files in the extensions directory or download from the Browse tab.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(plugins) { plugin ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = plugin.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Version: ${plugin.version}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "File: ${plugin.file.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                try {
                                    PluginLoader.deletePlugin(plugin)
                                    // Refresh list locally or trigger reload
                                    // Local state update via scope
                                    scope.launch {
                                        isLoading = true
                                        // Slight delay to allow file system to catch up if needed
                                        kotlinx.coroutines.delay(100) 
                                        plugins = PluginLoader.loadAllPlugins()
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseExtensions() {
    val scope = rememberCoroutineScope()
    var repositories by remember { mutableStateOf(RepositoryManager.getRepositories()) }
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var expandedRepo by remember { mutableStateOf<RepositoryManager.Repository?>(null) }
    var repoPlugins by remember { mutableStateOf<List<RepositoryManager.SitePlugin>>(emptyList()) }
    var isFetchingPlugins by remember { mutableStateOf(false) }

    // Error handling
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Repositories",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = { showAddRepoDialog = true }) {
                Icon(Icons.Default.Add, "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Repo")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (repositories.isEmpty()) {
            Text("No repositories added. Add a repository to browse plugins.")
            Text("Try shortcode: cs-main", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(repositories) { repo ->
                    val isExpanded = expandedRepo == repo
                    
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(repo.name, style = MaterialTheme.typography.titleMedium)
                                    if (repo.description != null) {
                                        Text(repo.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(repo.url, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        if (isExpanded) {
                                            expandedRepo = null
                                        } else {
                                            expandedRepo = repo
                                            isFetchingPlugins = true
                                            scope.launch {
                                                try {
                                                    repoPlugins = RepositoryManager.fetchPlugins(repo)
                                                    isFetchingPlugins = false
                                                } catch (e: Exception) {
                                                    isFetchingPlugins = false
                                                    e.printStackTrace()
                                                    errorMessage = "Failed to fetch plugins: ${e.message}"
                                                    showErrorDialog = true
                                                    expandedRepo = null
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Text(if (isExpanded) "Collapse" else "View Plugins")
                                }
                            }
                            
                            if (isExpanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                if (isFetchingPlugins) {
                                    CircularProgressIndicator()
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        repoPlugins.forEach { plugin ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(plugin.internalName, style = MaterialTheme.typography.titleSmall)
                                                    if (plugin.authors != null) {
                                                        Text("By: ${plugin.authors.joinToString()}", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            try {
                                                                println("Downloading ${plugin.internalName}...")
                                                                PluginDownloader.downloadPlugin(
                                                                    url = plugin.url,
                                                                    fileName = plugin.internalName
                                                                )
                                                                // Hot-reload plugins
                                                                PluginLoader.loadAllPlugins()
                                                                println("Downloaded and loaded ${plugin.internalName}")
                                                            } catch (e: Exception) {
                                                                println("Failed to download: ${e.message}")
                                                                errorMessage = "Failed to download: ${e.message}"
                                                                showErrorDialog = true
                                                                e.printStackTrace()
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Download, "Download")
                                                }
                                            }
                                        }
                                        if (repoPlugins.isEmpty()) {
                                            Text("No plugins found in this repository.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddRepoDialog) {
        AddRepoDialog(
            onDismiss = { showAddRepoDialog = false },
            onAdd = { url ->
                scope.launch {
                    try {
                        // In a real app, this should be suspend and handle IO
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            RepositoryManager.addRepository(url)
                        }
                        repositories = RepositoryManager.getRepositories()
                        showAddRepoDialog = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                        errorMessage = e.message ?: "Unknown error adding repository"
                        showErrorDialog = true
                    }
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun AddRepoDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Repository") },
        text = {
            Column {
                Text("Enter repository URL or shortcode (e.g. cs-main):")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(text) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
