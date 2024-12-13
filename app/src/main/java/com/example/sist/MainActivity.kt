package com.example.sist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.socket.client.IO
import com.google.android.gms.location.*
import io.socket.client.Socket
import org.json.JSONObject
import java.util.*

 class MainActivity : AppCompatActivity() {
    private lateinit var socket: Socket
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var startTrackingButton: Button
    private lateinit var stopTrackingButton: Button

    private val DEVICE_ID = UUID.randomUUID().toString()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var isTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            val options = IO.Options().apply {
                // Configuraciones importantes
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 5000
            }

            // Usar IP correcta para el emulador o dispositivo
            socket = IO.socket("http://10.0.2.2:3000", options)

            // Listeners de eventos de conexión
            socket.on(Socket.EVENT_CONNECT) {
                runOnUiThread {
                    Toast.makeText(this, "Conectado al servidor", Toast.LENGTH_SHORT).show()
                    Log.d("SocketIO", "Conexión establecida")
                }
            }

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                runOnUiThread {
                    val error = args[0].toString()
                    Toast.makeText(this, "Error de conexión: $error", Toast.LENGTH_SHORT).show()
                    Log.e("SocketIO", "Error de conexión: $error")
                }
            }

            socket.on(Socket.EVENT_DISCONNECT) { args ->
                runOnUiThread {
                    Toast.makeText(this, "Desconectado del servidor", Toast.LENGTH_SHORT).show()
                    Log.d("SocketIO", "Desconectado")
                }
            }

            // Conectar
            socket.connect()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al conectar: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Inicializar Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar botones
        startTrackingButton = findViewById(R.id.startTrackingButton)
        stopTrackingButton = findViewById(R.id.stopTrackingButton)


        // Listener para botón de inicio de seguimiento
        startTrackingButton.setOnClickListener {
            if (!isTracking) {
                if (checkLocationPermissions()) {
                    startLocationUpdates()
                    startTrackingButton.isEnabled = false
                    stopTrackingButton.isEnabled = true
                    isTracking = true

                    Toast.makeText(this, "Seguimiento iniciado", Toast.LENGTH_SHORT).show()
                } else {
                    requestLocationPermissions()
                }
            }
        }

        // Listener para botón de detención de seguimiento
        stopTrackingButton.setOnClickListener {
            if (isTracking) {
                stopLocationUpdates()
                startTrackingButton.isEnabled = true
                stopTrackingButton.isEnabled = false
                isTracking = false

                Toast.makeText(this, "Seguimiento detenido", Toast.LENGTH_SHORT).show()
            }
        }

        // Inicialmente, el botón de detener está deshabilitado
        stopTrackingButton.isEnabled = false



    }

    private fun checkLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Cada 10 segundos
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Enviar ubicación al servidor
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

        // Verificar permisos antes de solicitar actualizaciones
        if (ActivityCompat.checkSelfPermission(
                this,
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisos concedidos, iniciar seguimiento
                startLocationUpdates()
                startTrackingButton.isEnabled = false
                stopTrackingButton.isEnabled = true
                isTracking = true
            } else {
                // Manejar caso de permisos denegados
                Toast.makeText(
                    this,
                    "Permisos de ubicación necesarios",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener actualizaciones de ubicación
        if (isTracking) {
            stopLocationUpdates()
        }
        // Desconectar socket
        socket.disconnect()
    }
}