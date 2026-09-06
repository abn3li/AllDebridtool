package com.example.alldebrid.ui.screens

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.alldebrid.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import android.content.Intent

private enum class HomeTab(val title: String) {
    UNLOCK("Unlock Link"),
    MAGNET("Add Magnet"),
    FILES("My Files")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val pagerState = rememberPagerState { HomeTab.entries.size }
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(value = false) }
    var showSettings by remember { mutableStateOf(value = false) }
    var showHosts by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val user by viewModel.user.collectAsState()

    LaunchedEffect(pagerState.currentPage) {
        if (HomeTab.entries[pagerState.currentPage] == HomeTab.FILES) {
            viewModel.refreshLibrary()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AllDebrid")
                        user?.username?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Supported Hosts") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showHosts = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear History") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                viewModel.clearHistory()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showSettings = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showAbout = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Log out") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                viewModel.logout()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
                HomeTab.entries.forEach { tab ->
                    Tab(
                        selected = pagerState.currentPage == tab.ordinal,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = tab.ordinal,
                                    animationSpec = spring(stiffness = 1500f)
                                )
                            }
                        },
                        text = { Text(tab.title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (HomeTab.entries[page]) {
                    HomeTab.MAGNET -> MagnetTab(viewModel = viewModel)
                    HomeTab.UNLOCK -> UnlockTab(viewModel = viewModel)
                    HomeTab.FILES -> FilesTab(viewModel = viewModel)
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(viewModel = viewModel) { showSettings = false }
    }

    if (showHosts) {
        HostsScreen(viewModel = viewModel, onDismiss = { showHosts = false })
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Alldebrid Tools") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This app was made with AI",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Contact Developer:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://t.me/hjil_l".toUri())
                        context.startActivity(intent)
                    }
                ) {
                    Text(
                        text = "@hjil_l on Telegram",
                        color = Color(0xFFFACC15),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
