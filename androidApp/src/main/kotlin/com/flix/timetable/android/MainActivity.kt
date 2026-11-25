package com.flix.timetable.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flix.timetable.android.ui.theme.FlixTheme
import com.flix.timetable.android.ui.timetable.TimetableRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FlixTheme {
                TimetableRoute()
            }
        }
    }
}
