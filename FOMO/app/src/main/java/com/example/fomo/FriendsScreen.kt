package com.example.fomo

  import android.Manifest
  import android.app.NotificationChannel
  import android.app.NotificationManager
  import android.content.Context
  import android.content.pm.PackageManager
  import android.graphics.Paint.Align
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
  import android.widget.Space
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
  import androidx.compose.foundation.background
  import androidx.compose.foundation.layout.width
  import androidx.compose.foundation.layout.wrapContentSize
  import androidx.compose.foundation.layout.wrapContentWidth
  import androidx.compose.material.icons.filled.Add
  import androidx.compose.material.icons.filled.ArrowDropDown
  import androidx.compose.material.icons.filled.LocationOn
  import androidx.compose.material.icons.filled.Notifications
  import androidx.compose.material.icons.filled.PersonAdd
  import androidx.compose.material.icons.filled.PersonAddAlt
  import androidx.compose.material.icons.filled.PersonAddAlt1
  import androidx.compose.material.icons.outlined.PersonAdd
  import androidx.compose.material3.Button
  import androidx.compose.material3.ButtonDefaults
  import androidx.compose.material3.ExperimentalMaterial3Api
  import androidx.compose.material3.ExposedDropdownMenuBox
  import androidx.compose.material3.ExposedDropdownMenuDefaults
  import androidx.compose.material3.TextField
  import androidx.compose.material3.TextFieldColors
  import androidx.compose.material3.TextFieldDefaults
  import androidx.compose.runtime.MutableState
  import androidx.compose.runtime.collectAsState
  import androidx.compose.runtime.mutableIntStateOf
  import androidx.compose.ui.window.Dialog
  import androidx.lifecycle.viewmodel.compose.viewModel
  import com.example.fomo.models.Group
  import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

class FriendsScreen(private val myViewModel: MyViewModel) : Screen {
    @Composable
    override fun Content() {
      var isFriendDialogOpen by remember { mutableStateOf(false )}
      var isGroupDialogOpen by remember { mutableStateOf(false) }
      var isGroupReqDialogOpen by remember { mutableStateOf(false) }
      val selectedItemIndex = remember { mutableIntStateOf(-1) } // = mutableState so it can be changed when passed as param
      var selectedGroupReq by remember { mutableStateOf<Pair<User, Group>?>(null) }
      val friendsList = remember { mutableStateOf<List<User>>(myViewModel.friendsList)}
      val isDataLoaded by myViewModel.isDataLoaded.collectAsState()
      val isSessionRestored by myViewModel.sessionRestored.collectAsState()

      fun openFriendModal() {
        isFriendDialogOpen = true
      }

      fun openGroupModal() {
        isGroupDialogOpen = true
      }

      fun openGroupRequest(request: Pair<User, Group>) {
        selectedGroupReq = request
        isGroupReqDialogOpen = true
      }

      if (isDataLoaded && isSessionRestored) {
        Column(
          verticalArrangement = Arrangement.spacedBy(24.dp),
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          Header()
          Nav(
            selectedItemIndex,
            ::openFriendModal,
            ::openGroupModal,
            ::openGroupRequest,
            friendsList,
            myViewModel
          )
          AddFriend(
            isOpen = isFriendDialogOpen,
            onDismissRequest = { isFriendDialogOpen = false },
            selectedItemIndex.intValue,
            friendsList.value,
            myViewModel
          )
          CreateGroup(
            isOpen = isGroupDialogOpen,
            onDismissRequest = { isGroupDialogOpen = false },
            selectedItemIndex,
            friendsList,
            myViewModel
          )
          if (selectedGroupReq != null) {
            GroupRequest(
              isOpen = isGroupReqDialogOpen,
              onDismissRequest = { isGroupReqDialogOpen = false },
              selectedGroupReq!!,
              friendsList,
              selectedItemIndex,
              myViewModel,
            )
          }
          FriendsList(
            selectedItemIndex.intValue,
            friendsList.value,
            myViewModel
          )
        }
      } else {
        LoadingScreen()
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

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun Nav(selectedItemIndex: MutableState<Int>, openAddFriend: () -> Unit, openCreateGroup: () -> Unit,
          openGroupRequest: (Pair<User, Group>) -> Unit, friendsList: MutableState<List<User>>, myViewModel: MyViewModel) {
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

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
        modifier = Modifier.width(250.dp)
      ) {
        OutlinedTextField(
          value = if (selectedItemIndex.value == -1) "All Friends" else myViewModel.groupList[selectedItemIndex.value].name,
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
                fontWeight = if (selectedItemIndex.value == -1) FontWeight.Bold else FontWeight.Normal
              )},
            onClick = {
              selectedItemIndex.value = -1
              expanded = false
              friendsList.value = myViewModel.friendsList
            }
          )
          // Group Requests
          for ((sender, group) in myViewModel.groupRequestList) {
            DropdownMenuItem(
              text = {
                Row {
                  Text(
                    text = group.name,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Spacer(Modifier.weight(1f))
                  Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Group invite",
                    modifier = Modifier.size(20.dp)
                  )
                }
              },
              onClick = {
                openGroupRequest(Pair(sender, group))
                expanded = false
              }
            )
          }
          // Friend groups
          myViewModel.groupList.forEachIndexed { i, group ->
            DropdownMenuItem(
              text = {
                Text(
                  text = group.name,
                  fontWeight = if (i == selectedItemIndex.value) FontWeight.Bold else FontWeight.Normal
                )},
              onClick = {
                selectedItemIndex.value = i
                myViewModel.getGroupMembers(context, myViewModel.groupList[i].id!!) { result ->
                  friendsList.value = result
                }
                expanded = false
              }
            )
          }
          // Create new friend group
          DropdownMenuItem(
            text = {
             Row {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = "Add Icon",
                  modifier = Modifier.size(20.dp)
                )
              }
            },
            onClick = {
              openCreateGroup()
              expanded = false
            }
          )
        }
      }

      Button(
        onClick = openAddFriend,
        colors = ButtonDefaults.buttonColors(
          containerColor = Colors.primary
        ),
        modifier = Modifier.height(50.dp),
        shape = RoundedCornerShape(8.dp)
      ) {
        if (selectedItemIndex.value == -1) {
          Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Friend")
        } else {
          Icon(imageVector = Icons.Default.PersonAddAlt1, contentDescription = "Add Friend to Group")
        }

      }
    }
  }

  @Composable
  fun FriendsList(selectedItemIndex: Int, friendsList: List<User>, myViewModel: MyViewModel) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var showRemoveFriendConfirmation by remember { mutableStateOf<Boolean>(false) }
    var selectedFriend by remember {mutableStateOf<User?>(null)}
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
      myViewModel.removeFriend(context, selectedFriend!!.username) { success ->
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

    // List of Friend Requests (above friends)
    if (selectedItemIndex == -1) {
      Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        for (requester in myViewModel.requestList) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(60.dp)
          ) {
            Image(
              painter = rememberAsyncImagePainter(
                myViewModel.getImgUrl(requester.uid),
                error = painterResource(id = R.drawable.default_pfp)
              ),
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
                text = requester.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Accept Friend Request",
                modifier = Modifier.clickable(
                  onClick = {
                    myViewModel.acceptRequest(context, requester.uid, myViewModel.uid) {result ->
                      if (result) {
                        Toast.makeText(context, "Successfully accepted friend request", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Error: Something went wrong", Toast.LENGTH_SHORT).show()
                      }
                    }
                  }
                )
              )
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Accept Friend Request",
                modifier = Modifier.clickable(
                  onClick = {
                    myViewModel.declineRequest(context, requester.uid, myViewModel.uid) {result ->
                      if (result) {
                        Toast.makeText(context, "Successfully declined friend request", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Error: Something went wrong", Toast.LENGTH_SHORT).show()
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

    // Creating list of friends with distances to sort by distance
    for(friend in friendsList) {
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
          modifier = Modifier
            .height(75.dp)
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
          if (selectedItemIndex == -1) {
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
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun AddFriend(isOpen: Boolean, onDismissRequest: () -> Unit, selectedItemIndex: Int,
                friendsList: List<User>, myViewModel: MyViewModel) {
    if (isOpen) {
      val context = LocalContext.current
      var text by remember { mutableStateOf("") }
      var expanded by remember { mutableStateOf(false) }
      var selectedFriend by remember { mutableStateOf<User?>(null)}

      Dialog(onDismissRequest = onDismissRequest) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .height(300.dp)
        ) {
          Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .padding(horizontal = 32.dp, vertical = 32.dp)
              .fillMaxSize()
          ) {
            Text(
              text = if (selectedItemIndex == -1) "Add Friend" else "Add Friend to Group",
              fontWeight = FontWeight.Bold,
              fontSize = 24.sp,
              modifier = Modifier.padding(top = 8.dp)
            )

            if (selectedItemIndex == -1) {
              OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter Username") },
                placeholder = { Text("eg: bobthetroll") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Text,
                  imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(16.dp),
              )
            } else {
              Box (
                modifier = Modifier
                  .fillMaxWidth()
                  .wrapContentSize(Alignment.TopStart)
              ) {
                ExposedDropdownMenuBox(
                  expanded = expanded,
                  onExpandedChange = {
                    expanded = !expanded
                  },
                ) {
                  OutlinedTextField(
                    value = if (selectedFriend != null) selectedFriend!!.displayName else "Select a friend",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                      Icon(
                        imageVector = Icons.Default.ArrowDropDown, // Use a dropdown arrow icon
                        contentDescription = "Dropdown icon"
                      )
                    },
                    colors = TextFieldDefaults.colors(
                      focusedIndicatorColor = Color.Black,
                      unfocusedContainerColor = Color.Transparent,
                      focusedContainerColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.menuAnchor()
                  )

                  ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                  ) {

                    for (friend in myViewModel.friendsList) {
                      DropdownMenuItem(
                        text = {
                          Text(
                            text = friend.displayName,
                          )
                        },
                        onClick = {
                          selectedFriend = friend
                          expanded = false
                        }
                      )
                    }
                  }
                }
              }
            }

            Row(
              horizontalArrangement = Arrangement.End,
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = {
                  if (selectedItemIndex == -1) {
                    myViewModel.createRequest(context, text) { success ->
                      if (success) {
                        Toast.makeText(context, "Sent Friend Request", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Error: User Not Found or Invalid", Toast.LENGTH_SHORT).show()
                      }
                    }
                  } else {
                    if (selectedFriend != null) {
                      myViewModel.createGroupRequest(context, myViewModel.groupList[selectedItemIndex].id!!, selectedFriend!!.uid) { success ->
                        if (success) {
                          Toast.makeText(context, "Sent Invite to Group", Toast.LENGTH_SHORT).show()
                        } else {
                          Toast.makeText(context, "Error: User Not Found or Invalid", Toast.LENGTH_SHORT).show()
                        }
                      }
                    } else {
                      Toast.makeText(context, "Did not select a friend to add", Toast.LENGTH_SHORT).show()
                    }
                  }
                  onDismissRequest()
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
    }
  }

  @Composable
  fun CreateGroup(isOpen: Boolean, onDismissRequest: () -> Unit, selectedItemIndex: MutableState<Int>,
                  friendsList: MutableState<List<User>>, myViewModel: MyViewModel) {
    if (isOpen) {
      val context = LocalContext.current
      var text by remember { mutableStateOf("") }

      Dialog(onDismissRequest = onDismissRequest) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .height(300.dp)
        ) {
          Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .padding(horizontal = 32.dp, vertical = 32.dp)
              .fillMaxSize()
          ) {
            Text(
              text = "Create Group",
              fontWeight = FontWeight.Bold,
              fontSize = 24.sp,
              modifier = Modifier.padding(top = 8.dp)
            )

            OutlinedTextField(
              value = text,
              onValueChange = { text = it },
              label = { Text("Enter group name") },
              placeholder = { Text("eg: awesome group") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
              ),
              shape = RoundedCornerShape(16.dp),
            )

            Row(
              horizontalArrangement = Arrangement.End,
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = {
                  myViewModel.createGroup(context, text) { result ->
                    if (result != -1L) {
                      Toast.makeText(context, "Group Created", Toast.LENGTH_LONG).show()
                      myViewModel.getGroupMembers(context, result) { members ->
                        friendsList.value = members
                        selectedItemIndex.value = myViewModel.groupList.indexOfFirst { it.id == result }
                      }
                    } else {
                      Toast.makeText(context, "Error: Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                    }
                  }
                  onDismissRequest()
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
    }
  }

  @Composable
  fun GroupRequest(isOpen: Boolean, onDismissRequest: () -> Unit, selectedGroupReq: Pair<User, Group>,
                   friendsList: MutableState<List<User>>, selectedItemIndex: MutableState<Int>, myViewModel: MyViewModel) {
    if (isOpen) {
      val sender: User = selectedGroupReq.first
      val group: Group = selectedGroupReq.second
      val context = LocalContext.current

      Dialog(onDismissRequest = onDismissRequest) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .height(300.dp)
        ) {
          Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .padding(horizontal = 32.dp, vertical = 32.dp)
              .fillMaxSize()
          ) {
            Column {
              Text(
                text = "Group Invite",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 8.dp)
              )
              Text(
                text = "Invite to \"${group.name}\"",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp)
              )
              Text(
                text = "Sent by ${sender.displayName}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
            Column(
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxHeight()
            ) {
              Row (
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                  .fillMaxWidth()
              ) {
                Button(
                  onClick = {
                    myViewModel.acceptGroupRequest(context, group.id!!) { result ->
                      if (result != -1L) {
                        Toast.makeText(context, "Request Accepted", Toast.LENGTH_SHORT).show()
                        myViewModel.getGroupMembers(context, result) { members ->
                          friendsList.value = members
                          selectedItemIndex.value = myViewModel.groupList.indexOfFirst { it.id == result }
                        }
                      } else {
                        Toast.makeText(context, "Error: Something went wrong", Toast.LENGTH_SHORT)
                          .show()
                      }
                    }
                    onDismissRequest()
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                  )
                ) {
                  Icon(Icons.Default.Check, contentDescription = "Accept Request", tint = Colors.primary)
                }
                Button (
                  onClick = {
                    myViewModel.declineGroupRequest(context, group.id!!) {result ->
                      if (result) {
                        Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Error: Something went wrong", Toast.LENGTH_SHORT).show()
                      }
                    }
                    onDismissRequest()
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                  )
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Decline Request", tint = Color.Red)
                }
              }
            }
          }
        }
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
                  myViewModel.acceptRequest(context, request.uid, myViewModel.uid) { success ->
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
                  myViewModel.declineRequest(context, request.uid, myViewModel.uid) { success ->
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
      .setContentTitle("New Request")
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