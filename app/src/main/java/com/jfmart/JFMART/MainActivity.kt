package com.jfmart.JFMART

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jfmart.JFMART.navigation.AppNavigation
import com.jfmart.JFMART.ui.theme.JFMartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JFMartTheme {
                AppNavigation()
            }
        }
    }
}