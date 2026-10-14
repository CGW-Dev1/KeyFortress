package com.keyfortress.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.keyfortress.app.ui.navigation.KeyFortressNavGraph
import com.keyfortress.app.ui.theme.KeyFortressTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Protect screen content: Prevent screenshots, screen recording, and obscure in recent tasks
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        setContent {
            KeyFortressTheme {
                KeyFortressNavGraph()
            }
        }
    }
}
