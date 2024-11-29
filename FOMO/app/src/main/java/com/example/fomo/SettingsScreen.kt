package com.example.fomo

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.tooling.data.EmptyGroup.data
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.provider.Settings
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.fomo.const.Colors
import com.example.fomo.models.MyViewModel
import cafe.adriel.voyager.navigator.LocalNavigator


class SettingsScreen(private val myViewModel: MyViewModel) : Screen {
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var isLocationSharingEnabled by remember { mutableStateOf(
      LocationHelper.areLocationPermissionsGranted(context))
    }
    var isNotificationSharingEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val foreground by remember { mutableStateOf(LocationHelper.areLocationPermissionsGranted(context)) }
    val background by remember { mutableStateOf(LocationHelper.isBackgroundLocationPermissionGranted(context)) }


    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.Top
    ) {
      Text(text = "Settings",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp))


      Spacer(modifier = Modifier.height(16.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(text = "Location Sharing"          ,
          modifier = Modifier.weight(1f) // Push the text to the left
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Location sharing toggle switch
        Switch(
          checked = isLocationSharingEnabled,
          onCheckedChange = { isEnabled ->
            isLocationSharingEnabled = isEnabled
            openSetting(context)
          }
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(text = "Notification",
          modifier = Modifier.weight(1f) // Push the text to the left
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Location sharing toggle switch
        Switch(
          checked = isNotificationSharingEnabled,
          onCheckedChange = { isEnabled ->
            isNotificationSharingEnabled = isEnabled

          }
        )
      }


      Button(
        onClick = {
          // log out function here!
          myViewModel.logout(context)
          navigator!!.push(SignIn(myViewModel))
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = Colors.primary,
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .padding(top = 32.dp)
          .fillMaxWidth()
      ) {
        Text(
          "Log Out",
          fontSize = 16.sp,
          modifier = Modifier
            .padding(vertical = 8.dp)
            .align(Alignment.Bottom)
        )
      }
    }



  }

  fun openSetting(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
  }
}
