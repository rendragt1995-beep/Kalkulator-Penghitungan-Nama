package com.penyandingan.nama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.penyandingan.nama.ui.KalkulatorScreen
import com.penyandingan.nama.ui.KalkulatorViewModel
import com.penyandingan.nama.ui.theme.KalkulatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KalkulatorTheme {
                val vm: KalkulatorViewModel = viewModel()
                KalkulatorScreen(viewModel = vm)
            }
        }
    }
}
