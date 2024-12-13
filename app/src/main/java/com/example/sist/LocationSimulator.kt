package com.example.sist

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.*

class LocationSimulator(private val fusedLocationClient: FusedLocationProviderClient, private val context: Context) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var simulationJob: Job? = null

    // Ruta de ejemplo (coordenadas de una ruta)
    private val simulationRoute = listOf(
        Pair(19.4326, -99.1332),  // Ciudad de México (punto inicial)
        Pair(19.4364, -99.1425),  // Movimiento 1
        Pair(19.4402, -99.1518),  // Movimiento 2
        Pair(19.4440, -99.1611)   // Movimiento 3
    )

    fun startSimulation() {
        // Comprobar si los permisos están concedidos
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

            // Si no se tienen los permisos, solicitarlos
            ActivityCompat.requestPermissions(
                context as Activity,  // Necesitamos el contexto de la actividad para solicitar permisos
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_PERMISSION_CODE
            )
            return
        }

        // Si los permisos están concedidos, inicia la simulación
        simulationJob = coroutineScope.launch {
            simulationRoute.forEachIndexed { index, (latitude, longitude) ->

                val location = Location("MockProvider").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                    time = System.currentTimeMillis()
                    accuracy = 10f
                }
                Log.d("Simulación", location.toString())
                // Simular ubicación
                fusedLocationClient.setMockMode(true)
                fusedLocationClient.setMockLocation(location)

                // Esperar antes de la siguiente ubicación
                delay(5000)  // 5 segundos entre cada punto
            }
        }
    }

    fun stopSimulation() {
        // Comprobar si los permisos están concedidos antes de detener la simulación
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

            // Si no se tienen los permisos, mostrar un mensaje de error
            Toast.makeText(context, "Permisos de ubicación no concedidos", Toast.LENGTH_SHORT).show()
            return
        }

        // Si los permisos están concedidos, cancelar la simulación
        simulationJob?.cancel()
        fusedLocationClient.setMockMode(false)
    }

    companion object {
        const val REQUEST_PERMISSION_CODE = 1001
    }
}
