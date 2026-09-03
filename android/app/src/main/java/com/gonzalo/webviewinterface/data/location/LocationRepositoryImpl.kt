package com.gonzalo.webviewinterface.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.gonzalo.webviewinterface.domain.model.LocationErrorCode
import com.gonzalo.webviewinterface.domain.model.LocationResult
import com.gonzalo.webviewinterface.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [FusedLocationProviderClient]를 이용해 현재 위치를 조회하는 구현체.
 * 권한/위치서비스 상태를 먼저 확인하고, 10초 타임아웃을 두고 위치를 요청한다.
 */
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.Failure(LocationErrorCode.PERMISSION_DENIED)
        }
        if (!isLocationServiceEnabled()) {
            return LocationResult.Failure(LocationErrorCode.LOCATION_DISABLED)
        }

        return try {
            withTimeout(TIMEOUT_MS) {
                val location = suspendCancellableCoroutine<android.location.Location?> { continuation ->
                    val cancellationTokenSource = CancellationTokenSource()
                    continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
                    try {
                        fusedLocationProviderClient
                            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                            .addOnSuccessListener { location ->
                                if (continuation.isActive) continuation.resume(location)
                            }
                            .addOnFailureListener { error ->
                                if (continuation.isActive) continuation.resumeWithException(error)
                            }
                    } catch (e: SecurityException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }

                if (location != null) {
                    LocationResult.Success(location.latitude, location.longitude)
                } else {
                    LocationResult.Failure(LocationErrorCode.UNKNOWN)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "getCurrentLocation() timed out")
            LocationResult.Failure(LocationErrorCode.TIMEOUT)
        } catch (e: SecurityException) {
            Log.w(TAG, "getCurrentLocation() missing permission", e)
            LocationResult.Failure(LocationErrorCode.PERMISSION_DENIED)
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentLocation() failed", e)
            LocationResult.Failure(LocationErrorCode.UNKNOWN)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    companion object {
        private const val TAG = "LocationRepositoryImpl"
        private const val TIMEOUT_MS = 10_000L
    }
}
