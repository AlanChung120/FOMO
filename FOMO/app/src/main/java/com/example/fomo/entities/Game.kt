package com.example.fomo.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Game(
  @SerialName("id") val id: Long? = null,                                 // Primary key
  @SerialName("start_time") val startTime: String? = null,
  @SerialName("end_time") val endTime: String? = null,
  @SerialName("group_id") val groupId: Long,
  @SerialName("hunter_id") val hunterId: String? = null,
  @SerialName("running") val running: Boolean? = null,
)