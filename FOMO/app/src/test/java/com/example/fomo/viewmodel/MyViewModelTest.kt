package com.example.fomo.viewmodel

import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Test

import org.junit.Assert.*


val mockSupabaseClient = mockk<SupabaseClient>(relaxed = true)
val viewModel = MyViewModel(mockSupabaseClient)

class MyViewModelTest {
  @Test
  fun testBasicFunctionality() {
    val viewModel = viewModel
    assertNotNull(viewModel)
  }


  @Test
  fun togglegame(): Unit = runTest {
    viewModel.toggleGameModal(true)
    assertTrue(viewModel.isGameModalVisible.value)

    viewModel.toggleGameModal(false)
    assertFalse(viewModel.isGameModalVisible.value)
  }

  @Test
  fun `selectGroup should update groupIndex`() = runTest {
    viewModel.selectGroup(2)
    assertEquals(2, viewModel.groupIndex.value)
  }

  @Test
  fun `calculateDistance should compute correct distance`() {
    val point1 = LatLng(43.4723, -80.5449)
    val point2 = LatLng(43.4720, -80.5400)
    val distance = viewModel.calculateDistance(point1, point2)

    assertTrue(distance > 0) // Ensure distance is calculated
  }
  @Test
  fun `toggleStartGame updates startGame state`() = runTest {
    viewModel.toggleStartGame(true)
    assertTrue(viewModel.startGame.value)

    viewModel.toggleStartGame(false)
    assertFalse(viewModel.startGame.value)
  }


  @Test
  fun `setGameDuration updates game duration`() {
    viewModel.setGameDuration(10)
    assertEquals(10, viewModel.gameDuration.value)
  }


  @Test
  fun `calculateDistance returns zero for same coordinates`() {
    val point = LatLng(43.4723, -80.5449)
    val distance = viewModel.calculateDistance(point, point)
    assertEquals(0.0, distance, 0.001)
  }

  @Test
  fun `LatLngListToJSON converts list to JSON correctly`() {
    val latLngList = listOf(LatLng(43.4723, -80.5449), LatLng(43.4730, -80.5450))
    val jsonArray = viewModel.LatLngListToJSON(latLngList)

    assertEquals(2, jsonArray.size)
    assertEquals(43.4723, jsonArray[0].jsonObject["latitude"]!!.jsonPrimitive.double, 0.001)
    assertEquals(-80.5449, jsonArray[0].jsonObject["longitude"]!!.jsonPrimitive.double, 0.001)
  }

  @Test
  fun `JSONToLatLngList converts JSON to list correctly`() {
    val jsonArray = buildJsonArray {
      add(buildJsonObject {
        put("latitude", 43.4723)
        put("longitude", -80.5449)
      })
      add(buildJsonObject {
        put("latitude", 43.4730)
        put("longitude", -80.5450)
      })
    }

    val latLngList = viewModel.JSONToLatLngList(jsonArray)

    assertEquals(2, latLngList.size)
    assertEquals(LatLng(43.4723, -80.5449), latLngList[0])
    assertEquals(LatLng(43.4730, -80.5450), latLngList[1])
  }

  @Test
  fun `setSignedInState updates signedIn state`() {
    viewModel.setSignedInState(true)
    assertTrue(viewModel.signedIn.value)

    viewModel.setSignedInState(false)
    assertFalse(viewModel.signedIn.value)
  }

}