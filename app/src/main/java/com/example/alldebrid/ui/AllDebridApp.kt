package com.example.alldebrid.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.alldebrid.ui.screens.HomeScreen
import com.example.alldebrid.ui.screens.LoginScreen
import com.example.alldebrid.viewmodel.AppViewModel
import com.example.alldebrid.viewmodel.LoginState

@Composable
fun AllDebridApp(viewModel: AppViewModel) {
    val loginState by viewModel.loginState.collectAsState()

    when (loginState) {
        LoginState.LOGGED_IN -> HomeScreen(viewModel = viewModel)
        else -> LoginScreen(viewModel = viewModel)
    }
}
