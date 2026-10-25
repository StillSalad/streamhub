package com.lollomarta.streamhub.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lollomarta.streamhub.AppContainer
import com.lollomarta.streamhub.StreamHubApp

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as StreamHubApp).container

/** ViewModel legato alla destinazione di navigazione corrente, creato con le dipendenze dell'app. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = appContainer()
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
