package com.example.fomo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Place(
  @SerialName("id") val id: Long? = null,
  @SerialName("name") val name: String,
  @SerialName("latitude") val latitude: Double,
  @SerialName("longitude") val longitude: Double,
  @SerialName("radius") val radius: Double,
  @SerialName("owner_id") val owner_id: String,
)
