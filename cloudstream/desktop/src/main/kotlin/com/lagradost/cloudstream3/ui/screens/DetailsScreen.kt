@file:OptIn(ExperimentalMaterial3Api::class)

package com.lagradost.cloudstream3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.utils.SettingsManager
import com.lagradost.cloudstream3.player.ExternalPlayerBridge

@Composable
fun DetailsScreen(
    result: SearchResult,
    onBack: () -> Unit
) {
    var showPlayerDialog by remember { mutableStateOf(false) }
    var streamUrl by remember { mutableStateOf("") }
    var selectedPlayer by remember { mutableStateOf(SettingsManager.getDefaultPlayer()) }
    val availablePlayers = remember { ExternalPlayerBridge.detectPlayers() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Details",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        // Content
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Poster and basic info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Poster image
                AsyncImage(
                    model = result.posterUrl,
                    contentDescription = "${result.title} poster",
                    modifier = Modifier
                        .width(150.dp)
                        .height(225.dp),
                    contentScale = ContentScale.Crop
                )
                
                // Basic info
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(result.year)
                        Chip(result.type)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Action buttons
                    Button(
                        onClick = { showPlayerDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play")
                    }
                }
            }
            
            HorizontalDivider()
            
            // Synopsis section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Synopsis",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "This is a placeholder synopsis for ${result.title}. In a future phase, " +
                        "this will fetch real metadata from the provider's API including plot summary, " +
                        "cast, rating, and more.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Additional info section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Additional Information",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    InfoRow("Type", result.type)
                    InfoRow("Year", result.year)
                    InfoRow("Status", "Available")
                    
                    if (result.type == "TV Series") {
                        InfoRow("Episodes", "Coming in future phase")
                    }
                }
            }
        }
    }
    
    // Player dialog
    if (showPlayerDialog) {
        AlertDialog(
            onDismissRequest = { showPlayerDialog = false },
            title = { Text("Play Video") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Enter stream URL to test external player:")
                    
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("Stream URL") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text(
                        "Select Player:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    availablePlayers.forEach { player ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPlayer == player,
                                onClick = { selectedPlayer = player }
                            )
                            Column {
                                Text(player.name.replace("_", " "))
                                if (player == ExternalPlayerBridge.Player.SYSTEM_DEFAULT) {
                                    Text(
                                        "Opens URL in default browser/app",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Text(
                        "Note: YouTube URLs won't work - use direct .mp4/.m3u8 stream links",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (streamUrl.isNotBlank()) {
                            ExternalPlayerBridge.launchPlayer(
                                player = selectedPlayer,
                                url = streamUrl,
                                title = result.title
                            )
                            showPlayerDialog = false
                        }
                    },
                    enabled = streamUrl.isNotBlank()
                ) {
                    Text("Launch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlayerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
