package dev.lifeos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.lifeos.App
import dev.lifeos.data.Source

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as LifeOsApp
        setContent { App(app.repository, Source.ANDROID, sync = app.sync) }
    }
}
