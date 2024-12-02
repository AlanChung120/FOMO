package com.example.fomo.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameLink(
  @SerialName("id") val id: Long? = null,
  @SerialName("created_at") val createdAt: String,
  @SerialName("uid") val uid: String,
  @SerialName("group_id") val groupId: Long,
  @SerialName("game_id") val gameId: Long,  // ID of the user who created the group
  @SerialName("hunter_id") val hunterId: String? = null,  // ID of the user who created the group
  @SerialName("accepted") val accepted: Boolean? = null,  // ID of the user who created the group
)