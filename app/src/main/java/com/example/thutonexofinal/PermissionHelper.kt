package com.example.thutonexofinal

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    // Request codes
    const val REQUEST_MEDIA_IMAGES = 200
    const val REQUEST_MEDIA_VIDEO = 201
    const val REQUEST_MEDIA_AUDIO = 202
    const val REQUEST_CONTACTS = 203
    const val REQUEST_STORAGE = 204
    const val REQUEST_NOTIFICATIONS = 205

    // ---------------- Media Permissions ----------------
    fun requestImagePermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                REQUEST_MEDIA_IMAGES
            )
        } else {
            Toast.makeText(activity, "Image permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestVideoPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                REQUEST_MEDIA_VIDEO
            )
        } else {
            Toast.makeText(activity, "Video permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestAudioPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                REQUEST_MEDIA_AUDIO
            )
        } else {
            Toast.makeText(activity, "Audio permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Contacts Permission ----------------
    fun requestContactsPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CONTACTS
            )
        } else {
            Toast.makeText(activity, "Contacts permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Storage (Older Devices) ----------------
    fun requestStoragePermission(activity: Activity) {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                REQUEST_STORAGE
            )
        } else {
            Toast.makeText(activity, "Storage permissions already granted", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Notifications (Android 13+) ----------------
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATIONS
                )
            } else {
                Toast.makeText(activity, "Notification permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------- Handle Permission Results ----------------
    fun handlePermissionResult(
        activity: Activity,
        requestCode: Int,
        grantResults: IntArray,
        onGranted: (() -> Unit)? = null,
        onDenied: (() -> Unit)? = null
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Permission granted", Toast.LENGTH_SHORT).show()
            onGranted?.invoke()
        } else {
            Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show()
            onDenied?.invoke()
        }
    }
}