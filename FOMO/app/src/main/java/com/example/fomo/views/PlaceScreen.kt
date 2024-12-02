package com.example.fomo.views

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import cafe.adriel.voyager.core.screen.Screen
import com.example.fomo.consts.Colors
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.fomo.viewmodel.MyViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.kotlin.place
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.ktx.model.cameraPosition
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi

class PlaceScreen(private val myViewModel: MyViewModel) : Screen {
  @Composable
  override fun Content() {
    val cameraPositionState = rememberCameraPositionState {
      position = CameraPosition.fromLatLngZoom(myViewModel.center, 15f)
    }
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier
          .weight(1f) // Use weight to make PlaceMap take up most of the space
          .fillMaxWidth()
      ) {
        PlaceMap(myViewModel, cameraPositionState)
      }
      PlaceSettings(myViewModel, cameraPositionState)
    }
  }
}

@Composable
fun PlaceSettings(myViewModel: MyViewModel, cameraPositionState: CameraPositionState) {
  val context = LocalContext.current
  var placesExpanded by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .height(250.dp)
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // TextField for place name
      TextField(
        value = myViewModel.placeName,
        onValueChange = { newName -> myViewModel.placeName = newName },
        label = { Text("Place Name") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
          .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
      )

      // Slider for radius
      Column {
        Text(text = "Radius: ${myViewModel.radius.toInt()} meters", style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.Slider(
          value = myViewModel.radius.toFloat(),
          onValueChange = { newRadius -> myViewModel.radius = newRadius.toDouble() },
          valueRange = 100f..1000f, // Adjust range as needed
          modifier = Modifier.fillMaxWidth()
        )
      }

      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
      ) {
        Button(
          onClick = {
            myViewModel.createPlace(cameraPositionState.position.target, myViewModel.radius, myViewModel.placeName) { success ->
              if (success) {
                Toast.makeText(context, "Place Created", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(context, "Error: Something went wrong",
                  Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Colors.primary
          ),
          modifier = Modifier
            .padding(top = 8.dp)
        ) {
          Text(
            text = "Submit",
            modifier = Modifier.padding(4.dp)
          )
        }
      }
    }
  }
}


@OptIn(InternalSerializationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaceMap(myViewModel: MyViewModel, cameraPositionState: CameraPositionState) {
  val context = LocalContext.current
  var isMapLoaded by remember { mutableStateOf(false) }

  // Update camera position whenever myViewModel.center changes
  LaunchedEffect(myViewModel.center) {
    cameraPositionState.animate(
      CameraUpdateFactory.newLatLngZoom(myViewModel.center, 15f),
      1000 // Optional animation duration in milliseconds
    )
  }

  Box(modifier = Modifier.fillMaxSize()) {
    GoogleMap(
      modifier = Modifier.fillMaxSize(),
      cameraPositionState = cameraPositionState,
      onMapLoaded = {
        isMapLoaded = true
      },
      uiSettings = MapUiSettings(
        zoomControlsEnabled = false
      ),
    ) {
      // display places
      for (place in myViewModel.places) {
        val placeLocation = LatLng(place.latitude, place.longitude)
        PlaceMarker(name = place.name, location = placeLocation, color = Colors.dark)
        Circle(
          center = placeLocation,
          radius = place.radius, // in meters
          fillColor = Colors.translucent,
          strokeColor = Colors.translucent,
          strokeWidth = 2f // in pixels
        )
      }

      // center of screen marker for new place
      PlaceMarker(
        name = myViewModel.placeName,
        location = cameraPositionState.position.target,
        color = Colors.dark
      )
      Circle(
        center = cameraPositionState.position.target,
        radius = myViewModel.radius, // in meters
        fillColor = Colors.translucent,
        strokeColor = Colors.translucent,
        strokeWidth = 2f // in pixels
      )
    }
  }



//    TextButton(onClick = {  // save button
//
//      myViewModel.placeName = ""
//    }) {
//      Text("Save")
//    }
}

@Composable
fun PlaceMarker(
  name: String,
  location: LatLng,
  color: Color,
) {
  val markerState = MarkerState(location)
  MarkerComposable(
    state = markerState,
    title = name,
    anchor = Offset(0.5f, 1f),
  ) {
    Icon(
      imageVector = Icons.Default.Circle,
      contentDescription = name, tint = color,
      modifier = Modifier.scale(1.2f)
    )
    Icon(
      imageVector = Icons.Default.Flag,
      contentDescription = name, tint = Color.White,
      modifier = Modifier.scale(0.7f)
    )
  }
}
