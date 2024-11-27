package com.example.fomo.models

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.core.app.ActivityCompat.recreate
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.example.fomo.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.google.maps.android.PolyUtil
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import java.io.InputStream
import android.content.ContentResolver
import android.net.Uri

class MyViewModel : ViewModel() {

    // constants
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    private val defaultStatus = Status(id=7, dateFormat.format(Date()), "Idle", "\uD83D\uDCA4")

    // clients
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://vwapghztewutqqmzaoib.supabase.co",
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
    }

    private val ktorClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // start of signed in stateflow
    private val _signedInFlow = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedInFlow

    private fun setSignedInState(isSignedIn: Boolean) {
        _signedInFlow.value = isSignedIn
    }

    // end of signed in stateflow


    // user
    var displayName by mutableStateOf("")
    var username by mutableStateOf("")
    var imageUri by mutableStateOf<Uri?>(null)
    var email by mutableStateOf("")
    var uid by mutableStateOf("")
    private var password by mutableStateOf("")
    var userLongitude by mutableDoubleStateOf(0.0)
    var userLatitude by mutableDoubleStateOf(0.0)
    var center by mutableStateOf(LatLng(43.4723, -80.5449))
    var status by mutableStateOf<Status>(defaultStatus)
    var friendsList by mutableStateOf<List<User>>(emptyList())
    var requestList by mutableStateOf<List<User>>(emptyList())
    var groupMemberList by mutableStateOf<List<User>>(emptyList())
    var statusList by mutableStateOf<List<Status>>(emptyList())
    var groupList by mutableStateOf<List<Group>>(emptyList())
    var groupRequestList by mutableStateOf<List<Group>>(emptyList())
    var selectedLocation by mutableStateOf<LatLng?>(null)
    var route by mutableStateOf<List<LatLng>?>(null)
    var mode by mutableStateOf<String>("walking")
    var places by mutableStateOf<List<Place>>(emptyList())

    val routeMutex = Mutex()

    // Start of Map Functions

    fun onMyWay(coords: LatLng) {
        if (status.description != "On my way") {
            return
        }
        selectedLocation = null
        route = null
        viewModelScope.launch {
            selectedLocation = coords
            route = getRoute(center, selectedLocation!!, mode)
            if (route != null && selectedLocation != null) {
                setRoute(route!!, selectedLocation!!)
            }
            updateStatus(statusList.filter{status -> status.description == "On my way"}[0])
        }
    }

    suspend fun getPlaceId(coords: LatLng): String? {
        try {
            val response: GeocodeResponse = ktorClient.get("https://maps.googleapis.com/maps/api/geocode/json") {
                parameter("latlng", "${coords.latitude},${coords.longitude}")
                parameter("location_type", "ROOFTOP")
                parameter("result_type", "street_address")
                parameter("key", BuildConfig.GOOGLE_MAPS_API_KEY)
            }.body()
            if (response.results.isEmpty()) {
                throw IllegalArgumentException("Place_id API returned no results")
            }
            val place_id = response.results[0].place_id
            return place_id
        } catch(e: Exception) {
            "Request failed: ${e.message}"
            return null
        }
    }

    // mode should be "walking", "driving", "bicycling", "transit"
    suspend fun getRoute(origin: LatLng, destination: LatLng, mode: String): List<LatLng>? {
        try {
            val response: DirectionsResponse = ktorClient.get("https://maps.googleapis.com/maps/api/directions/json") {
                parameter("origin", "${origin.latitude},${origin.longitude}")
                parameter("destination", "${destination.latitude},${destination.longitude}")
                parameter("mode", mode)
                parameter("key", BuildConfig.GOOGLE_MAPS_API_KEY)
            }.body()
            if (response.routes.isEmpty()) {
                throw IllegalArgumentException("Directions API returned no routes")
            }
            // finds the route with shortest distance
            val encodedRoute = response.routes.minByOrNull{ it.legs[0].distance.value }!!.overview_polyline.points
            val route = PolyUtil.decode(encodedRoute)
            return route
        } catch(e: Exception) {
            "Request failed: ${e.message}"
            return null
        }
    }

    fun LatLngListToJSON(route: List<LatLng>): JsonArray {
        val jsonRoute = buildJsonArray {
            for (point in route) {
                add(buildJsonObject {
                    put("latitude", JsonPrimitive(point.latitude))
                    put("longitude", JsonPrimitive(point.longitude))
                })
            }
        }
        return jsonRoute
    }

    fun JSONToLatLngList(route: JsonArray): List<LatLng> {
        val listRoute = mutableListOf<LatLng>()
        for (point in route) {
            listRoute.add(LatLng(
                (point.jsonObject)["latitude"]!!.jsonPrimitive.double,  // Access latitude as Double
                (point.jsonObject)["longitude"]!!.jsonPrimitive.double  // Access longitude as Double
            ))
        }
        return listRoute
    }

    fun fetchRoute() {
        viewModelScope.launch {
            routeMutex.withLock {
                try {
                    val response = supabase.from("users").select(
                        Columns.list("route, destination_latitude, destination_longitude")
                    ) {
                        filter {
                            eq("uid", uid)
                        }
                    }.decodeSingle<User>()

                    if (response.route != null && response.destination_latitude != null && response.destination_longitude != null) {
                        selectedLocation = LatLng(response.destination_latitude, response.destination_longitude)
                        route = JSONToLatLngList(response.route)
                    }
                } catch (e: Exception) {
                    Log.d("Supabase fetchRoute()", "Error: ${e.message}")
                }
            }
        }

    }


    fun setRoute(route: List<LatLng>, destination: LatLng) {
        println("setroute route ${route}")
        println("setroute destination ${destination}")
        viewModelScope.launch {
            routeMutex.withLock {
                try {
                    val jsonRoute = LatLngListToJSON(route)
                    supabase.from("users").update({
                        set("route", jsonRoute)
                        set("destination_latitude", destination.latitude)
                        set("destination_longitude", destination.longitude)
                    }) {
                        filter {
                            eq("uid", uid)
                        }
                    }
                } catch(e: Exception) {
                    Log.e("Supabase setRoute()", "Error: ${e.message}")
                }
            }

        }
    }

    private fun removeRoute() {
        viewModelScope.launch {
            routeMutex.withLock {
                try {
                    supabase.from("users").update({
                        set("route", null as String?)
                        set("destination_latitude", null as Double?)
                        set("destination_longitude", null as Double?)
                    }) {
                        filter {
                            eq("uid", uid)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Supabase removeRoute()", "Error: ${e.message}")
                }
            }
        }
    }

    // End of Map Functions

    // Start of Auth Functions

    fun signUp(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Call Supabase's signUpWith function
                val user = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }


                if (user != null) {
                    // this line should not run for now
                    Log.d("SupabaseAuth", "User created with email confirmation: $email")
                    onResult(true)
                } else {
                    val userObject = supabase.auth.retrieveUserForCurrentSession(updateSession = true)
                    val users = supabase.from("users")

                    users.insert(User(
                        uid = userObject.id,
                        displayName = email,
                        createdAt = dateFormat.format(Date()),
                        email = email,
                        username = email,
                        password = password,
                        latitude = 0.0,
                        longitude = 0.0,
                        status_id = 1,
                    ))

                    Log.d("SupabaseAuth", "User signed up created and added to DB: $email")
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Sign-up failed: ${e.message}")
                onResult(false)
            }
        }
    }
    fun signIn(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try{
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val session = supabase.auth.currentSessionOrNull()
                val user = supabase.auth.retrieveUserForCurrentSession(updateSession = true)

                // initialize sign-in state and establish userid
                if (session != null) {
                    Log.d("SupabaseAuth", "Session active, userid ${user.id}")
                    uid = user.id
                    onResult(true)
                    setSignedInState(true)
                } else {
                    Log.d("SupabaseAuth", "Sign-in credentials invalid")
                    onResult(false)
                    setSignedInState(false)
                }

            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Sign-in failed: ${e.message}")
                onResult(false)
            }
        }

    }

    private fun reset() {
        displayName = ""
        username = ""
        imageUri = null
        email = ""
        uid = ""
        password = ""
        userLongitude = 0.0
        userLatitude = 0.0
        center = LatLng(43.4723, -80.5449)
        status = defaultStatus
        friendsList = emptyList()
        requestList = emptyList()
        groupMemberList = emptyList()
        statusList = emptyList()
        groupList = emptyList()
        groupRequestList = emptyList()
        selectedLocation = null
        route = null
        mode = "walking"
        places = emptyList()

    }

    fun logout() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                Log.d("SupabaseAuth", "User signed out")
                setSignedInState(false)
                uid = ""
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "failed to sign out: ${e.message}")
            }
        }
    }



    // End of Auth Functions


    // Start of Database Functions

    fun getImgUrl(userID: String): String {
        val supabaseUrl = "https://vwapghztewutqqmzaoib.supabase.co"
        return "$supabaseUrl/storage/v1/object/public/profile-pictures/$userID.jpg"
    }

    fun fetchPlaces() {
        viewModelScope.launch {
            try {
                places = supabase.from("places").select() {
                    filter {
                        eq("owner_id", uid)
                    }
                }.decodeList<Place>()
                Log.d("Supabase fetchPlaces()", "Places fetched")
            } catch (e: Exception) {
                Log.e("Supabase fetchPlaces()", "Error: ${e.message}")
            }
        }
    }

    fun setPlace(name:String, coords: LatLng?, radius: Double?) {

        viewModelScope.launch {
            try {
                val newPlace = Place(name = name, latitude = coords?.latitude ?: userLatitude,
                    longitude = coords?.longitude ?: userLongitude,  radius = radius ?: 0.001, owner_id = uid)
                supabase.from("places").insert(newPlace)
            } catch (e: Exception) {
                Log.e("Supabase setPlace()", "Error: ${e.message}")
            }
        }
    }

    // Checks if the current user location is in the given place
    fun isInPlace(place: Place): Boolean {
        return ((place.latitude - place.radius < userLatitude &&
                userLatitude < place.latitude + place.radius) &&
                (place.longitude - place.radius < userLongitude &&
                    userLongitude < place.longitude + place.radius))

    }

    fun removePlace(id: Long) {
        viewModelScope.launch {
            try {
                supabase.from("places").delete() {
                    filter {
                        eq("id", id)
                    }
                }
            } catch (e: Exception) {
                Log.e("Supabase removePlace()", "Error: ${e.message}")
            }
        }
    }

    fun fetchFriends() {
        viewModelScope.launch {
            try {
                val userRes = supabase.from("users")
                val friendshipRes = supabase.from("friendship")
                    .select()
                    .decodeList<Friendship>()
                val tempFriends = mutableListOf<User>()
                val tempRequesters = mutableListOf<User>()
                for (friendship in friendshipRes) {
                    if (friendship.accepted && friendship.receiverId == uid) {
                        val friendId = friendship.requesterId
                        val tempFriend = userRes
                            .select() {
                                filter {
                                    eq("uid", friendId)
                                }
                            }.decodeSingle<User>()
                        tempFriends.add(tempFriend)
                    } else if (friendship.accepted && friendship.requesterId == uid) {
                        // Add receiverId if requesterId is the current user
                        val friendId = friendship.receiverId
                        val tempFriend = userRes
                            .select() {
                                filter {
                                    eq("uid", friendId)
                                }
                            }.decodeSingle<User>()
                        tempFriends.add(tempFriend)
                    } else if (!friendship.accepted && friendship.receiverId == uid) {
                        // Add requester if receiverId is 2
                        val friendId = friendship.requesterId
                        val tempFriend = userRes
                            .select() {
                                filter {
                                    eq("uid", friendId)
                                }
                            }.decodeSingle<User>()
                        tempRequesters.add(tempFriend)
                    }
                }
                friendsList = tempFriends
                requestList = tempRequesters


            } catch (e: Exception) {
                Log.e("SupabaseConnection", "Failed to connect to database: ${e.message}")
            }
        }
    }

    fun fetchGroups() {
        viewModelScope.launch {
            try {
                val linkRes = supabase.from("group_links")
                    .select() {
                        filter {
                            eq("user_uid", uid)
                        }
                    }.decodeList<GroupLink>()
                val tempGroups = mutableListOf<Group>()
                val tempRequesters = mutableListOf<Group>()
                for (groupLink in linkRes) {
                    val groupId = groupLink.groupId
                    val tempGroup = supabase.from("groups")
                        .select() {
                            filter {
                                eq("id", groupId)
                            }
                        }.decodeSingle<Group>()
                    if (groupLink.accepted) {
                        tempGroups.add(tempGroup)
                    } else {
                        tempRequesters.add(tempGroup)
                    }
                }
                groupList = tempGroups
                groupRequestList = tempRequesters

                Log.d("groupFetching", "Success!")
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "Failed to connect to database: ${e.message}")
            }
        }
    }

    fun fetchDatabase() {
        viewModelScope.launch {
            try {
                val statusRes = supabase.from("statuses").select().decodeList<Status>()
                val picBucket = supabase.storage.from("profile-pictures")

                val me = supabase.from("users").select() {
                    filter {
                        eq("uid", uid)
                    }
                }.decodeSingle<User>()

                statusList = statusRes
                displayName = me.displayName
                username = me.username
                email = me.email
                password = me.password
                status = statusRes.filter {it.id == me.status_id}[0]
                imageUri = Uri.parse(getImgUrl(uid))

                fetchRoute()
                fetchFriends()
                fetchPlaces()
                fetchGroups()

                Log.d("SupabaseConnection", "Friends fetched: $friendsList")
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "Failed to connect to database: ${e.message}")
            }
        }
    }

    fun updateDisplayName(newDisplayName: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            try {
               supabase.from("users").update({
                   set("display_name", newDisplayName)
               }){
                   filter {
                       eq("uid", uid)
                   }
               }

                displayName = newDisplayName
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun updateProfilePicture(contentResolver: ContentResolver, newUri: Uri?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (newUri != null) {
                    val picBucket = supabase.storage.from("profile-pictures")
                    val inputStream: InputStream? = contentResolver.openInputStream(newUri)
                    val response = picBucket.upload("$uid.jpg", inputStream?.readBytes()!!) {
                        upsert = true
                    }
                    imageUri = newUri
                    Log.d("uploadImage", "Uploaded as ${response.path}")
                    onResult(true)
                } else {
                    Log.e("uploadImage", "Image Upload Error")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("uploadImage", "Image Upload Error: ${e.message}")
                onResult(false)
            }
        }
    }


    fun updateEmail(newEmail: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            try {
                supabase.from("users").update({
                    set("email", newEmail)
                }){
                    filter {
                        eq("uid", uid)
                    }
                }

                email = newEmail
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun updateUsername(newUsername: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            try {
                supabase.from("users").update({
                    set("username", newUsername)
                }){
                    filter {
                        eq("uid", uid)
                    }
                }

                username = newUsername
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun updatePassword(newPassword: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            try {
                supabase.from("users").update({
                    set("password", newPassword)
                }){
                    filter {
                        eq("uid", uid)
                    }
                }

                password = newPassword
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        return SphericalUtil.computeDistanceBetween(point1, point2)
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                supabase.from("users").update({
                    set("latitude", latitude)
                    set("longitude", longitude)
                }){
                    filter {
                        eq("uid", uid)
                    }
                }
                userLongitude = longitude
                userLatitude = latitude
                center = LatLng(latitude, longitude)

                Log.d("Map View Model Location Update", "Latitude: $latitude, Longitude: $longitude")
                Log.d("Supabase Location Update", "Latitude: $latitude, Longitude: $longitude")
            } catch (e: Exception) {
                Log.e("Supabase", "Location Update Error: ${e.message}")
            }
        }
    }

    fun updateStatus(newStatus: Status) {
        viewModelScope.launch {
            try {
                supabase.from("users").update({
                    set("status", newStatus.id)
                }){
                    filter {
                        eq("uid", uid)
                    }
                }

                status = newStatus
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
            }
        }
    }

    fun createRequest(username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val receiver = supabase.from("users").select() { // Find the receiver
                    filter {
                        eq("username", username)
                    }
                }.decodeSingle<User>()
                val oppositeCheck = supabase.from("friendship").select() { // check if opposite request exists
                    filter {
                        eq("requester_id", receiver.uid)
                        eq("receiver_id", uid)
                    }
                }.decodeList<Friendship>()
                if (oppositeCheck.isNotEmpty() && !oppositeCheck[0].accepted) { // if the opposite request exists just become friends
                    supabase.from("friendship").update({
                        set("accept_date", dateFormat.format(Date()))
                        set("accepted", true)
                    }) {
                        filter {
                            eq("requester_id", receiver.uid)
                            eq("receiver_id", uid)
                        }
                    }
                    fetchFriends()
                    onResult(true)
                } else if (oppositeCheck.isEmpty() && receiver.uid != uid) { // if not create a new friend request
                    val newRequest = Friendship(createdAt = dateFormat.format(Date()), requesterId = uid,
                        receiverId = receiver.uid, accepted = false)
                    supabase.from("friendship").insert(newRequest)
                    fetchFriends()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun removeFriend(username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val friendToRemove = supabase.from("users").select() {
                    filter {
                        eq("username", username);
                    }
                }.decodeSingle<User>()
                val res = supabase.from("friendship").delete() {
                    filter {
                        or {
                            and {
                                eq("requester_id", friendToRemove.uid);
                                eq("receiver_id", uid);
                            }
                            and {
                                eq("requester_id", uid);
                                eq("receiver_id", friendToRemove.uid);
                            }
                        }
                    }
                }
                fetchFriends()
                onResult(true)
            } catch(e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun acceptRequest(requester: String, receiver: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.from("friendship").update({
                    set("accept_date", dateFormat.format(Date()))
                    set("accepted", true)
                }){
                    filter {
                        eq("requester_id", requester)
                        eq("receiver_id", receiver)
                    }
                }
                fetchFriends()
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun declineRequest(requester: String, receiver: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.from("friendship").delete(){
                    filter {
                        eq("requester_id", requester)
                        eq("receiver_id", receiver)
                    }
                }
                fetchFriends()
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }

    }

    // create a group
    fun createGroup(groupName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try{
                val newGroup = Group(createdAt = dateFormat.format(Date()), name = groupName,
                    creatorId =  uid)
                supabase.from("groups").insert(newGroup)
                val groupCreated = supabase.from("groups").select() {
                    filter {
                        eq("name", groupName);
                        eq("creator_id", uid);
                    }
                }.decodeSingle<Group>()
                val newLink = GroupLink(createdAt = dateFormat.format(Date()), userId = uid,
                    groupId = groupCreated.id ?: 0, accepted = true)
                supabase.from("group_links").insert(newLink)
                fetchGroups()
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // create a group request to add someone to the group
    fun createGroupRequest(groupId: Long, userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try{
                val newRequest = GroupLink(createdAt = dateFormat.format(Date()), userId = userId,
                    groupId = groupId, accepted = false)
                supabase.from("group_links").insert(newRequest)
                fetchGroups()
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // leave group
    fun removeGroup(groupId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val groupDelete = supabase.from("groups").select() {
                    filter {
                        eq("id", groupId);
                    }
                }.decodeSingle<Group>()
                supabase.from("group_links").delete() {
                    filter {
                        eq("user_uid", uid);
                        eq("group_id", groupId);
                    }
                }
                if (groupDelete.creatorId == uid) { // if the creator leaves the group, delete group
                    supabase.from("groups").delete() {
                        filter {
                            eq("id", groupId);
                        }
                    }
                }
                fetchGroups()
                onResult(true)
            } catch(e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // accept group requests
    fun acceptGroupRequest(groupId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.from("group_links").update({
                    set("accepted", true)
                }){
                    filter {
                        eq("user_uid", uid)
                        eq("group_id", groupId)
                    }
                }
                fetchGroups()
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // decline group requests
    fun declineGroupRequest(groupId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.from("group_links").delete() {
                    filter {
                        eq("user_uid", uid);
                        eq("group_id", groupId);
                    }
                }
                fetchGroups()
                onResult(true)
            } catch(e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // get group member list of the given group id
    fun getGroupMembers(groupId: Long) {
        viewModelScope.launch {
            try {
                fetchGroups()
                val groupLinkList = supabase.from("group_links").select() {
                    filter {
                        eq("group_id", groupId);
                    }
                }.decodeList<GroupLink>()
                val tempMembers = mutableListOf<User>()
                for (groupLink in groupLinkList) {
                    val memberUID = groupLink.userId
                    val tempMember =  supabase.from("users")
                        .select() {
                            filter {
                                eq("uid", memberUID)
                            }
                        }.decodeSingle<User>()
                    tempMembers.add(tempMember)
                }
                groupMemberList = tempMembers
                Log.d("groupMembers", "Success!")
            } catch(e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
            }
        }
    }

    // End of Database Functions

}


