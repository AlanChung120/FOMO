package com.example.fomo.entities

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals

class EntitiesTest {

  @Test
  fun `Friendship serializes and deserializes correctly`() {
    val friendship = Friendship(
      id = 1,
      createdAt = "2024-12-01T10:00:00Z",
      requesterId = "user1",
      receiverId = "user2",
      acceptDate = "2024-12-02T10:00:00Z",
      accepted = true
    )

    val json = Json.encodeToString(Friendship.serializer(), friendship)
    val deserialized = Json.decodeFromString(Friendship.serializer(), json)

    assertEquals(friendship, deserialized)
  }

  @Test
  fun `Game serializes and deserializes correctly`() {
    val game = Game(
      id = 1,
      startTime = "2024-12-01T12:00:00Z",
      endTime = "2024-12-01T14:00:00Z",
      groupId = 123,
      hunterId = "hunter1",
      running = true
    )

    val json = Json.encodeToString(Game.serializer(), game)
    val deserialized = Json.decodeFromString(Game.serializer(), json)

    assertEquals(game, deserialized)
  }

  @Test
  fun `GameLink serializes and deserializes correctly`() {
    val gameLink = GameLink(
      id = 1,
      createdAt = "2024-12-01T10:00:00Z",
      uid = "user1",
      groupId = 123,
      gameId = 456,
      hunterId = "hunter1",
      accepted = true
    )

    val json = Json.encodeToString(GameLink.serializer(), gameLink)
    val deserialized = Json.decodeFromString(GameLink.serializer(), json)

    assertEquals(gameLink, deserialized)
  }

  @Test
  fun `Group serializes and deserializes correctly`() {
    val group = Group(
      id = 1,
      createdAt = "2024-12-01T10:00:00Z",
      name = "Test Group",
      creatorId = "creator1"
    )

    val json = Json.encodeToString(Group.serializer(), group)
    val deserialized = Json.decodeFromString(Group.serializer(), json)

    assertEquals(group, deserialized)
  }

  @Test
  fun `GroupLink serializes and deserializes correctly`() {
    val groupLink = GroupLink(
      id = 1,
      createdAt = "2024-12-01T10:00:00Z",
      userId = "user1",
      senderUid = "sender1",
      groupId = 123,
      accepted = true
    )

    val json = Json.encodeToString(GroupLink.serializer(), groupLink)
    val deserialized = Json.decodeFromString(GroupLink.serializer(), json)

    assertEquals(groupLink, deserialized)
  }

  @Test
  fun `GeocodeResponse serializes and deserializes correctly`() {
    val response = GeocodeResponse(
      results = listOf(Result("place1", "123 Main St")),
      status = "OK"
    )

    val json = Json.encodeToString(GeocodeResponse.serializer(), response)
    val deserialized = Json.decodeFromString(GeocodeResponse.serializer(), json)

    assertEquals(response, deserialized)
  }

  @Test
  fun `DirectionsResponse serializes and deserializes correctly`() {
    val response = DirectionsResponse(
      routes = listOf(
        Route(
          legs = listOf(Leg(Distance("5 km", 5000), Duration("10 min", 600))),
          overview_polyline = Polyline("encodedPolyline")
        )
      )
    )

    val json = Json.encodeToString(DirectionsResponse.serializer(), response)
    val deserialized = Json.decodeFromString(DirectionsResponse.serializer(), json)

    assertEquals(response, deserialized)
  }

  @Test
  fun `Place serializes and deserializes correctly`() {
    val place = Place(
      id = 1,
      name = "Central Park",
      latitude = 40.785091,
      longitude = -73.968285,
      radius = 0.5,
      groupId = 123
    )

    val json = Json.encodeToString(Place.serializer(), place)
    val deserialized = Json.decodeFromString(Place.serializer(), json)

    assertEquals(place, deserialized)
  }

  @Test
  fun `Status serializes and deserializes correctly`() {
    val status = Status(
      id = 1,
      createdAt = "2024-12-01T10:00:00Z",
      description = "Available",
      emoji = "😊"
    )

    val json = Json.encodeToString(Status.serializer(), status)
    val deserialized = Json.decodeFromString(Status.serializer(), json)

    assertEquals(status, deserialized)
  }
}
