package com.mahjongzen

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahjongzen.data.settings.AppSettings
import com.mahjongzen.ui.navigation.AppNavigation
import com.mahjongzen.ui.theme.MahjongTheme

/** AppCompatActivity (a ComponentActivity) so that per-app languages work on all API levels. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MahjongApp).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            MahjongTheme(themeMode = settings.themeMode) {
                AppNavigation(container = container, settings = settings)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as MahjongApp).container.sound.onAppForeground(true)
    }

    override fun onStop() {
        super.onStop()
        (application as MahjongApp).container.sound.onAppForeground(false)
    }
}
