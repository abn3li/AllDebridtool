package com.example.alldebrid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.alldebrid.data.HostInfo
import com.example.alldebrid.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsScreen(viewModel: AppViewModel, onDismiss: () -> Unit) {
    val hosts by viewModel.hosterStats.collectAsState()
    val loading by viewModel.hostsLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshHosts()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Supported Hosts") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.refreshHosts() }, enabled = !loading) {
                                if (loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                if (hosts.isEmpty() && !loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hosts found")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(padding)
                    ) {
                        items(hosts) { host ->
                            HostCard(host)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostCard(host: HostInfo) {
    val isUp = host.status == true
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = host.name ?: "Unknown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            
            AssistChip(
                onClick = {},
                label = { 
                    Text(
                        text = if (isUp) "UP" else "DOWN",
                        fontSize = 10.sp
                    ) 
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (isUp) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            host.type?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!host.extensions.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = host.extensions.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }
    }
}
