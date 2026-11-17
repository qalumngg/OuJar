package com.qalumn.ourjar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qalumn.ourjar.data.Repo
import com.qalumn.ourjar.ui.AppRoot
import com.qalumn.ourjar.ui.theme.OurTheme

class MainActivity : ComponentActivity() {

    private var requestedTab by mutableStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedTab = intent?.getIntExtra(EXTRA_TAB, -1) ?: -1
        setContent {
            OurTheme {
                AppRoot(requestedTab = requestedTab, onTabRequestHandled = { requestedTab = -1 })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTab = intent.getIntExtra(EXTRA_TAB, -1)
    }

    override fun onResume() {
        super.onResume()
        // Refresh the home screen widgets whenever the app is opened.
        Repo.pingWidgets()
    }

    companion object {
        const val EXTRA_TAB = "tab"
    }
}
