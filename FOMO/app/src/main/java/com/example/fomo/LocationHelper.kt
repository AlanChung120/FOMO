// LocationHelper.kt
package com.example.fomo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.fomo.models.MapViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

object LocationHelper {


  // Check if fine and coarse location permissions are granted
  fun areLocationPermissionsGranted(context: Context): Boolean {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
      context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseLocationGranted = ContextCompat.checkSelfPermission(
      context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineLocationGranted && coarseLocationGranted
  }

  // Check if background location permission is granted (for Android Q and above)
  fun isBackgroundLocationPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  @Composable
  fun LocationChecker(
    foregroundPermissionsGranted: Boolean,
    backgroundPermissionGranted: Boolean,
    mapViewModel: MapViewModel,
    context: Context
  ) {
    val locationPermissions =
      mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    var requestBackgroundPermission by remember { mutableStateOf(false) }

    val locationPermissionLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions: Map<String, Boolean> ->
          permissions.forEach { (permission, isGranted) ->
            println("Permission: $permission is granted: $isGranted")
          }
          val foregroundGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

          if (foregroundGranted &&
            !backgroundPermissionGranted &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestBackgroundPermission = true
          } else if (foregroundGranted){
            getPreciseLocation(context, mapViewModel)
          }
        })

    LaunchedEffect(Unit) {
      if (!foregroundPermissionsGranted) {
        try {
          locationPermissionLauncher.launch(locationPermissions.toTypedArray())
        } catch (e: Exception) {
          println("Error requesting permissions")
          e.printStackTrace()
        }
      } else if (!backgroundPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requestBackgroundPermission = true
      } else {
        getPreciseLocation(context, mapViewModel)
      }
    }

    // Trigger the background location permission request in a proper composable context
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        RequestBackgroundLocationPermission(requestBackgroundPermission, context)
    }
  }

  @SuppressLint("MissingPermission")
  fun getPreciseLocation(context: Context, viewState: MapViewModel) {
    val fusedLocationClient: FusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
      .addOnSuccessListener { location: Location? ->
        if (location != null) {
          val latitude = location.latitude
          val longitude = location.longitude

          viewState.updateUserLocation(latitude, longitude)
        } else {
          Log.d("PreciseLocation", "Location is null, unable to retrieve location.")
          println("Location is null, unable to retrieve location.")
        }
      }
      .addOnFailureListener { exception ->
        Log.e("PreciseLocation", "Failed to get location: ${exception.message}")
        println("Failed to get location: ${exception.message}")
      }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  @Composable
  fun RequestBackgroundLocationPermission(requestBackgroundPermission: Boolean, context: Context) {

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { isGranted: Boolean ->
        if (isGranted) {
          println("Background location permission was granted")
        } else {
          println("Background location permission was denied")
        }
      })

    LaunchedEffect(requestBackgroundPermission) {
      // Only request if permission is not already granted
      if (requestBackgroundPermission &&
        ContextCompat.checkSelfPermission(
          context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
        try {
          backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }
}