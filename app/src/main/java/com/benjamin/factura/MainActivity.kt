package com.benjamin.factura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.benjamin.factura.ui.navigation.FacturaAppNavigation
import com.benjamin.factura.ui.theme.FacturaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FacturaTheme {
                FacturaAppNavigation()
            }
        }
    }
}
