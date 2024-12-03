package com.example.fomo.entities

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals

class UserTest {

  @Test
  fun `User object serializes correctly to JSON`() {
    val user = User(
      uid = "123",
      createdAt = "2024-12-01T10:00:00Z",
      email = "test@example.com",
      displayName = "TestUser",
      username = "testuser",
      password = "securepassword",
      latitude = 43.4723,
      longitude = -80.5449,
      status_id = 1,
      route = JsonArray(listOf(JsonPrimitive("PointA"), JsonPrimitive("PointB"))),
      destination_latitude = 43.5,
      destination_longitude = -80.5
    )

    val json = Json.encodeToString(User.serializer(), user)

    val expectedJson = """
            {"uid":"123","created_at":"2024-12-01T10:00:00Z","email":"test@example.com","display_name":"Test User","username":"testuser","password":"securepassword","latitude":43.4723,"longitude":-80.5449,"status":1,"route":["Point A","Point B"],
                "destination_latitude":43.5,
                "destination_longitude":-80.5
            }
        """.trimIndent().replace("\n", "").replace(" ", "")

    assertEquals(expectedJson, json)
  }

  @Test
  fun `JSON deserializes correctly to User object`() {
    val json = """
            {
                "uid": "123",
                "created_at": "2024-12-01T10:00:00Z",
                "email": "test@example.com",
                "display_name": "Test User",
                "username": "testuser",
                "password": "securepassword",
                "latitude": 43.4723,
                "longitude": -80.5449,
                "status": 1,
                "route": ["Point A", "Point B"],
                "destination_latitude": 43.5,
                "destination_longitude": -80.5
            }
        """.trimIndent()

    val user = Json.decodeFromString(User.serializer(), json)

    assertEquals("123", user.uid)
    assertEquals("2024-12-01T10:00:00Z", user.createdAt)
    assertEquals("test@example.com", user.email)
    assertEquals("Test User", user.displayName)
    assertEquals("testuser", user.username)
    assertEquals("securepassword", user.password)
    assertEquals(43.4723, user.latitude)
    assertEquals(-80.5449, user.longitude)
    assertEquals(1, user.status_id)
    assertEquals(JsonArray(listOf(JsonPrimitive("Point A"), JsonPrimitive("Point B"))), user.route)
    assertEquals(43.5, user.destination_latitude)
    assertEquals(-80.5, user.destination_longitude)
  }

  @Test
  fun `User object assigns default values to nullable fields`() {
    val user = User(
      uid = "123",
      createdAt = "2024-12-01T10:00:00Z",
      email = "test@example.com",
      displayName = "Test User",
      username = "testuser",
      password = "securepassword",
      latitude = 43.4723,
      longitude = -80.544,
      status_id = 1
    )

    assertEquals(null, user.route)
    assertEquals(null, user.destination_latitude)
    assertEquals(null, user.destination_longitude)
  }

  @Test
  fun `User object is preserved during round-trip serialization and deserialization`() {
    val user = User(
      uid = "123",
      createdAt = "2024-12-01T10:00:00Z",
      email = "test@example.com",
      displayName = "Test User",
      username = "testuser",
      password = "securepassword",
      latitude = 43.4723,
      longitude = -80.5449,
      status_id = 1,
      route = JsonArray(listOf(JsonPrimitive("Point A"), JsonPrimitive("Point B"))),
      destination_latitude = 43.5,
      destination_longitude = -80.5
    )

    val json = Json.encodeToString(User.serializer(), user)
    val deserializedUser = Json.decodeFromString(User.serializer(), json)

    assertEquals(user, deserializedUser)
  }
}
