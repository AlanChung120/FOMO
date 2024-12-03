package com.example.fomo.views

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import cafe.adriel.voyager.core.screen.Screen
import coil.compose.rememberAsyncImagePainter
import com.example.fomo.LoadingScreen
import com.example.fomo.R
import com.example.fomo.viewmodel.MyViewModel


class ProfileScreen(private val myViewModel: MyViewModel) : Screen {
  @Composable
  override fun Content() {
    val isDataLoaded by myViewModel.isDataLoaded.collectAsState()
    val isSessionRestored by myViewModel.sessionRestored.collectAsState()

    if (isDataLoaded && isSessionRestored) {
      ProfileContent(myViewModel)
    } else {
      LoadingScreen()
    }
  }
}
@Composable
fun ProfileContent(myViewModel: MyViewModel) {
    var showDialog by remember { mutableStateOf(false) }  // popup state
    var toChange by remember { mutableStateOf("null") } // what to change
    var droppedDown by remember { mutableStateOf(false) } // display dropdown
    var newValue by remember { mutableStateOf("") } // updated name state
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val imagePickerLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
      myViewModel.updateProfilePicture(contentResolver, uri) { success ->
        if (success) {
          Toast.makeText(context, "Profile Picture Updated!", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(context, "Error: Invalid Picture", Toast.LENGTH_SHORT).show()
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(text = "Profile",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ){
        Button(
          contentPadding = PaddingValues(0.dp),
          modifier = Modifier
          .size(125.dp)
          .clip(CircleShape),
          onClick = {
            imagePickerLauncher.launch("image/*")
          }
        ) {
          Image(
            painter = rememberAsyncImagePainter(myViewModel.imageUri,
              error = painterResource(id = R.drawable.default_pfp) ),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }

      }

      // Row to display the current name and the "Change" button on the same line
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Display Name: ${myViewModel.displayName}",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f) // Push the text to the left
        )

        TextButton(
          onClick = {
            showDialog = true
            toChange = "Display Name"
            newValue = myViewModel.displayName
          }
        ) {
          Text("Change", color = MaterialTheme.colorScheme.primary)
        }
      }

      // Row to display the current name and the "Change" button on the same line
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Username: ${myViewModel.username}",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f) // Push the text to the left
        )

        TextButton(
          onClick = {
            showDialog = true
            toChange = "Username"
            newValue = myViewModel.username
          }
        ) {
          Text("Change", color = MaterialTheme.colorScheme.primary)
        }
      }

      // Row to display the password and the "Change" button on the same line
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Password: ******",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f) // Push the text to the left
        )

        TextButton(
          onClick = {
            showDialog = true
            toChange = "Password"
            newValue = ""
          }
        ) {
          Text("Change", color = MaterialTheme.colorScheme.primary)
        }
      }

      // Row to display status and the "Change" button on the same line
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {

        Text(
          text = "Current Activity: ",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f) // Push the text to the left
        )

        Box {
          Button(
            shape = RoundedCornerShape(24.dp),
            onClick = { droppedDown = !droppedDown },
            modifier = Modifier
              .padding(6.dp)
          ) {
            Text(
              text = "${myViewModel.status.emoji} ${myViewModel.status.description}",
              fontSize = 14.sp,
              fontWeight = FontWeight.Normal,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
          }

          // Dropdown menu items
          DropdownMenu(
            expanded = droppedDown,
            onDismissRequest = { droppedDown = false },
            modifier = Modifier
              .width(200.dp)
              .heightIn(max = 400.dp) // Set max height of the dropdown
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
                    droppedDown = false
                  })
              }
            }
          }
        }

      }

      if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("Enter New $toChange")

              Spacer(modifier = Modifier.height(8.dp))

              // TextField to input new display name
              OutlinedTextField(
                value = newValue,
                onValueChange = { newValue = it },
                label = { Text("New $toChange") },
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
              ) {
                TextButton(onClick = { showDialog = false }) {
                  Text("Cancel")
                }
                TextButton(onClick = {  //save button
                  if ( toChange == "Display Name") {
                    myViewModel.updateDisplayName(newValue) { success ->
                      if (success) {
                        Toast.makeText(context, "Display Name Updated", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Error: Input too long (above 20 characters)",
                          Toast.LENGTH_SHORT).show()
                      }
                    }
                  }  else if ( toChange == "Username") {
                    myViewModel.updateUsername(newValue) { success ->
                      if (success) {
                        Toast.makeText(context, "Username Updated!", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Error: Input too long (above 20 characters) or username already exists",
                          Toast.LENGTH_SHORT).show()
                      }
                    }
                  } else {
                    myViewModel.updatePassword(newValue) { success ->
                      if (success) {
                        Toast.makeText(context, "Password Updated!", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Error: Input too long (above 20 characters) or too short (below 8 characters)",
                          Toast.LENGTH_SHORT).show()
                      }
                    }
                  }
                  showDialog = false
                  newValue = ""
                }) {
                  Text("Save")
                }
              }
            }
          }
        }
      }
    }
}
