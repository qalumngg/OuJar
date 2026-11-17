package com.qalumn.ourjar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.qalumn.ourjar.R
import com.qalumn.ourjar.data.Repo
import com.qalumn.ourjar.ui.theme.Ours

private enum class Tab(val label: String) {
    Notes("Notes"), Jar("Jar"), Dates("Dates"), Us("Us")
}

@Composable
fun AppRoot(requestedTab: Int, onTabRequestHandled: () -> Unit) {
    val session by Repo.session.collectAsState()
    val welcome by Repo.welcomeCode.collectAsState()

    if (session == null || welcome != null) {
        OnboardingScreen(welcome)
    } else {
        MainShell(requestedTab, onTabRequestHandled)
    }
}

@Composable
private fun MainShell(requestedTab: Int, onHandled: () -> Unit) {
    val c = Ours.colors
    var tabIndex by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(requestedTab) {
        if (requestedTab in Tab.entries.indices) {
            tabIndex = requestedTab
            onHandled()
        }
    }

    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            Column {
                HorizontalDivider(color = c.line)
                NavigationBar(containerColor = c.card, tonalElevation = 0.dp) {
                    Tab.entries.forEachIndexed { i, tab ->
                        NavigationBarItem(
                            selected = tabIndex == i,
                            onClick = { tabIndex = i },
                            icon = {
                                when (tab) {
                                    Tab.Notes -> Icon(Icons.Filled.Email, contentDescription = null)
                                    Tab.Jar -> Icon(painterResource(R.drawable.ic_jar), contentDescription = null)
                                    Tab.Dates -> Icon(Icons.Filled.DateRange, contentDescription = null)
                                    Tab.Us -> Icon(Icons.Filled.Favorite, contentDescription = null)
                                }
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = c.onPrimaryContainer,
                                selectedTextColor = c.ink,
                                indicatorColor = c.primaryContainer,
                                unselectedIconColor = c.inkSoft,
                                unselectedTextColor = c.inkSoft,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().background(c.bg).padding(inner)) {
            when (Tab.entries[tabIndex]) {
                Tab.Notes -> NotesScreen(onOpenUs = { tabIndex = Tab.Us.ordinal })
                Tab.Jar -> JarScreen()
                Tab.Dates -> CalendarScreen()
                Tab.Us -> UsScreen()
            }
        }
    }
}
