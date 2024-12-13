package com.example.sist.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.sist.R
import io.socket.client.IO
import com.google.android.gms.location.*
import io.socket.client.Socket
import org.json.JSONObject
import java.util.*

class PanicButtonFragment : Fragment() {
    private lateinit var socket: Socket
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var startTrackingButton: Button
    private lateinit var stopTrackingButton: Button

    private val DEVICE_ID = UUID.randomUUID().toString()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var isTracking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_button_panic, container, false)

        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 5000
            }
            socket = IO.socket("http://10.0.2.2:3000", options)

            socket.on(Socket.EVENT_CONNECT) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Conectado al servidor", Toast.LENGTH_SHORT).show()
                    Log.d("SocketIO", "Conexión establecida")
                }
            }

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                requireActivity().runOnUiThread {
                    val error = args[0].toString()
                    Toast.makeText(requireContext(), "Error de conexión: $error", Toast.LENGTH_SHORT).show()
                    Log.e("SocketIO", "Error de conexión: $error")
                }
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Desconectado del servidor", Toast.LENGTH_SHORT).show()
                    Log.d("SocketIO", "Desconectado")
                }
            }

            socket.connect()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al conectar: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        startTrackingButton = view.findViewById(R.id.btn_panic)
        stopTrackingButton = view.findViewById(R.id.btn_stop)

        startTrackingButton.setOnClickListener {
            if (!isTracking) {
                if (checkLocationPermissions()) {
                    startLocationUpdates()
                    startTrackingButton.isEnabled = false
                    stopTrackingButton.isEnabled = true
                    isTracking = true

                    Toast.makeText(requireContext(), "Seguimiento iniciado", Toast.LENGTH_SHORT).show()
                } else {
                    requestLocationPermissions()
                }
            }
        }

        stopTrackingButton.setOnClickListener {
            if (isTracking) {
                stopLocationUpdates()
                startTrackingButton.isEnabled = true
                stopTrackingButton.isEnabled = false
                isTracking = false

                Toast.makeText(requireContext(), "Seguimiento detenido", Toast.LENGTH_SHORT).show()
            }
        }

        stopTrackingButton.isEnabled = false

        return view
    }

    private fun checkLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = JSONObject().apply {
                        put("deviceId", DEVICE_ID)
                        put("latitude", location.latitude)
                        put("longitude", location.longitude)
                        put("timestamp", System.currentTimeMillis())
                    }
                    Log.d("Location", locationData.toString())
                    socket.emit("location_update", locationData)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isTracking) stopLocationUpdates()
        socket.disconnect()
    }
}