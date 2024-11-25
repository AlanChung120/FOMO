package com.example.fomo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.screen.Screen
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.fomo.models.MyViewModel
import com.google.android.gms.maps.model.LatLng
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.ripple.rememberRipple
import com.example.fomo.const.Colors
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fomo.models.User
import io.ktor.client.request.request
import coil.compose.rememberAsyncImagePainter
import android.widget.Toast
import cafe.adriel.voyager.navigator.LocalNavigator


class FriendsScreen(private val myViewModel: MyViewModel) : Screen {
  @Composable
  override fun Content() {
    var selectedTab by remember { mutableStateOf(0)}

    fun onSelectTab(newTab: Int) {
      selectedTab = newTab
    }

    Column(
      verticalArrangement = Arrangement.spacedBy(24.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      Header()
      Nav(selectedTab, ::onSelectTab)
      when (selectedTab) {
        0 -> FriendsList(myViewModel)
        1 -> AddFriends(myViewModel)
        2 -> Requests(myViewModel)
      }
    }
  }
}

@Composable
fun Header() {
  Row(
    modifier = Modifier
      .padding(top = 16.dp)
  ) {
    Text(
      text = "Friends",
      fontWeight = FontWeight.ExtraBold,
      fontSize = 32.sp
    )
  }
}

val navCardTitles = arrayOf(
  "My Friends",
  "Add Friends",
  "Requests",
)

@Composable
fun Nav(selectedTab: Int, onSelectTab: (Int) -> Unit){
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    for((i, title) in navCardTitles.withIndex()) {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (i == selectedTab) Colors.primary else Color.LightGray),
        modifier = Modifier
          .clip(RoundedCornerShape(24.dp))
          .clickable (
            onClick = {onSelectTab(i)},
            // bounds ripple animation to rounded shape instead of rectangle
            indication = rememberRipple(bounded = true),
            interactionSource = remember { MutableInteractionSource() }
          )
      ) {
        Text(
          text = title,
          color = if (i == selectedTab) Color.White else Color.Black,
          fontWeight = if (i == selectedTab) FontWeight.SemiBold else FontWeight.Normal,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
  }
}

@Composable
fun RemoveFriendConfirmation(friend: User?, onDismissRequest: () -> Unit,
                             onConfirmation: () -> Unit, ) {
  AlertDialog(
    icon = {
    },
    title = {
      Text(text = "Confirmation")
    },
    text = {
      Text(text = "Are you sure you want to remove ${friend!!.displayName} as a friend?")
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



fun showNotification(context: Context, message: String) {
  val builder = NotificationCompat.Builder(context, "friend_request_channel")
    .setSmallIcon(R.drawable.notification_icon) // Replace with your icon
    .setContentTitle("New Friend Request")
    .setContentText(message)
    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

  with(NotificationManagerCompat.from(context)) {
    // Ensure permission is granted for Android 13+
    if (ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      Log.d("FriendsScreen","allowed")
      // Show the notification with a unique ID
      notify(2, builder.build())
    } else {
      Log.d("FriendsScreen","not Allowed")

    }
  }
}

@Composable
fun FriendsList(myViewModel: MyViewModel) {
  val navigator = LocalNavigator.current
  val context = LocalContext.current
  var isLoaded by remember { mutableStateOf<Boolean>(false) }
  var expanded = remember { mutableStateMapOf<String, Boolean>() }
  var showRemoveFriendConfirmation by remember { mutableStateOf<Boolean>(false) }
  var selectedFriend by remember {mutableStateOf<User?>(null)}
  val friends = myViewModel.friendsList
  val myLocation = myViewModel.center
  val friendsWithDistance = mutableListOf<Pair<User, Double>>()

  // Start of Remove Friend Confirmation
  fun openRemoveFriendConfirmation(friend: User) {
    selectedFriend = friend
    showRemoveFriendConfirmation = true
  }

  fun onDismissRequest() {
    selectedFriend = null
  }

  fun onConfirmationRequest() {
    showRemoveFriendConfirmation = false
    myViewModel.removeFriend(selectedFriend!!.username) { success ->
      if (success) {
        Toast.makeText(context, "Friend Removed", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Error: Friend Remove Failed", Toast.LENGTH_SHORT).show()
      }
    }
  }

  if (showRemoveFriendConfirmation && selectedFriend != null) {
    RemoveFriendConfirmation(selectedFriend, ::onDismissRequest, ::onConfirmationRequest)
  }
  // End of Remove Friend Confirmation

  // Creating list of friends with distances to sort by distance
  for(friend in friends) {
    val friendLocation = LatLng(friend.latitude, friend.longitude)
    val distance = myViewModel.calculateDistance(friendLocation, myLocation)
    friendsWithDistance.add(friend to distance)
  }
  friendsWithDistance.sortBy{ it.second }

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    for((friend, distance) in friendsWithDistance) {
      val friendStatus = myViewModel.statusList.filter{ it.id == friend.status_id }[0]
      val distanceText = if (distance > 1000)  "${distance.toInt() / 1000.0}km away" else "${distance.toInt()}m away"

      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(75.dp)
          .clickable {
            navigator?.push(MapScreen(myViewModel, LatLng(friend.latitude, friend.longitude)))
          }
      ) {
        Image(
          painter = rememberAsyncImagePainter(myViewModel.getImgUrl(friend.uid),
            error = painterResource(id = R.drawable.default_pfp) ),
          contentDescription = "Profile Picture",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(65.dp)
            .clip(CircleShape)
        )
        Column(
          verticalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier.fillMaxHeight()
        ) {
          Text(
            text = friend.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = distanceText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
          )
          Text(
            text = "${friendStatus.emoji} ${friendStatus.description}",
            fontSize = 16.sp,
          )
          Text(
            text = "${friendStatus.emoji} ${friendStatus.description}",
            fontSize = 16.sp,
          )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            modifier = Modifier.clickable(
              onClick = {
                expanded[friend.uid] = true
              }
            )
          )
          DropdownMenu(
            expanded = expanded[friend.uid] ?: false,
            onDismissRequest = { expanded[friend.uid] = false },
          ) {
            DropdownMenuItem(
              onClick = {
                openRemoveFriendConfirmation(friend)
              },
              text = {
                Text("Remove Friend")
              },
            )
          }
        }
      }
    }
  }
}

@Composable
fun AddFriends(myViewModel: MyViewModel) {
  val context = LocalContext.current
  var text by remember { mutableStateOf("") }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    OutlinedTextField(
      value = text,
      onValueChange = { text = it },
      label = { Text("Add Friend") },
      placeholder = { Text("Enter your friend's username") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done
      ),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .weight(1f)
        .padding(end = 2.dp)
    )

    TextButton(
      onClick = {
        myViewModel.createRequest(text) { success ->
          if (success) {
            Toast.makeText(context, "Sent Friend Request", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Error: User Not Found or Valid", Toast.LENGTH_SHORT).show()
          }
        }
      },
      modifier = Modifier
        .padding(top = 8.dp)
    ) {
      Text(
        text = "Add",
        color = MaterialTheme.colorScheme.primary, fontSize=18.sp)
    }
  }
}

@Composable
fun Requests(myViewModel: MyViewModel) {
  val context = LocalContext.current;
  val requestList = myViewModel.requestList
  var previousRequestCount by remember { mutableStateOf(requestList.size)}

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { isGranted ->
      if (isGranted) {
        showNotification(context, "You have a new friend request from ${requestList.lastOrNull()?.username ?: "someone"}")
      }
    }
  )

//   Check for new friend requests and show notification
  LaunchedEffect(requestList.size) {
    if (requestList.size > previousRequestCount) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
          ) != PackageManager.PERMISSION_GRANTED
        ) {
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
          showNotification(context, "You have a new friend request from ${requestList.lastOrNull()?.username ?: "someone"}")
        }
      } else {
        showNotification(context, "You have a new friend request from ${requestList.lastOrNull()?.username ?: "someone"}")
      }
    }
    previousRequestCount = requestList.size
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text("Requests: ${myViewModel.requestList.size}")
    for(request in myViewModel.requestList) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
          .height(75.dp)
          .fillMaxWidth()
      ) {
        Image(
          painter = rememberAsyncImagePainter(myViewModel.getImgUrl(request.uid),
            error = painterResource(id = R.drawable.default_pfp) ),
          contentDescription = "Profile Picture",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(75.dp)
            .clip(CircleShape)
        )
        Column(
          verticalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier.fillMaxHeight()
        ) {
          Text(
            text = request.username ?: "",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }

        // puts space between name and check to push check to far right
        Spacer(modifier = Modifier.weight(1f))

        Row(
          horizontalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier
            .fillMaxHeight()
            .padding(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Check, contentDescription = "Check Icon",
            modifier = Modifier.clickable (
              onClick = {
                myViewModel.acceptRequest(request.uid, myViewModel.uid) { success ->
                  if (success) {
                    Toast.makeText(context, "Friend Request Accepted", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "Error: Friend Request Accept Failed", Toast.LENGTH_SHORT).show()
                  }
                }
              }
            )
          )
          Icon(
            imageVector = Icons.Default.Close, contentDescription = "X Icon",
            modifier = Modifier.clickable (
              onClick = {
                myViewModel.declineRequest(request.uid, myViewModel.uid) { success ->
                  if (success) {
                    Toast.makeText(context, "Friend Request Declined", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "Error: Friend Request Decline Failed", Toast.LENGTH_SHORT).show()
                  }
                }
              }
            )
          )
        }
      }
    }
  }
}