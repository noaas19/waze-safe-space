package com.wazesafespace

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.maps.android.PolyUtil
import com.wazesafespace.ui.theme.WazeSafeSpaceTheme
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : FragmentActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private val TAG = "MainActivity"
    private val DIRECTIONS_API_KEY = "AIzaSyBalGEcpzKfl4Aixz30b8-ggW0Yp1JTJXM"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        enableEdgeToEdge()

        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Firestore
        val db = FirebaseFirestore.getInstance()

        // Initialize Firebase Cloud Messaging (FCM)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        // Initialize Firebase Analytics
        val analytics = FirebaseAnalytics.getInstance(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up Compose content
        setContent {
            WazeSafeSpaceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /*override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        // Fetch directions from a sample origin to destination
        fetchDirections(LatLng(-34.0, 151.0), LatLng(-33.8675, 151.2070))
    }*/

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Fetch directions from Jerusalem to Tel Aviv
        val jerusalem = LatLng(31.7683, 35.2137)
        val telAviv = LatLng(32.0853, 34.7818)
        fetchDirections(jerusalem, telAviv)
    }

    private fun fetchDirections(origin: LatLng, destination: LatLng) {
        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}&" +
                "destination=${destination.latitude},${destination.longitude}&" +
                "key=$DIRECTIONS_API_KEY"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Directions API request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "Directions API response not successful: $response")
                    return
                }

                val responseData = response.body?.string()
                Log.d(TAG, "Directions API response: $responseData")

                responseData?.let {
                    val jsonResponse = JSONObject(it)
                    val routes = jsonResponse.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val overviewPolyline = routes.getJSONObject(0)
                            .getJSONObject("overview_polyline")
                            .getString("points")

                        runOnUiThread {
                            displayRoute(overviewPolyline)
                        }
                    }
                }
            }
        })
    }

    private fun displayRoute(encodedPolyline: String) {
        val decodedPath = PolyUtil.decode(encodedPolyline)
        val polylineOptions = PolylineOptions()
            .addAll(decodedPath)
            .color(Color.BLUE)
            .width(10f)

        mMap.addPolyline(polylineOptions)

        // Add markers at the start and end points
        if (decodedPath.isNotEmpty()) {
            mMap.addMarker(MarkerOptions().position(decodedPath[0]).title("Start"))
            mMap.addMarker(MarkerOptions().position(decodedPath[decodedPath.size - 1]).title("End"))
        }

        // Move the camera to view the entire track
        val builder = LatLngBounds.Builder()
        for (point in decodedPath) {
            builder.include(point)
        }
        val bounds = builder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WazeSafeSpaceTheme {
        Greeting("Android")
    }
}

