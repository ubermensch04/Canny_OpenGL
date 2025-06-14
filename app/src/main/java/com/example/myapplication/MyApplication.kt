// In app/src/main/java/com/example/myapplication/MyApplication.kt
package com.example.myapplication

import androidx.multidex.MultiDexApplication

/**
 * This custom Application class is necessary to enable multidex support
 * on all Android versions when the automatic process fails.
 */
class MyApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        // You can add any other app-wide initialization code here if needed.
    }
}
