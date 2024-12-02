package com.example.fomo.models

import android.Manifest
import android.util.Log
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
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
import kotlinx.serialization.json.Json
import com.google.maps.android.PolyUtil
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.fomo.R
import com.example.fomo.showNotification
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class MyViewModel : ViewModel() {


    private val _bitmapDescriptor = mutableStateOf<BitmapDescriptor?>(null)
    private val _sessionRestored = MutableStateFlow(false)
    private val _isDataLoaded = MutableStateFlow(false)
    private var previousGroupRequestCount by mutableStateOf(0)
    private var previousFriendRequestCount by mutableStateOf(0)

    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded
    var bitmapDescriptor by _bitmapDescriptor
    val sessionRestored: StateFlow<Boolean> = _sessionRestored

    suspend fun uriToBitmapDescriptor(context: Context, imageUri: String): BitmapDescriptor? {

        Log.d("ImageLoading", imageUri)
        var request = ImageRequest.Builder(context)
            .data(imageUri) // Uses either the URI or fallback drawable
            .allowHardware(false) // Shown if data is null
            .build()

        var result = context.imageLoader.execute(request)

        if (!(result is SuccessResult)){
             request = ImageRequest.Builder(context)
                .data(R.drawable.default_pfp) // Uses either the URI or fallback drawable
                .allowHardware(false)
                .build()
            result = context.imageLoader.execute(request)
        }

        if (result is SuccessResult) {
            Log.d("ImageLoading", "Image loaded successfully")
            val originalBitmap = result.drawable.toBitmap()
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 124, 124, true) // Resize to 48x48
            val roundedBitmap = createRoundedBitmap(resizedBitmap, cornerRadius = 32) // Add rounded corners

            val borderedBitmap = addWhiteBorder(roundedBitmap, 8) // Add 8px white border
            val roundedBitmap2 = createRoundedBitmap(borderedBitmap, cornerRadius = 32) // Add rounded corners


            return BitmapDescriptorFactory.fromBitmap(roundedBitmap2)
        } else {
            Log.e("ImageLoading", "Failed to load image: $result")
            return null
        }
    }


    private fun addWhiteBorder(bitmap: Bitmap, borderSize: Int): Bitmap {
        val width = bitmap.width + borderSize * 2
        val height = bitmap.height + borderSize * 2
        val bitmapWithBorder = Bitmap.createBitmap(width, height, bitmap.config)

        val canvas = Canvas(bitmapWithBorder)
        canvas.drawColor(Color.WHITE) // Draw white border
        canvas.drawBitmap(bitmap, borderSize.toFloat(), borderSize.toFloat(), null) // Draw original bitmap

        return bitmapWithBorder
    }

    private fun createRoundedBitmap(bitmap: Bitmap, cornerRadius: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())

        // Draw rounded rectangle as the background
        canvas.drawRoundRect(rect, cornerRadius.toFloat(), cornerRadius.toFloat(), paint)

        // Apply PorterDuff mode to overlay the original bitmap
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, null, rect, paint)

        return output
    }



    fun loadImage(context: Context, imageUri: String){
        Log.d("bitMapDescriptor", "called")
        viewModelScope.launch {
            _bitmapDescriptor.value = uriToBitmapDescriptor(context,imageUri)
        }
        Log.d("bitMapDescriptor", "updated ${_bitmapDescriptor.value}")
    }

    private val _friendIcons = mutableStateMapOf<String, BitmapDescriptor?>()
    val friendIcons: Map<String, BitmapDescriptor?> get() = _friendIcons

    fun loadFriendImage(context: Context, friend: User){
        Log.d("ImageLoading", "loadFriendImageCalled")
        viewModelScope.launch {
            _friendIcons[friend.uid] = uriToBitmapDescriptor(context, getImgUrl(friend.uid))
        }
        Log.d("ImageLoading", "updated ${_friendIcons[uid]}")
    }




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

    fun setSignedInState(isSignedIn: Boolean) {
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
    var status by mutableStateOf(defaultStatus)
    var friendsList by mutableStateOf<List<User>>(emptyList())
    var requestList by mutableStateOf<List<User>>(emptyList())
    var groupMemberList by mutableStateOf<List<User>>(emptyList())
    var statusList by mutableStateOf<List<Status>>(emptyList())
    var groupList by mutableStateOf<List<Group>>(emptyList())
    var groupRequestList by mutableStateOf<List<Pair<User, Group>>>(emptyList())
    var selectedLocation by mutableStateOf<LatLng?>(null)
    var route by mutableStateOf<List<LatLng>?>(null)
    var mode by mutableStateOf("walking")
    var places by mutableStateOf<List<Place>>(emptyList())
    var groupIndex by mutableIntStateOf(-1)

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
    fun saveSession(context: Context, accessToken: String, refreshToken: String) {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("ACCESS_TOKEN", accessToken)
            putString("REFRESH_TOKEN", refreshToken)
            apply()
        }
    }
    fun restoreSession(context: Context, onResult: (Boolean) -> Unit) {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("ACCESS_TOKEN", null)
        val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", null)

        if (accessToken != null && refreshToken != null) {
            viewModelScope.launch {
                try {
                    supabase.auth.refreshSession(refreshToken = refreshToken) // Refresh session
                    val session = supabase.auth.currentSessionOrNull()
                    if (session != null) {
                        uid = supabase.auth.retrieveUserForCurrentSession(updateSession = true).id
                        setSignedInState(true)
                        _sessionRestored.value = true
                        onResult(true)
                        fetchDatabase(context)
                    } else {
                        _sessionRestored.value = true
                        onResult(false)
                    }
                } catch (e: Exception) {
                    Log.e("SessionRecovery", "Failed to restore session: ${e.message}")
                    _sessionRestored.value = true
                    onResult(false)
                }
            }
        } else {
            _sessionRestored.value = true
            onResult(false)
        }
    }
    fun clearSession(context: Context) {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }



    fun signUp(context: Context, email: String, password: String, onResult: (Boolean) -> Unit) {
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
    fun signIn(context: Context, email: String, password: String, onResult: (Boolean) -> Unit) {
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
                    saveSession(context, session.accessToken, session.refreshToken)
                    uid = user.id
                    onResult(true)
                    fetchDatabase(context)
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

    fun logout(context: Context) {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                Log.d("SupabaseAuth", "User signed out")
                setSignedInState(false)
                _isDataLoaded.value = false
                clearSession(context)
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
                if (groupIndex == -1) {
                    places = emptyList<Place>()
                } else {
                    places = supabase.from("places").select() {
                        filter {
                            eq("group_id", groupList[groupIndex].id!!.toLong())
                        }
                    }.decodeList<Place>()
                }
                Log.d("Supabase fetchPlaces()", "Places fetched")
            } catch (e: Exception) {
                Log.e("Supabase fetchPlaces()", "Error: ${e.message}")
            }
        }
    }

    // create place on current location
    fun createPlace(name:String, onResult: (Boolean) -> Unit) {

        viewModelScope.launch {
            try {
                val newPlace = Place(name = name, latitude = userLatitude,
                    longitude = userLongitude,  radius = 0.001, groupId = groupList[groupIndex].id!!.toLong())
                supabase.from("places").insert(newPlace)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
                Log.e("Supabase setPlace()", "Error: ${e.message}")
            }
        }
    }

    // Checks if the current user location is in a place in places
    fun getUserPlace(loc: LatLng, me: Boolean): Place? {
        for (place in places) {
            if (((place.latitude - place.radius < loc.latitude &&
                        loc.latitude < place.latitude + place.radius) &&
                        (place.longitude - place.radius < loc.longitude &&
                                loc.longitude < place.longitude + place.radius))) {
                if (me) {
                    predictStatus(place.name)
                }
                return place
            }
        }
        return null
    }

    fun removePlace(id: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.from("places").delete() {
                    filter {
                        eq("id", id)
                    }
                }
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
                Log.e("Supabase removePlace()", "Error: ${e.message}")
            }
        }
    }

    fun predictStatus(placeName: String): String {
        val upperPlaceName = placeName.uppercase()
        var predictedStatus = ""
        if (upperPlaceName == "HOME") {
            predictedStatus = "Chilling"
        } else if (upperPlaceName == "GYM") {
            predictedStatus = "Exercising"
        } else if (upperPlaceName == "SCHOOL") {
            predictedStatus = "Studying"
        } else if (upperPlaceName == "WORK") {
            predictedStatus = "Working"
        } else if (upperPlaceName == "RESTAURANT") {
            predictedStatus = "Eating"
        }
        if (predictedStatus != "") {
            updateStatus(statusList.filter {it.description == predictedStatus}[0])
        }
        return predictedStatus
    }

    fun fetchFriends(context: Context) {
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

                if (tempRequesters.size > previousFriendRequestCount){
                    Log.d("friendrequestsent", "Friendrequestsent")
                    val newRequest = tempRequesters.drop(previousFriendRequestCount)
                    newRequest.forEach {(friend)->
                        val message = "You have a new friend request"
                        showNotification(context, message)
                    }
                }

                previousFriendRequestCount = tempRequesters.size


            } catch (e: Exception) {
                Log.e("SupabaseConnection", "Failed to connect to database: ${e.message}")
            }
        }
    }

    suspend fun fetchGroups(context: Context) {
        try {
            val linkRes = supabase.from("group_links")
                .select() {
                    filter {
                        eq("uid", uid)
                    }
                }.decodeList<GroupLink>()
            val tempGroups = mutableListOf<Group>()
            val tempRequesters = mutableListOf<Pair<User, Group>>()
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
                    val sender = supabase.from("users")
                        .select() {
                            filter {
                                eq("uid", groupLink.senderUid)
                            }
                        }.decodeSingle<User>()
                    tempRequesters.add(Pair(sender, tempGroup))
                }
            }
            groupList = tempGroups
            groupRequestList = tempRequesters

            if (tempRequesters.size > previousGroupRequestCount){
                Log.d("friendrequestsent", "Friendrequestsent")
                val newRequest = tempRequesters.drop(previousGroupRequestCount)
                newRequest.forEach {(sender, group)->
                    val message = "You have a new group invite to ${group.name} from ${sender.displayName}"
                    showNotification(context, message)
                }
            }

            previousGroupRequestCount = tempRequesters.size
            Log.d("groupFetching", "Success!")
        } catch (e: Exception) {
            Log.e("SupabaseConnection", "Failed to connect to database: ${e.message}")
        }
    }

//    private fun showNotification(context: Context, message: String) {
//        val builder = NotificationCompat.Builder(context, "group_invite_channel")
//            .setSmallIcon(R.drawable.notification_icon) // Replace with your icon
//            .setContentTitle("New Group Invitation")
//            .setContentText(message)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//        with(NotificationManagerCompat.from(context)) {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
//                    context, Manifest.permission.POST_NOTIFICATIONS
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                notify(System.currentTimeMillis().toInt(), builder.build()) // Unique ID for each notification
//            }
//        }
//    }

    fun fetchDatabase(context: Context) {
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
                fetchFriends(context)
                fetchGroups(context)

                Log.d("SupabaseConnection", "Friends fetched: $friendsList")
                    _isDataLoaded.value = true
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

    fun createRequest(context: Context, username: String, onResult: (Boolean) -> Unit) {
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
                    fetchFriends(context)
                    onResult(true)
                } else if (oppositeCheck.isEmpty() && receiver.uid != uid) { // if not create a new friend request
                    val newRequest = Friendship(createdAt = dateFormat.format(Date()), requesterId = uid,
                        receiverId = receiver.uid, accepted = false)
                    supabase.from("friendship").insert(newRequest)
                    fetchFriends(context)
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

    fun removeFriend(context: Context, username: String, onResult: (Boolean) -> Unit) {
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
                fetchFriends(context)
                onResult(true)
            } catch(e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun acceptRequest(context: Context, requester: String, receiver: String, onResult: (Boolean) -> Unit) {
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
                fetchFriends(context)
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    fun declineRequest(context: Context, requester: String, receiver: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.from("friendship").delete(){
                    filter {
                        eq("requester_id", requester)
                        eq("receiver_id", receiver)
                    }
                }
                fetchFriends(context)
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }

    }

    // create a group
    fun createGroup(context:Context, groupName: String, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            var groupCreated: Group? = null
            try{
                val newGroup = Group(createdAt = dateFormat.format(Date()), name = groupName,
                    creatorId = uid)
                groupCreated = supabase.from("groups").insert(newGroup) {
                    select()
                }.decodeSingle<Group>()
                val newLink = GroupLink(createdAt = dateFormat.format(Date()), userId = uid,
                    senderUid = uid, groupId = groupCreated.id ?: 0, accepted = true)
                supabase.from("group_links").insert(newLink)
                fetchGroups(context)
                println("urmom1 ${groupCreated.id!!}")
                onResult(groupCreated.id!!)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(-1L)
            }
        }
    }

    // create a group request to add someone to the group
    fun createGroupRequest(context: Context, groupId: Long, userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try{
                val response = supabase.from("group_links").select() {
                    filter {
                        eq("uid", userId)
                        eq("group_id", groupId)
                    }
                }.decodeList<GroupLink>()

                if (response.isEmpty()) { // checking if it was already sent (by someone)
                    val newRequest = GroupLink(createdAt = dateFormat.format(Date()), userId = userId,
                        senderUid = uid, groupId = groupId, accepted = false)
                    supabase.from("group_links").insert(newRequest)
                    fetchGroups(context)
                }
                onResult(true)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // leave group
    fun removeGroup(context: Context, groupId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val groupDelete = supabase.from("groups").select() {
                    filter {
                        eq("id", groupId);
                    }
                }.decodeSingle<Group>()
                supabase.from("group_links").delete() {
                    filter {
                        eq("uid", uid);
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
                fetchGroups(context)
                onResult(true)
            } catch(e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // accept group requests
    fun acceptGroupRequest(context: Context, groupId: Long, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.from("group_links").update({
                    set("accepted", true)
                }){
                    filter {
                        eq("uid", uid)
                        eq("group_id", groupId)
                    }
                }
                fetchGroups(context)
                onResult(groupId)
            } catch (e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(-1L)
            }
        }
    }

    // decline group requests
    fun declineGroupRequest(context: Context, groupId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                supabase.from("group_links").delete() {
                    filter {
                        eq("uid", uid);
                        eq("group_id", groupId);
                    }
                }
                fetchGroups(context)
                onResult(true)
            } catch(e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(false)
            }
        }
    }

    // get group member list of the given group id
    fun getGroupMembers(context: Context, groupId: Long, onResult: (List<User>) -> Unit) {
        val tempMembers = mutableListOf<User>()
        viewModelScope.launch {
            try {
                fetchGroups(context)
                val groupLinkList = supabase.from("group_links").select() {
                    filter {
                        eq("group_id", groupId)
                        eq("accepted", true)
                        neq("uid", uid) // not current user
                    }
                }.decodeList<GroupLink>()
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
                onResult(groupMemberList)
            } catch(e: Exception) {
                Log.e("SupabaseConnection", "DB Error: ${e.message}")
                onResult(emptyList())
            }
        }
    }

    // End of Database Functions

}


