package com.example.fomo.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Game(
  @SerialName("id") val id: Long? = null,                                 // Primary key
  @SerialName("start_time") val startTime: String? = null, // Timestamp for creation date
  @SerialName("end_time") val endTime: String? = null, // Timestamp for creation date
  @SerialName("group_id") val groupId: Long,
  @SerialName("hunter_id") val hunterId: String? = null,  // ID of the user who created the group
  @SerialName("running") val running: Boolean? = null,  // ID of the user who created the group
)