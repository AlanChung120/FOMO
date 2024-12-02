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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import cafe.adriel.voyager.core.screen.Screen
import com.example.fomo.BuildConfig
import com.example.fomo.consts.Colors
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.fomo.viewmodel.MyViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.coroutines.launch

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
  val scope = rememberCoroutineScope()

  val userMarkerState = rememberMarkerState(key = "User Position", position = LatLng(myViewModel.userLatitude, myViewModel.userLongitude))

  var groupsExpanded by remember { mutableStateOf(false) }
  var statusExpanded by remember { mutableStateOf(false) }
  var showAddPlace by remember {mutableStateOf(false)}
  var placeName by remember { mutableStateOf("") }

  LaunchedEffect(key1 = true){
    myViewModel.loadImage(context, myViewModel.getImgUrl(myViewModel.uid))
  }

  LaunchedEffect(myViewModel.userLatitude, myViewModel.userLongitude){
    userMarkerState.position = LatLng(myViewModel.userLatitude, myViewModel.userLongitude)
  }

  LaunchedEffect(key1 = myViewModel.friendsList) {
    for (friend in myViewModel.friendsList) {
      myViewModel.loadFriendImage(context, friend);
    }
  }

  Log.d("bitMapDescriptor", "${myViewModel.bitmapDescriptor}")


  // Update camera position whenever myViewModel.center changes
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
        val icon = myViewModel.bitmapDescriptor
        val userPlace = myViewModel.getUserPlace(LatLng(myViewModel.userLatitude, myViewModel.userLongitude) , true)
        var userMessage = ""
        if (userPlace == null) {
          userMessage = "${myViewModel.status.emoji} ${myViewModel.status.description}"
        } else {
          userMessage = "${userPlace.name}: ${myViewModel.status.emoji} ${myViewModel.status.description}"
        }

        // user avatar
          Marker(
            state = userMarkerState,
            title = myViewModel.displayName,
            snippet = userMessage,
            icon = icon  // Use the descriptor from ViewModel
          )
        Log.d("mapdebug", "marker loaded at ${myViewModel.userLatitude}")

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
          val friendPlace = myViewModel.getUserPlace(friendLocation, false)
          var friendMessage = ""
          if (friendPlace == null) {
            friendMessage = "${friendStatus.emoji} ${friendStatus.description}"
          } else {
            friendMessage = "${friendPlace.name}: ${friendStatus.emoji} ${friendStatus.description}"
          }

          Marker(
            state = friendMarkerState,
            title = friend.displayName,
            snippet = friendMessage,
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
            myViewModel.fetchPlaces() // reset places
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
              myViewModel.fetchPlaces() // fetch new group places
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

    if (showAddPlace) {
      Dialog(onDismissRequest = { showAddPlace = false }) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.background,
          modifier = Modifier.padding(16.dp)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Enter New Place Name")

            Spacer(modifier = Modifier.height(8.dp))

            // TextField to input new place name
            OutlinedTextField(
              value = placeName,
              onValueChange = { placeName = it },
              label = { Text("New Place Name") },
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
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
          .padding(16.dp, top = 80.dp)
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
