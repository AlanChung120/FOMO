package com.example.fomo.views

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.rememberAsyncImagePainter
import com.example.fomo.R
import com.example.fomo.consts.Colors
import com.example.fomo.entities.Group
import com.example.fomo.entities.Place
import com.example.fomo.entities.User
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
import com.google.maps.android.compose.rememberTileOverlayState
import com.google.maps.android.ktx.model.cameraPosition
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.InternalSerializationApi

class PlaceScreen(private val myViewModel: MyViewModel) : Screen {
  @Composable
  override fun Content() {
    val isDataLoaded by myViewModel.isDataLoaded.collectAsState()
    val isSessionRestored by myViewModel.sessionRestored.collectAsState()

    if (isDataLoaded && isSessionRestored) {
      if (!myViewModel.addingPlace) {
        Column(
          verticalArrangement = Arrangement.spacedBy(24.dp),
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          PlaceHeader()
          PlaceNav(myViewModel)
          PlaceList(myViewModel)
        }
      } else {
        Column {
          AddPlace(myViewModel)
        }
      }
    }
  }
}

@Composable
fun PlaceHeader() {
  Row(
    modifier = Modifier
      .padding(top = 16.dp)
  ) {
    Text(
      text = "Places",
      fontWeight = FontWeight.ExtraBold,
      fontSize = 32.sp
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceNav(myViewModel: MyViewModel) {
  var expanded by remember { mutableStateOf(false) }
  val groupIndex by myViewModel.groupIndex.collectAsState()
  val context = LocalContext.current
  var showLeaveGroupConfirmation by remember { mutableStateOf(false) }
  var selectedGroup by remember { mutableStateOf<Group?>(null) }

  fun onConfirmationRequest() {
    myViewModel.removeGroup(context, selectedGroup!!.id!!) { success ->
      if (success) {
        Toast.makeText(context, "Group Removed", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Error: Group Remove Failed", Toast.LENGTH_SHORT).show()
      }
    }
  }

  if (showLeaveGroupConfirmation && selectedGroup != null) {
    Confirmation(
      "Are you sure you want to leave ${selectedGroup!!.name}?",
      onDismissRequest = { showLeaveGroupConfirmation = false },
      onConfirmation = ::onConfirmationRequest
    )
  }

  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
  ) {
    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = {
        expanded = !expanded
      },
    ) {
      OutlinedTextField(
        value = if (groupIndex == -1) "All Friends" else myViewModel.groupList[groupIndex].name,
        onValueChange = {},
        readOnly = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        colors = TextFieldDefaults.colors(
          focusedIndicatorColor = Color.Black,
          unfocusedContainerColor = Color.Transparent,
          focusedContainerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .menuAnchor()
      )

      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        // Show all friends (not a group)
        DropdownMenuItem(
          text = {
            Text(
              text = "All Friends",
              fontWeight = if (groupIndex == -1) FontWeight.Bold else FontWeight.Normal
            )
          },
          onClick = {
            myViewModel.selectGroup(-1)
            expanded = false
            myViewModel.fetchPlaces() // reset places
          }
        )
        myViewModel.groupList.forEachIndexed { i, group ->
          DropdownMenuItem(
            text = {
              Row {
                Text(
                  text = group.name,
                  fontWeight = if (i == groupIndex) FontWeight.SemiBold
                               else FontWeight.Normal
                )
                Spacer(Modifier.weight(1f))
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .clickable {
                      selectedGroup = group
                      showLeaveGroupConfirmation = true
                      expanded = false
                    }
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Group Remove",
                    tint = Color.Red
                  )
                }
              }},
            onClick = {
              myViewModel.selectGroup(i)
              myViewModel.getGroupMembers(context, myViewModel.groupList[i].id!!) {}
              myViewModel.fetchPlaces() // fetch new group places
              expanded = false
            }
          )
        }
      }
    }

    if (groupIndex != -1) {
      Button(
        onClick = { myViewModel.addingPlace = true },
        colors = ButtonDefaults.buttonColors(
          containerColor = Colors.primary
        ),
        modifier = Modifier
          .height(55.dp)
          .padding(horizontal = 7.dp),
        shape = RoundedCornerShape(6.dp)
      ) {
        Icon(imageVector = Icons.Default.AddBusiness, contentDescription = "Add Friend")
      }
    }
  }
}

@Composable
fun PlaceList(myViewModel: MyViewModel) {
  val navigator = LocalNavigator.current
  val context = LocalContext.current
  val groupIndex by myViewModel.groupIndex.collectAsState()
  val expanded = remember { mutableStateMapOf<Long, Boolean>() }
  var showRemovePlaceConfirmation by remember { mutableStateOf(false) }
  var selectedPlace by remember { mutableStateOf<Place?>(null)}

  fun onConfirmationRequest() {
    showRemovePlaceConfirmation = false
    myViewModel.removePlace(selectedPlace!!.id!!) { success ->
      if (success) {
        Toast.makeText(context, "Place Removed", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Error: Place Remove Failed", Toast.LENGTH_SHORT).show()
      }
    }
  }

  if (showRemovePlaceConfirmation && selectedPlace != null) {
    Confirmation(
      text = "Are you sure you want to remove ${selectedPlace!!.name}?",
      onDismissRequest = { showRemovePlaceConfirmation = false },
      onConfirmation = ::onConfirmationRequest)
  }

  if (groupIndex != -1) {
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      for (place in myViewModel.places) {
        val distance =
          myViewModel.calculateDistance(myViewModel.center, LatLng(place.latitude, place.longitude))
        val distanceText =
          if (distance > 1000) "${distance.toInt() / 1000.0}km away" else "${distance.toInt()}m away"

        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .height(75.dp)
            .clickable {
              navigator?.push(MapScreen(myViewModel, LatLng(place.latitude, place.longitude)))
            }
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(65.dp)
              .background(color = Colors.primary, shape = CircleShape)
          ) {
            Icon(
              imageVector = Icons.Default.Business,
              contentDescription = "Place Icon",
              tint = Color.White,
              modifier = Modifier.size(32.dp) // Adjust the icon size as needed
            )
          }
          Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxHeight()
          ) {
            Text(
              text = place.name,
              fontSize = 16.sp,
              fontWeight = FontWeight.Normal,
            )
            Text(
              text = distanceText,
              fontSize = 16.sp,
              fontWeight = FontWeight.Light,
            )
          }
          Spacer(modifier = Modifier.weight(1f))
          Box {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              modifier = Modifier.clickable(
                onClick = {
                  expanded[place.id!!] = true
                }
              )
            )
            DropdownMenu(
              expanded = expanded[place.id!!] ?: false,
              onDismissRequest = { expanded[place.id] = false },
            ) {
              DropdownMenuItem(
                onClick = {
                  expanded[place.id] = false
                  selectedPlace = place
                  showRemovePlaceConfirmation = true
                },
                text = {
                  Text("Remove Place")
                },
              )
            }
          }
        }
      }
    }
  } else {
    Box(
      modifier = Modifier.padding(start = 4.dp)
    ) {
      Text(
        text = "Select a group to view places"
      )
    }
  }
}

@Composable
fun PlaceSettings(myViewModel: MyViewModel, cameraPositionState: CameraPositionState) {
  val context = LocalContext.current

  fun reset() {
    myViewModel.placeName = ""
    myViewModel.radius = 100.0
  }

  Box(
    modifier = Modifier
      .height(280.dp)
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      OutlinedTextField(
        value = myViewModel.placeName,
        onValueChange = { newName -> myViewModel.placeName = newName },
        label = { Text("Place Name") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
      )

      // Slider for radius
      Column {
        Text(text = "Radius: ${myViewModel.radius.toInt()} meters", style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.Slider(
          value = myViewModel.radius.toFloat(),
          onValueChange = { newRadius -> myViewModel.radius = newRadius.toDouble() },
          valueRange = 1f..1000f, // Adjust range as needed
          modifier = Modifier.fillMaxWidth()
        )
      }

      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Button(
          onClick = {
            reset()
            myViewModel.addingPlace = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Colors.lightgray,
            contentColor = Color.Black
          ),
          modifier = Modifier
            .padding(top = 8.dp)
        ) {
          Text(
            text = "Cancel",
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(4.dp)
          )
        }

          Button(
            onClick = {
              myViewModel.createPlace(
                cameraPositionState.position.target,
                myViewModel.radius,
                myViewModel.placeName
              ) { success ->
                if (success) {
                  Toast.makeText(context, "Place Created", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(
                    context, "Error: Something went wrong",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              }
              reset()
              myViewModel.addingPlace = false
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


@Composable
fun Confirmation(text: String, onDismissRequest: () -> Unit,
                            onConfirmation: () -> Unit, ) {
  AlertDialog(
    icon = {
    },
    title = {
      Text(text = "Confirmation")
    },
    text = {
      Text(text = text)
    },
    onDismissRequest = {
      onDismissRequest()
    },
    confirmButton = {
      TextButton(
        onClick = {
          onConfirmation()
        }
      ) {
        Text("Confirm")
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onDismissRequest()
        }
      ) {
        Text("Dismiss")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceMap(myViewModel: MyViewModel, cameraPositionState: CameraPositionState) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isMapLoaded by remember { mutableStateOf(false) }
  val groupIndex by myViewModel.groupIndex.collectAsState()
  var groupsExpanded by remember { mutableStateOf(false) }
  var placesExpanded by remember { mutableStateOf(false) }

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
      if (groupIndex != -1) {
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
  }
}

@Composable
fun AddPlace(myViewModel: MyViewModel) {
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
    anchor = Offset(0.5f, 0.5f),
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
