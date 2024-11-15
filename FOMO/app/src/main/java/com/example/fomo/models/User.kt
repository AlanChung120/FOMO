package com.example.fomo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
  @SerialName("uid") val uid: String,
  @SerialName("created_at") val createdAt: String,              // Timestamp for when the user was created
  @SerialName("email") val email: String,                       // User's email address
  @SerialName("display_name") val displayName: String,          // User's display name
  @SerialName("username") val username: String,                 // Username
  @SerialName("password") val password: String,                 // Password (consider encrypting if used in production)
  @SerialName("latitude") val latitude: Double,                 // User's latitude location
  @SerialName("longitude") val longitude: Double,               // User's longitude location
  @SerialName("status") val status_id: Long,                        // Status ID
)
