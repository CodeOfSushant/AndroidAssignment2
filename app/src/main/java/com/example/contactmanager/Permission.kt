package com.example.contactmanager

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlin.invoke

class Permission(private val activity: AppCompatActivity) {

    private val PREFS_NAME = "PermissionPrefs"
    private val KEY_CONTACTS_REQUESTED = "contacts_requested"

    private var onPermissionsGranted: (() -> Unit)? = null


    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_CONTACTS] ?: false

        if ( readGranted && writeGranted) {
            onPermissionsGranted?.invoke()
        } else {
            // PermissionA denied
            setPermissionRequested()
        }
    }

    fun checkAndRequestContactsPermission(onSuccess: () -> Unit) {

        this.onPermissionsGranted = onSuccess

        val readPermission =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
        val writePermission =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS)
        val isFirstTime = !activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_CONTACTS_REQUESTED, false)

        when {
            // Case 1: PermissionA is already granted
            readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED -> {
                onSuccess()
                return
            }
            // Case 2: Educational UI (Rationale)
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_CONTACTS
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.WRITE_CONTACTS
            ) -> {
                showRationaleDialog()
            }

            !isFirstTime -> {
                showSettingsDialog()
            }
            // Case 3: Request the permission directly
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                    )
                )
            }
        }
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Contacts PermissionA Needed")
            .setMessage("This app is much more useful with your contacts! Allow access to see your")
            .setCancelable(false)
            .setPositiveButton("Allow") { _, _ ->
                // Re-request the permission after the user acknowledges the explanation
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                    )
                )
            }
            .setNegativeButton("No Thanks"){ _, _ ->
                activity.moveTaskToBack(true)
            }
            .setOnKeyListener { _, keyCode, _ ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    activity.finish()
                    true
                } else false
            }
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(activity)
            .setTitle("PermissionA Permanently Denied")
            .setMessage("You have denied contact permission twice. To use this feature, you must enable it manually in the App Settings.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                try{
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                    activity.startActivity(intent)
                }catch (e : Exception){
                    activity.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            .setNegativeButton("Cancel"){ _,_ ->
                activity.moveTaskToBack(true)
            }
            .setOnKeyListener { _, keyCode, _ ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    activity.moveTaskToBack(true)
                    true
                } else false
            }
            .show()
    }


    private fun setPermissionRequested() {
        activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            putBoolean(KEY_CONTACTS_REQUESTED, true)
        }
    }
}