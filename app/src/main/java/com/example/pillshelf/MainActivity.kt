package com.example.pillshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.pillshelf.ui.PillshelfApp
import com.example.pillshelf.ui.theme.PillshelfTheme
import com.example.pillshelf.ui.viewmodel.PillshelfViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PillshelfViewModel by viewModels {
        PillshelfViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PillshelfTheme {
                PillshelfApp(viewModel = viewModel)
            }
        }
    }
}
