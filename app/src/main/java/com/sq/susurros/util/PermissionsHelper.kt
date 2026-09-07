// app/src/main/java/com/sq/susurros/util/PermissionsHelper.kt
package com.sq.susurros.util

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * PermissionsHelper — Gestión de permisos escalonales para Android 15.
 *
 * Según el IDEA.md, usa registerForActivityResult() para:
 * - READ_MEDIA_AUDIO
 * - POST_NOTIFICATIONS
 * - FOREGROUND_SERVICE
 *
 * No se usan permisos legacy como READ_EXTERNAL_STORAGE.
 */
class PermissionsHelper(
    private val activity: ComponentActivity
) {

    // Launcher para solicitar múltiples permisos
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Callback: todos los permisos concedidos
            permissions.entries.all { it.value }
        }

    /**
     * Solicita los permisos necesarios para Android 15.
     */
    fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // READ_MEDIA_AUDIO (Android 13+/15)
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_MEDIA_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_AUDIO)
        }

        // POST_NOTIFICATIONS (Android 13+/15)
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * Verifica si todos los permisos requeridos están concedidos.
     */
    fun arePermissionsGranted(): Boolean {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasNotificationsPermission = ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        return hasAudioPermission && hasNotificationsPermission
    }
}
