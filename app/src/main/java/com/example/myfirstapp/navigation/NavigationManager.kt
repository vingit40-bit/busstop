package com.example.myfirstapp.navigation

import android.content.Context
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class NavigationState(
    val isNavigating: Boolean = false,
    val currentInstruction: String = "Ready for navigation",
    val nextTurnDistanceMeters: Int = 0,
    val totalDistanceRemainingKm: Double = 0.0,
    val totalEtaMinutes: Int = 0,
    val isMuted: Boolean = false,
    val isOffRoute: Boolean = false,
    val currentStepIndex: Int = 0,
    val stepsCount: Int = 0,
    val routeCoordinates: List<Pair<Double, Double>> = emptyList(),
    val destinationName: String = ""
)

class NavigationManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state

    private var steps = mutableListOf<ManeuverStep>()
    private var polylinePoints = mutableListOf<Pair<Double, Double>>()
    private var destinationLat: Double = 0.0
    private var destinationLng: Double = 0.0
    private var destinationName: String = ""

    private var lastRecalculateTime: Long = 0L

    init {
        initTts()
    }

    private fun initTts() {
        val appContext = context.applicationContext
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = true
        } catch (_: Exception) {}

        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                var result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts?.setLanguage(Locale("fil", "PH"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.getDefault())
                    }
                }

                val audioAttr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
                tts?.setAudioAttributes(audioAttr)
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)
            } else {
                Log.e("NavigationManager", "TTS initialization failed: $status")
            }
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (_state.value.isMuted || text.isBlank() || !isTtsReady) return

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, params, "Nav_${System.currentTimeMillis()}")
    }

    fun toggleMute(): Boolean {
        val newMuted = !_state.value.isMuted
        _state.value = _state.value.copy(isMuted = newMuted)
        if (newMuted) {
            tts?.stop()
        } else {
            speak("Voice guidance enabled", flush = true)
        }
        return newMuted
    }

    fun startNavigation(
        currentLat: Double,
        currentLng: Double,
        destLat: Double,
        destLng: Double,
        destName: String
    ) {
        destinationLat = destLat
        destinationLng = destLng
        destinationName = destName

        _state.value = _state.value.copy(
            isNavigating = true,
            destinationName = destName,
            currentInstruction = "Starting route navigation to $destName..."
        )

        speak("Starting route navigation to $destName", flush = true)

        fetchRoute(currentLat, currentLng, destLat, destLng)
    }

    fun stopNavigation() {
        tts?.stop()
        speak("Navigation ended", flush = true)
        _state.value = NavigationState()
        steps.clear()
        polylinePoints.clear()
    }

    fun onLocationUpdated(location: Location) {
        if (!_state.value.isNavigating || steps.isEmpty()) return

        val lat = location.latitude
        val lng = location.longitude

        // 1. Off-route deviation check (> 120m from polyline)
        if (isOffRoute(lat, lng) && (System.currentTimeMillis() - lastRecalculateTime > 12000L)) {
            lastRecalculateTime = System.currentTimeMillis()
            _state.value = _state.value.copy(isOffRoute = true, currentInstruction = "Recalculating route...")
            speak("Recalculating route", flush = true)
            fetchRoute(lat, lng, destinationLat, destinationLng)
            return
        }

        // 2. Find next uncompleted maneuver step
        val nextStepIdx = steps.indexOfFirst { !it.isCompleted }
        if (nextStepIdx == -1) {
            // All steps completed -> Arrived at destination
            val distToDest = distanceBetween(lat, lng, destinationLat, destinationLng)
            if (distToDest <= 60) {
                _state.value = _state.value.copy(
                    currentInstruction = "You have arrived at $destinationName. Navigation complete.",
                    nextTurnDistanceMeters = 0,
                    totalDistanceRemainingKm = 0.0,
                    totalEtaMinutes = 0
                )
                speak("You have arrived at your destination, $destinationName. Navigation complete.", flush = true)
                stopNavigation()
            }
            return
        }

        val step = steps[nextStepIdx]
        val distToStep = distanceBetween(lat, lng, step.latitude, step.longitude)
        val distInt = distToStep.toInt()

        // Total remaining distance & ETA calculation
        val remainingKm = calculateRemainingDistanceKm(nextStepIdx, distToStep)
        val etaMinutes = (remainingKm / 35.0 * 60).toInt().coerceAtLeast(1)

        val bannerInstruction = if (distInt > 200) {
            "In $distInt m, ${step.instruction}"
        } else {
            step.instruction
        }

        _state.value = _state.value.copy(
            currentInstruction = bannerInstruction,
            nextTurnDistanceMeters = distInt,
            totalDistanceRemainingKm = remainingKm,
            totalEtaMinutes = etaMinutes,
            currentStepIndex = nextStepIdx,
            stepsCount = steps.size
        )

        // 3. Proximity voice announcements
        if (distInt in 150..220 && !step.isSpokenEarly) {
            step.isSpokenEarly = true
            speak("In $distInt meters, ${step.instruction}", flush = true)
        } else if (distInt <= 45 && !step.isSpokenImmediate) {
            step.isSpokenImmediate = true
            step.isCompleted = true
            speak(step.instruction, flush = true)
        }
    }

    private fun fetchRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ) {
        scope.launch {
            try {
                val osrmUrl = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson&steps=true"
                val connection = (URL(osrmUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val routesArr = root.optJSONArray("routes")

                if (routesArr != null && routesArr.length() > 0) {
                    val routeObj = routesArr.getJSONObject(0)
                    val geometryObj = routeObj.optJSONObject("geometry")
                    val coordsArr = geometryObj?.optJSONArray("coordinates")

                    val newPolyline = mutableListOf<Pair<Double, Double>>()
                    if (coordsArr != null) {
                        for (i in 0 until coordsArr.length()) {
                            val c = coordsArr.getJSONArray(i)
                            val lng = c.getDouble(0)
                            val lat = c.getDouble(1)
                            newPolyline.add(Pair(lat, lng))
                        }
                    }

                    val legsArr = routeObj.optJSONArray("legs")
                    val newSteps = mutableListOf<ManeuverStep>()

                    if (legsArr != null && legsArr.length() > 0) {
                        val legObj = legsArr.getJSONObject(0)
                        val stepsArr = legObj.optJSONArray("steps")

                        if (stepsArr != null) {
                            for (i in 0 until stepsArr.length()) {
                                val stObj = stepsArr.getJSONObject(i)
                                val mObj = stObj.optJSONObject("maneuver")
                                val locArr = mObj?.optJSONArray("location")

                                val mLat = locArr?.optDouble(1) ?: endLat
                                val mLng = locArr?.optDouble(0) ?: endLng
                                val mType = mObj?.optString("type", "turn") ?: "turn"
                                val modifier = mObj?.optString("modifier", "") ?: ""
                                val street = stObj.optString("name", "")
                                val streetSuffix = if (street.isNotBlank()) "onto $street" else ""

                                val rawText = when {
                                    i == 0 -> "Head $modifier towards $destinationName"
                                    mType.contains("arrive") -> "Arrive at $destinationName"
                                    else -> "Turn $modifier $streetSuffix".trim()
                                }

                                newSteps.add(
                                    ManeuverStep(
                                        stepId = i,
                                        latitude = mLat,
                                        longitude = mLng,
                                        instruction = rawText,
                                        maneuverType = mType,
                                        modifier = modifier,
                                        streetName = street,
                                        distanceMeters = stObj.optDouble("distance", 0.0)
                                    )
                                )
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        polylinePoints = newPolyline
                        steps = newSteps
                        _state.value = _state.value.copy(
                            isOffRoute = false,
                            routeCoordinates = newPolyline,
                            stepsCount = newSteps.size,
                            currentInstruction = if (newSteps.isNotEmpty()) newSteps[0].instruction else "Follow route"
                        )
                    }
                } else {
                    fallbackRoute(startLat, startLng, endLat, endLng)
                }
            } catch (e: Exception) {
                Log.e("NavigationManager", "Route fetch error: ${e.localizedMessage}")
                fallbackRoute(startLat, startLng, endLat, endLng)
            }
        }
    }

    private suspend fun fallbackRoute(sLat: Double, sLng: Double, eLat: Double, eLng: Double) {
        val midLat = (sLat + eLat) / 2.0
        val midLng = (sLng + eLng) / 2.0

        val newPoly = listOf(Pair(sLat, sLng), Pair(midLat, midLng), Pair(eLat, eLng))
        val newSteps = listOf(
            ManeuverStep(0, sLat, sLng, "Head towards $destinationName"),
            ManeuverStep(1, midLat, midLng, "Continue straight along route"),
            ManeuverStep(2, eLat, eLng, "Arrive at $destinationName")
        )

        withContext(Dispatchers.Main) {
            polylinePoints = newPoly.toMutableList()
            steps = newSteps.toMutableList()
            _state.value = _state.value.copy(
                isOffRoute = false,
                routeCoordinates = newPoly,
                stepsCount = newSteps.size,
                currentInstruction = newSteps[0].instruction
            )
        }
    }

    private fun isOffRoute(lat: Double, lng: Double): Boolean {
        if (polylinePoints.isEmpty()) return false
        var minDist = Double.MAX_VALUE
        for (pt in polylinePoints) {
            val d = distanceBetween(lat, lng, pt.first, pt.second)
            if (d < minDist) minDist = d
        }
        return minDist > 120.0
    }

    private fun calculateRemainingDistanceKm(nextIdx: Int, distToNextStep: Double): Double {
        var total = distToNextStep
        for (i in (nextIdx + 1) until steps.size) {
            total += steps[i].distanceMeters
        }
        return (total / 1000.0).let { Math.round(it * 10.0) / 10.0 }
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    fun onDestroy() {
        scope.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}
