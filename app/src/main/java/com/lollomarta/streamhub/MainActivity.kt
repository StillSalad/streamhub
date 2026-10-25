package com.lollomarta.streamhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lollomarta.streamhub.domain.model.UserSettings
import com.lollomarta.streamhub.navigation.StreamHubNavHost
import com.lollomarta.streamhub.ui.theme.StreamHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as StreamHubApp).container
        setContent {
            val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
            StreamHubTheme(reduceMotion = settings.reduceMotion) {
                StreamHubNavHost()
            }
        }
    }
}
