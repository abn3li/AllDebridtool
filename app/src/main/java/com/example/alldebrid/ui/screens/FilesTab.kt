package com.example.alldebrid.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alldebrid.ui.FileAction
import com.example.alldebrid.ui.GetFileButton
import com.example.alldebrid.ui.copyToClipboard
import com.example.alldebrid.ui.formatSize
import com.example.alldebrid.ui.startSystemDownload
import com.example.alldebrid.ui.startSystemStream
import com.example.alldebrid.viewmodel.AppViewModel
import com.example.alldebrid.viewmodel.DownloadableFile
import com.example.alldebrid.viewmodel.FileSource
import kotlinx.coroutines.launch

@Composable
fun FilesTab(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var unlockingLink by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val files by viewModel.readyFiles.collectAsState()

    LaunchedEffect(Unit) {
        isRefreshing = true
        viewModel.refreshMagnets()
        isRefreshing = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ready to download",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.refreshMagnets()
                        isRefreshing = false
                    }
                },
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))

        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("No files ready yet \u2014 add a magnet first", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(files, key = { it.link }) { file ->
                    FileCard(
                        file = file,
                        isUnlocking = unlockingLink == file.link,
                        onDelete = {
                            when (file.source) {
                                FileSource.SAVED -> viewModel.deleteSavedLink(file.link)
                                FileSource.MAGNET -> file.magnetId?.let { viewModel.deleteMagnet(it) }
                                FileSource.HISTORY -> viewModel.deleteHistoryLink(file.link)
                            }
                        },
                        onAction = { action ->
                            unlockingLink = file.link
                            scope.launch {
                                val directLink = viewModel.unlockAndGetLink(file.link)
                                unlockingLink = null
                                if (directLink != null) {
                                    when (action) {
                                        FileAction.DOWNLOAD -> {
                                            startSystemDownload(context, directLink, file.fileName)
                                            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                        }
                                        FileAction.STREAM -> startSystemStream(context, directLink)
                                        FileAction.COPY -> copyToClipboard(context, directLink)
                                    }
                                } else {
                                    Toast.makeText(context, "Could not process link", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileCard(
    file: DownloadableFile,
    isUnlocking: Boolean,
    onDelete: () -> Unit,
    onAction: (FileAction) -> Unit
) {
    var expandedName by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = { expandedName = !expandedName }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth().animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (expandedName) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                
                val displaySize = remember(file.size) { formatSize(file.size) }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displaySize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (file.source == FileSource.SAVED) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "\u2022 Saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            GetFileButton(
                isUnlocking = isUnlocking,
                onAction = onAction
            )
        }
    }
}
