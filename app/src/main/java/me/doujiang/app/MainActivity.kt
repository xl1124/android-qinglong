package me.doujiang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import me.doujiang.app.ui.navigation.QingLongNavGraph
import me.doujiang.app.ui.theme.QingLongTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = QingLongApp.getInstance()
            val localStorage = app.localStorage
            val themeModeState = localStorage.themeMode.collectAsState(initial = "system")
            val dynamicColorState = localStorage.dynamicColor.collectAsState(initial = true)

            val darkTheme = when (themeModeState.value) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            QingLongTheme(darkTheme = darkTheme, dynamicColor = dynamicColorState.value) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QingLongNavGraph()
                }
            }
        }
    }
}
