package com.tescha.food.service

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationService(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? = suspendCoroutine { cont ->
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                cont.resume(location?.let { it.latitude to it.longitude })
            }
            .addOnFailureListener {
                fusedClient.lastLocation
                    .addOnSuccessListener { last ->
                        cont.resume(last?.let { it.latitude to it.longitude })
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
    }
}
