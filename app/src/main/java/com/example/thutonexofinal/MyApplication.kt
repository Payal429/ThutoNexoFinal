package com.example.thutonexofinal

import android.app.Application
import android.util.Log
import me.pushy.sdk.Pushy

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Start Pushy background service
        Pushy.listen(this)

        // Register device if not yet registered
        Thread {
            try {
                val token = Pushy.register(this)
                Log.d("Pushy", "Device token: $token")
                // TODO: Save or update token in Firestore for logged-in user
            } catch (e: Exception) {
                Log.e("Pushy", "Pushy registration failed", e)
            }
        }.start()
    }
}
