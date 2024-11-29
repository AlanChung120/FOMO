package com.example.fomo

import android.graphics.Paint.Align
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import com.example.fomo.models.MyViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fomo.const.Colors
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator

class SignUp(private val myViewModel: MyViewModel) : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    var username by remember{ mutableStateOf<String>("") }
    var password by remember{ mutableStateOf<String>("") }
    val navigator = LocalNavigator.current
    var showError by remember {mutableStateOf(false)}
    val context = LocalContext.current

    Box(
      contentAlignment = Alignment.BottomCenter,
      modifier = Modifier
        .fillMaxSize()
    ) {
      Image(
        painter = painterResource(id = R.drawable.sign_in_out_background),
        contentDescription = null,
        modifier = Modifier
          .fillMaxSize(),
        contentScale = ContentScale.Crop
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 32.dp)
      ) {
        // Username
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          TextField(
            value = username,
            onValueChange = { username = it },
            placeholder = { Text("Enter your email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Text,
              imeAction = ImeAction.Done
            ),
            colors = TextFieldDefaults.colors(
              unfocusedContainerColor = Color.Transparent,
              focusedContainerColor = Color.Transparent,
              focusedIndicatorColor = Color.Black,
            ),
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Mail,
                contentDescription = "Mail Icon"
              )
            },
            modifier = Modifier
              .weight(1f)
              .padding(end = 2.dp)
          )
        }

        // Password
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Choose a password") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Text,
              imeAction = ImeAction.Done
            ),
            colors = TextFieldDefaults.colors(
              unfocusedContainerColor = Color.Transparent,
              focusedContainerColor = Color.Transparent,
              focusedIndicatorColor = Color.Black,
            ),
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Lock Icon"
              )
            },
            modifier = Modifier
              .weight(1f)
              .padding(end = 2.dp)
          )
        }

        Text(
          text = if (showError) "Account already exists!" else "",
          color = if (showError) Color.Red else Color.Transparent, // Red when error, transparent otherwise
          fontSize = 12.sp,
          modifier = Modifier
            .align(Alignment.Start)
        )

        Button(
          onClick = {
            // Sign Up function here!
            myViewModel.signUp(context, username, password) {success ->
              showError = !success
              if (success) {
                navigator!!.push(SignIn(myViewModel))
              } else {
                Log.e("AuthFlow", "Sign up failed")
              }
            }

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
            "Create Account",
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        }

        HorizontalDivider(
          color = Color.Gray,
          thickness = 1.dp,
          modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedButton(
          onClick = {
            navigator!!.push(SignIn(myViewModel))
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            contentColor = Color.Gray,
            containerColor = Color.Transparent,

            ),
          modifier = Modifier
            .padding(bottom = 48.dp)
            .fillMaxWidth()
        ) {
          Text(
            "Sign In",
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        }
      }
    }
  }
}