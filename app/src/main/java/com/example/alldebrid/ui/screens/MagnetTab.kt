package com.example.alldebrid.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alldebrid.data.Magnet
import com.example.alldebrid.ui.formatSize
import com.example.alldebrid.viewmodel.AppViewModel
import kotlin.math.roundToInt

@Composable
fun MagnetTab(viewModel: AppViewModel) {
    var magnetLink by remember { mutableStateOf("") }
    val uploadInProgress by viewModel.uploadInProgress.collectAsState()
    val uploadMessage by viewModel.uploadMessage.collectAsState()
    val magnets by viewModel.magnets.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshMagnets() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = magnetLink,
            onValueChange = { magnetLink = it },
            label = { Text("Paste magnet link") },
            placeholder = { Text("magnet:?xt=urn:btih:...") },
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                viewModel.submitMagnet(magnetLink)
                magnetLink = ""
            },
            enabled = magnetLink.isNotBlank() && !uploadInProgress,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (uploadInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add magnet")
            }
        }

        uploadMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = msg,
                color = if (msg.startsWith("Failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Magnets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (magnets.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("No magnets yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(magnets, key = { it.id ?: it.hash ?: it.filename ?: System.identityHashCode(it) }) { magnet ->
                    MagnetCard(magnet = magnet) { magnet.id?.let { viewModel.deleteMagnet(it) } }
                }
            }
        }
    }
}

@Composable
private fun MagnetCard(magnet: Magnet, onDelete: () -> Unit) {
    var expandedName by remember { mutableStateOf(false) }
    val isReady = magnet.status?.equals("Ready", ignoreCase = true) == true || magnet.statusCode == 4
    val progress = remember(magnet.downloaded, magnet.size, magnet.processingPerc) {
        val size = magnet.size ?: 0L
        val downloaded = magnet.downloaded ?: 0L
        val perc = magnet.processingPerc ?: 0.0
        
        if (isReady) 1f
        else if (size > 0) (downloaded.toFloat() / size.toFloat()).coerceIn(0f, 1f)
        else if (perc > 0) (perc.toFloat() / 100f).coerceIn(0f, 1f)
        else 0f
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = { expandedName = !expandedName }
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isReady) Icons.Filled.CheckCircle else Icons.Filled.Downloading,
                    contentDescription = null,
                    tint = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = magnet.filename ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = if (expandedName) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }

            Spacer(Modifier.height(10.dp))

            if (isReady) {
                val sizeStr = remember(magnet.size) { formatSize(magnet.size) }
                AssistChip(onClick = {}, label = { Text("Ready \u00b7 $sizeStr") })
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.height(6.dp))
                
                val statusStr = remember(magnet.status, progress, magnet.downloaded, magnet.size) {
                    "${magnet.status ?: "Processing"} \u00b7 ${(progress * 100).roundToInt()}% \u00b7 " +
                        "${formatSize(magnet.downloaded)} / ${formatSize(magnet.size)}"
                }
                
                Text(
                    text = statusStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Removed duplicate formatSize
