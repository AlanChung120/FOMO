package com.example.fomo.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Place(
  @SerialName("id") val id: Long? = null,
  @SerialName("name") val name: String,
  @SerialName("latitude") val latitude: Double,
  @SerialName("longitude") val longitude: Double,
  @SerialName("radius") val radius: Double,
  @SerialName("group_id") val groupId: Long, // group that place represents
)
