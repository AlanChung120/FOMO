package com.example.fomo

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import com.example.fomo.const.Colors
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.fomo.models.MyViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import kotlinx.serialization.InternalSerializationApi


class MapScreen(private val myViewModel: MyViewModel, private val friendLocation: LatLng? = null) : Screen {
  @Composable
  override fun Content() {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      Map(myViewModel, friendLocation)
    }
  }
}

@Composable
fun CustomMapMarker(
  name: String,
  location: LatLng,
  color: Color
) {
  val markerState = rememberMarkerState(position = location)
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

@OptIn(InternalSerializationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Map(myViewModel: MyViewModel, friendLocation: LatLng?) {
  val context = LocalContext.current
  var isMapLoaded by remember { mutableStateOf(false) }
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(myViewModel.center, 15f)
  }
  val markerState = rememberMarkerState(
    key = "Selected Location",
  )
  var groupsExpanded by remember { mutableStateOf(false) }
  var statusExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(key1 = true){
    myViewModel.loadImage(context, myViewModel.getImgUrl(myViewModel.uid))
  }

  LaunchedEffect(key1 = myViewModel.friendsList) {
    for (friend in myViewModel.friendsList) {
      myViewModel.loadFriendImage(context, friend);
    }
  }

  Log.d("bitMapDescriptor", "${myViewModel.bitmapDescriptor}")


  // Update camera position whenever mapViewModel.center changes
  LaunchedEffect(myViewModel.center) {
    cameraPositionState.animate(
      CameraUpdateFactory.newLatLngZoom(myViewModel.center, 15f),
      1000 // Optional animation duration in milliseconds
    )
  }

  LaunchedEffect(myViewModel.selectedLocation) {
    if (myViewModel.selectedLocation != null) {
      markerState.position = myViewModel.selectedLocation!!
      markerState.showInfoWindow()
    }
  }

  LaunchedEffect(friendLocation) {
    friendLocation?.let {
      cameraPositionState.animate(
        CameraUpdateFactory.newLatLngZoom(it, 15f),
        1000 // Animation duration in milliseconds
      )
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Log.d("MapDebug", "API key is: ${BuildConfig.GOOGLE_MAPS_API_KEY}")
    Log.d("MapDebug", "Center is: ${myViewModel.center}")

    GoogleMap(
      modifier = Modifier.fillMaxSize(),
      cameraPositionState = cameraPositionState,
      onMapLoaded = {
        isMapLoaded = true
        Log.d("MapDebug", "Map loaded successfully")
      },
      onMapClick = { coords ->
        myViewModel.onMyWay(coords)
      },
      uiSettings = MapUiSettings(
        zoomControlsEnabled = false
      )
    ) {
      val isDataLoaded by myViewModel.isDataLoaded.collectAsState()
      val isSessionRestored by myViewModel.sessionRestored.collectAsState()
      if (isMapLoaded && isDataLoaded && isSessionRestored) {
        val userPosition = LatLng(myViewModel.userLatitude, myViewModel.userLongitude)  // Create LatLng object
        val icon = myViewModel.bitmapDescriptor

        // user avatar
          Marker(
            state = rememberMarkerState(key = "User Position", position = userPosition),
            title = myViewModel.displayName,
            snippet = "${myViewModel.status.emoji} ${myViewModel.status.description}",
            icon = icon  // Use the descriptor from ViewModel
          )

        // display friends
        val friendsList = if (myViewModel.groupIndex == -1) myViewModel.friendsList else myViewModel.groupMemberList
        for(friend in friendsList) {
          val friendLocation = LatLng(friend.latitude, friend.longitude)
          val friendStatus = myViewModel.statusList.filter {it.id == friend.status_id}[0]
          val friendMarkerState = rememberMarkerState(
            key = "friend " + friend.uid,
            position = friendLocation
          ).apply {
            showInfoWindow() // This ensures the info window is displayed when the marker is rendered
          }

          val friendIcon = myViewModel.friendIcons[friend.uid]

          Marker(
            state = friendMarkerState,
            title = friend.displayName,
            snippet = "${friendStatus.emoji} ${friendStatus.description}",
            icon = friendIcon,
          )

          // display friends' on my way routes
          if (friend.status_id == 2L && friend.destination_latitude != null && friend.destination_longitude != null) {
            Marker(
              state = rememberMarkerState(
                key = "${friend.displayName}'s destination",
                position = LatLng(friend.destination_latitude, friend.destination_longitude)
              ),
              title = "${friend.displayName} is on their way"
            )
            if (friend.route != null) {
              val routePoints = myViewModel.JSONToLatLngList(friend.route)
              Polyline(
                points = routePoints,
                color = Color.Blue,
                width = 10f,
              )
            }
          }
        }

        // display on my way route
        if (myViewModel.selectedLocation != null && myViewModel.status.description == "On my way") {
          Marker(
            state = markerState,
            title = "On my way",
          )
          markerState.showInfoWindow()
          if (myViewModel.route != null) {
            Polyline(
              points = myViewModel.route!!,
              color = Color.Blue,
              width = 10f,
            )
          }
        }

        // Special Places
        for (place in myViewModel.places) {
          val center = LatLng(place.latitude, place.longitude)
          Circle(
            center = center,
            radius = place.radius, // in meters
            fillColor = Colors.translucent,
            strokeColor = Colors.translucent,
            strokeWidth = 2f // in pixels
          )
          CustomMapMarker(name = place.name, location = center, color = Colors.dark)
        }

        // special locations

      }
    }

    // Friend groups / circles dropdown
    ExposedDropdownMenuBox(
      expanded = groupsExpanded,
      onExpandedChange = {
        groupsExpanded = !groupsExpanded
      },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(16.dp)
    ) {
      OutlinedTextField(
        value = if (myViewModel.groupIndex == -1) "All Friends" else myViewModel.groupList[myViewModel.groupIndex].name,
        onValueChange = {},
        readOnly = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupsExpanded) },
        colors = TextFieldDefaults.colors(
          focusedIndicatorColor = Color.Black,
          unfocusedContainerColor = Color.White,
          focusedContainerColor = Color.White,
        ),
        shape = RoundedCornerShape(8.dp),
        textStyle = TextStyle(
          fontSize = 14.sp,
        ),
        modifier = Modifier
          .height(48.dp)
          .width(300.dp)
          .menuAnchor()
      )

      ExposedDropdownMenu(
        expanded = groupsExpanded,
        onDismissRequest = { groupsExpanded = false },
        modifier = Modifier.background(Color.White)
      ) {
        // Show all friends (not a group)
        DropdownMenuItem(
          text = {
            Text(
              text = "All Friends",
              fontWeight = if (myViewModel.groupIndex == -1) FontWeight.Bold else FontWeight.Normal
            )},
          onClick = {
            myViewModel.groupIndex = -1
            groupsExpanded = false
          },
        )
        // Friend groups
        myViewModel.groupList.forEachIndexed { i, group ->
          DropdownMenuItem(
            text = {
              Text(
                text = group.name,
                fontWeight = if (i == myViewModel.groupIndex) FontWeight.Bold else FontWeight.Normal
              )},
            onClick = {
              myViewModel.groupIndex = i
              myViewModel.getGroupMembers(context, myViewModel.groupList[i].id!!) { result ->
                myViewModel.friendsList = result
              }
              groupsExpanded = false
            },
          )
        }
      }
    }

    // Status dropdown
    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
    ) {
      Box(
        modifier = Modifier
          .padding(6.dp)
          .border(
            width = 1.dp, // Set the border width
            color = Colors.primary, // Choose your border color
            shape = RoundedCornerShape(24.dp) // Match the button's corner shape
          )
      ) {
        Button(
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
          ),
          shape = RoundedCornerShape(24.dp),
          onClick = { statusExpanded = !statusExpanded },
          modifier = Modifier
            .padding(0.dp) // No padding inside the Box
        ) {
          Text(
            text = "${myViewModel.status.emoji} ${myViewModel.status.description}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
          )
        }
      }

      // Dropdown menu items
      DropdownMenu(
        expanded = statusExpanded,
        onDismissRequest = { statusExpanded = false },
        modifier = Modifier
          .width(200.dp)
          .heightIn(max = 500.dp) // Set max height of the dropdown
          .background(Color.White)
          .padding(8.dp)
          .background(Color.White, shape = RoundedCornerShape(10.dp)),
        offset = DpOffset(x = 0.dp, y = 0.dp),
        properties = PopupProperties(focusable = true),
      ) {
        myViewModel.statusList.forEach { activity ->
          if (activity.description != "Idle") {
            DropdownMenuItem(
              text = { Text(text = "${activity.emoji} ${activity.description}") },
              onClick = {
                myViewModel.updateStatus(activity)
                statusExpanded = false
              })
          }
        }
      }
    }

    val modesList: Array<Pair<String, @Composable () -> Unit>> = arrayOf(
      Pair("walking", {Icon(imageVector = Icons.AutoMirrored.Default.DirectionsWalk, contentDescription = "Walking")}),
      Pair("bicycling", {Icon(imageVector = Icons.Default.PedalBike, contentDescription = "Bicycling")}),
      Pair("driving", {Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = "Car")}),
      Pair("transit", {Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = "Bus")}),
    )

    if (myViewModel.selectedLocation != null && myViewModel.status.description == "On my way") {
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(16.dp)
      )
      {
        for ((modeString, icon) in modesList) {
          Button(
            onClick = {
              myViewModel.mode = modeString
              myViewModel.onMyWay(myViewModel.selectedLocation!!)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (modeString == myViewModel.mode) Colors.primary else Colors.greyed
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.size(64.dp)
          ) {
            icon()
          }
        }
      }
    }
  }
}
