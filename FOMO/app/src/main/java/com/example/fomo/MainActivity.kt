package com.example.fomo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import com.example.fomo.viewmodel.MyViewModel
import com.example.fomo.consts.Colors
import com.example.fomo.ui.theme.FOMOTheme
import com.example.fomo.util.LocationHelper
import com.example.fomo.views.FriendsScreen
import com.example.fomo.views.MapScreen
import com.example.fomo.views.PlaceScreen
import com.example.fomo.views.ProfileScreen
import com.example.fomo.views.SettingsScreen
import com.example.fomo.views.SignInScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
  private val myViewModel: MyViewModel by viewModels()
  private val coroutineScope = CoroutineScope(Dispatchers.Main)

  @RequiresApi(Build.VERSION_CODES.Q)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    createNotificationChannel()
    createFriendRequestChannel()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        ActivityCompat.requestPermissions(
          this,
          arrayOf(Manifest.permission.POST_NOTIFICATIONS),
          NOTIFICATION_PERMISSION_REQUEST_CODE
        )
      } else {
        showNotification()
      }
    } else {
      showNotification()
    }


    val backgroundLocationGranted = LocationHelper.isBackgroundLocationPermissionGranted(this)
    val locationPermissionsAlreadyGranted = LocationHelper.areLocationPermissionsGranted(this)

    enableEdgeToEdge()
    myViewModel.restoreSession(this) {success ->
      if (success) {
        myViewModel.setSignedInState(true)
        Log.d("Supabase-Auth Session", "Session restored successfully")
      } else {
        Log.d("Supabase-Auth Session", "No sessions saved")
      }
      setContent {
        val myViewModel: MyViewModel = viewModel()

        FOMOTheme {
          LocationHelper.LocationChecker(
            locationPermissionsAlreadyGranted,
            backgroundLocationGranted,
            myViewModel,
            this
          )
          NavigatorFun(myViewModel = myViewModel)
        }
      }
    }

    updateData(this)
  }


  private fun createFriendRequestChannel(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Friend Requests"
      val descriptionText = "Notifications for new friend requests"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel("friend_request_channel", name, importance).apply {
        description = descriptionText
      }
      val notificationManager: NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }
  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "myChannel"
      val descriptionText = "Notification Channel Description"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel("my_channel_id", name, importance).apply {
        description = descriptionText
      }
      val notificationManager: NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
      Log.d("MainActivity", "Notification channel created")
    }
  }

  private fun showNotification(){
    val builder = NotificationCompat.Builder(this, "my_channel_id")
      .setSmallIcon(R.drawable.notification_icon)
      .setContentTitle("Welcome!")
      .setContentText("Thank you for opening the app")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(this)) {
      // Ensure permission is granted for Android 13+
      if (ActivityCompat.checkSelfPermission(
          this@MainActivity,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        Log.d("MainActivity","allowed")
        // Show the notification with a unique ID
        notify(1, builder.build())
      } else {
        Log.d("MainActivity","not Allowed")

      }
    }
  }

  private fun updateData(context: Context) {
    lifecycleScope.launch {
      myViewModel.signedIn.collect { isSignedIn ->
        if (isSignedIn) {
          while (isActive) {
            myViewModel.fetchFriends(context)
            myViewModel.fetchPlaces()
            myViewModel.fetchGroups(context)
            LocationHelper.getPreciseLocation(this@MainActivity, myViewModel)
            delay(5000)
            Log.d("updateData", "data has been updated")
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    coroutineScope.cancel()
  }
}
@Composable
fun LoadingScreen() {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.fillMaxSize()
  ) {
    Text(
      "LOADING",
      fontSize = 16.sp,
      modifier = Modifier.padding(vertical = 8.dp)
    )
  }
}
@Composable
fun NavigatorFun(myViewModel: MyViewModel) {
  val isSignedIn by myViewModel.signedIn.collectAsState()
  val sessionRestored by myViewModel.sessionRestored.collectAsState()

  if (!sessionRestored) {
    // Show a loading indicator while session restoration is in progress
    LoadingScreen()
  } else {
    val startScreen = if (isSignedIn) MapScreen(myViewModel) else SignInScreen(myViewModel)
    Navigator(startScreen) { Content(myViewModel) }
  }
}


@Composable
fun Content(myViewModel: MyViewModel) {
  if (myViewModel.signedIn.collectAsState().value) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = { Navbar(viewModel = myViewModel) }
    ) {
        innerPadding ->
      // Pass the innerPadding as a modifier to the Content
      Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        CurrentScreen() // Show the current screen
      }
    }
  } else {
    CurrentScreen() // Show the current screen
  }
}

@Composable
fun State(state: String, modifier: Modifier = Modifier) {
  Text(state, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navbar(viewModel: MyViewModel) {
  val navigator = LocalNavigator.current
  val items: Array<Pair<() -> Unit, @Composable () -> Unit>> = arrayOf(
    Pair({ navigator?.push(MapScreen(viewModel)) }, {Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Map Icon")}),
    Pair({ navigator?.push(PlaceScreen(viewModel)) }, {Icon(imageVector = Icons.Default.House, contentDescription = "Places Icon")}),
    Pair({ navigator?.push(FriendsScreen(viewModel)) }, {Icon(imageVector = Icons.Default.People, contentDescription = "Friends Icon")}),
    Pair({ navigator?.push(ProfileScreen(viewModel)) }, {Icon(imageVector = Icons.Default.Person, contentDescription = "Profile Icon")}),
    Pair({ navigator?.push(SettingsScreen(viewModel)) }, {Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings Icon")}),
  )
  CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
      for ((onClick, icon) in items) {
        Button(
          onClick = onClick,
          modifier = Modifier.weight(1f),
          shape = RectangleShape,
          contentPadding = PaddingValues(top = 25.dp, bottom = 25.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Colors.primary)
        ) {
          icon()
        }
      }
    }
  }
}
