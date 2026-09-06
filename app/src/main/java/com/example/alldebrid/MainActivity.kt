package com.example.alldebrid

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.alldebrid.data.ThemeMode
import com.example.alldebrid.ui.AllDebridApp
import com.example.alldebrid.ui.theme.AllDebridTheme
import com.example.alldebrid.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        enableHighRefreshRate()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            AllDebridTheme(darkTheme = darkTheme) {
                AllDebridApp(viewModel = viewModel)
            }
        }
    }

    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowParams = window.attributes
            
            val display = try {
                display
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            
            val maxRefreshRate = display?.supportedModes
                ?.maxByOrNull { it.refreshRate }
                ?.refreshRate ?: 0f
                
            if (maxRefreshRate > 60f) {
                windowParams.preferredRefreshRate = maxRefreshRate
                window.attributes = windowParams
            }
        } else {
            // Fallback for older APIs if they have manufacturer-specific high refresh rate
            @Suppress("DEPRECATION")
            val windowParams = window.attributes
            val display = @Suppress("DEPRECATION") windowManager.defaultDisplay
            val maxRefreshRate = @Suppress("DEPRECATION") display?.supportedModes
                ?.maxByOrNull { it.refreshRate }
                ?.refreshRate ?: 0f
                
            if (maxRefreshRate > 60f) {
                windowParams.preferredRefreshRate = maxRefreshRate
                window.attributes = windowParams
            }
        }
    }
}
