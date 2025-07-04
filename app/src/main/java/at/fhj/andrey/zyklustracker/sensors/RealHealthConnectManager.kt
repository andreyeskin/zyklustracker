package at.fhj.andrey.zyklustracker.sensors

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.*
import java.time.Instant
import java.time.temporal.ChronoUnit


data class SensorData(
    val heartRate: Float,
    val oxygenSaturation: Float,
    val bodyTemperature: Float
)

class RealHealthConnectManager(context: Context) {

    private val client = HealthConnectClient.getOrCreate(context)

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val requiredPermissionsSet: Set<String> = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class)
    )

    /**
     * Checks which of the required permissions are currently granted.
     * Uses callbacks for Java interop.
     */

    fun checkGrantedPermissions(
        onSuccess: (grantedPermissions: Set<String>) -> Unit,
        onError: (exception: Exception) -> Unit
    ) {
        managerScope.launch {
            try {
                Log.d("HealthConnectManager", "Checking granted permissions against required: $requiredPermissionsSet")

                val allGrantedBySystem: Set<String> = client.permissionController.getGrantedPermissions()
                Log.d("HealthConnectManager", "All permissions granted by system to app: $allGrantedBySystem")


                val actuallyGrantedRequiredPermissions = requiredPermissionsSet.intersect(allGrantedBySystem)
                Log.d("HealthConnectManager", "Intersection (actually granted from our set): $actuallyGrantedRequiredPermissions")

                withContext(Dispatchers.Main) { // Switch to Main for UI-safe callback
                    onSuccess(actuallyGrantedRequiredPermissions)
                }
            } catch (e: Exception) {
                Log.e("HealthConnectManager", "Error checking permissions: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }



    /**
     * Reads the latest sensor data from Health Connect.
     * Uses callbacks for Java interop.
     * Returns SensorData or null via onSuccess if no relevant data found.
     */
    fun readLatestSensorData(
        onSuccess: (sensorData: SensorData?) -> Unit,
        onError: (exception: Exception) -> Unit
    ) {
        managerScope.launch {
            try {
                val end = Instant.now()

                val start = end.minus(12, ChronoUnit.HOURS)
                val range = TimeRangeFilter.between(start, end)

                Log.d("HealthConnectDebug", "Erfasse Sensordaten für Zeitraum: $start - $end")

                // 1) HeartRate
                val hr: Float = try {
                    Log.d("HealthConnectDebug", "HeartRateRecord...")
                    val hrResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = HeartRateRecord::class,
                            timeRangeFilter = range,
                            ascendingOrder = false,
                            pageSize = 1
                        )
                    )
                    Log.d("HealthConnectDebug", "Becommen ${hrResponse.records.size}  HeartRateRecord.")
                    hrResponse.records.firstOrNull()?.samples?.firstOrNull()?.beatsPerMinute?.toFloat() ?: 0f.also {
                        Log.d("HealthConnectDebug", "Final Puls: $it")
                    }
                } catch (e: Exception) {
                    Log.e("HealthConnectDebug", "Fehler HeartRate: ${e.message}", e)
                    0f
                }

                // 2) Oxygen Saturation
                val spO2: Float = try {
                    Log.d("HealthConnectDebug", "OxygenSaturationRecord...")
                    val o2Response = client.readRecords(
                        ReadRecordsRequest(
                            recordType = OxygenSaturationRecord::class,
                            timeRangeFilter = range,
                            ascendingOrder = false,
                            pageSize = 1
                        )
                    )
                    Log.d("HealthConnectDebug", " ${o2Response.records.size}  OxygenSaturationRecord.")
                    o2Response.records.firstOrNull()?.percentage?.value?.toFloat() ?: 0f.also {
                        Log.d("HealthConnectDebug", ": $it")
                    }
                } catch (e: Exception) {
                    Log.e("HealthConnectDebug", " OxygenSaturation: ${e.message}", e)
                    0f
                }

                // 3) Body Temperature
                val temp: Float = try {
                    Log.d("HealthConnectDebug", "BodyTemperatureRecord...")
                    val tempResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = BodyTemperatureRecord::class,
                            timeRangeFilter = range,
                            ascendingOrder = false,
                            pageSize = 10  // Увеличиваем до 10 записей
                        )
                    )
                    Log.d("HealthConnectDebug", " ${tempResponse.records.size}  BodyTemperatureRecord.")

                    // Детальное логирование всех найденных записей
                    tempResponse.records.forEachIndexed { index, record ->
                        Log.d("HealthConnectDebug", " $index: ${record.temperature.inCelsius}°C в ${record.time}")
                    }

                    val result = tempResponse.records.firstOrNull()?.temperature?.inCelsius?.toFloat() ?: 0f
                    Log.d("HealthConnectDebug", " $result")
                    result
                } catch (e: Exception) {
                    Log.e("HealthConnectDebug", " ${e.message}", e)
                    0f
                }


                val resultData = if (hr == 0f && spO2 == 0f && temp == 0f) {
                    null
                } else {
                    SensorData(heartRate = hr, oxygenSaturation = spO2, bodyTemperature = temp)
                }
                Log.d("HealthConnectDebug", "Final result: $resultData")
                withContext(Dispatchers.Main) {
                    onSuccess(resultData)
                }

            } catch (e: Exception) { // Общий catch для всего процесса чтения
                Log.e("HealthConnectManager", "Error reading latest sensor data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    /**
     * Call this method when the manager is no longer needed to cancel ongoing coroutines.
     * For example, in ViewModel.onCleared() or Activity.onDestroy().
     */
    fun cleanup() {
        managerScope.cancel()
    }
}