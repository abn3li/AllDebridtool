package com.example.alldebrid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.alldebrid.R
import com.example.alldebrid.ui.screens.HomeScreen
import com.example.alldebrid.ui.screens.LoginScreen
import com.example.alldebrid.viewmodel.AppViewModel
import com.example.alldebrid.viewmodel.LoginState

@Composable
fun AllDebridApp(viewModel: AppViewModel) {
    val loginState by viewModel.loginState.collectAsState()

    when (loginState) {
        LoginState.LOGGED_IN -> HomeScreen(viewModel = viewModel)
        LoginState.CHECKING -> SplashScreen()
        else -> LoginScreen(viewModel = viewModel)
    }
}

@Composable
fun SplashScreen() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp)
                )
            }
        }
    }
}
