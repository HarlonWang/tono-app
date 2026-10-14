package com.tono.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.tono.app.auth.initLoginbaseAuth
import com.tono.app.core.App

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initLoginbaseAuth(applicationContext)

        setContent {
            // 系统栏深浅由 appsettings-kmp 的 AppearanceTheme 按外观模式同步（TonoTheme 内），这里不再按系统深浅写死
            App()
        }
    }
}
