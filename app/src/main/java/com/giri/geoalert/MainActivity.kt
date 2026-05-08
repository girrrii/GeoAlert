package com.giri.geoalert

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.giri.geoalert.ui.screens.AppNavigation
import com.giri.geoalert.ui.theme.GEOALERTModelTheme
import com.giri.geoalert.viewmodel.GeoAlertViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GeoAlertViewModel by viewModels()

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionRequest.launch(permissions.toTypedArray())

        setContent {
            GEOALERTModelTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}