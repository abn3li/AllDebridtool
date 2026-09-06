package com.example.alldebrid.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alldebrid.data.LinkUnlockResponse
import com.example.alldebrid.ui.FileAction
import com.example.alldebrid.ui.GetFileButton
import com.example.alldebrid.ui.copyToClipboard
import com.example.alldebrid.ui.formatSize
import com.example.alldebrid.ui.startSystemDownload
import com.example.alldebrid.ui.startSystemStream
import com.example.alldebrid.viewmodel.AppViewModel

@Composable
fun UnlockTab(viewModel: AppViewModel) {
    var url by remember { mutableStateOf("") }
    var isUnlocking by remember { mutableStateOf(false) }
    val history by viewModel.unlockedHistory.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Paste hoster link") },
            placeholder = { Text("https://mega.nz/file/...") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                viewModel.unlockDirectLink(url, { isUnlocking = it }) { _ ->
                    url = ""
                    Toast.makeText(context, "Link unlocked", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = url.isNotBlank() && !isUnlocking,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isUnlocking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("No unlocked links yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { item ->
                    UnlockedCard(
                        item = item,
                        onAction = { action ->
                            val directLink = item.link ?: ""
                            when (action) {
                                FileAction.DOWNLOAD -> {
                                    startSystemDownload(context, directLink, item.filename ?: "file")
                                    Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                }
                                FileAction.STREAM -> startSystemStream(context, directLink)
                                FileAction.COPY -> copyToClipboard(context, directLink)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockedCard(
    item: LinkUnlockResponse,
    onAction: (FileAction) -> Unit
) {
    var expandedName by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = { expandedName = !expandedName }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.filename ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (expandedName) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                
                val displayInfo = remember(item.host, item.filesize) {
                    "${item.host ?: "unknown"} \u2022 ${formatSize(item.filesize)}"
                }
                
                Text(
                    text = displayInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GetFileButton(
                isUnlocking = false,
                onAction = onAction
            )
        }
    }
}
