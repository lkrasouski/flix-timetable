package com.flix.timetable.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flix.timetable.android.ui.theme.FlixTheme
import com.flix.timetable.android.ui.timetable.TimetableRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The top bar is brand green in light and dark theme alike, so status bar icons are always dark.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            FlixTheme {
                TimetableRoute()
            }
        }
    }
}
