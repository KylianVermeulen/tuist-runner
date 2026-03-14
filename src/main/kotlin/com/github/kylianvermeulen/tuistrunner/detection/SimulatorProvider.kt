package com.github.kylianvermeulen.tuistrunner.detection

import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.json.*

data class Simulator(
    val udid: String,
    val name: String,
    val runtime: String,
    val state: String,
) {
    val displayName: String get() = "$name ($runtime)"
    val destinationString: String get() = "id=$udid"
}

object SimulatorProvider {

    private val LOG = logger<SimulatorProvider>()

    private val RUNTIME_REGEX = Regex("""com\.apple\.CoreSimulator\.SimRuntime\.(.+)""")

    fun listAvailableSimulators(): List<Simulator> {
        try {
            val process = ProcessBuilder("xcrun", "simctl", "list", "devices", "available", "--json")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                LOG.warn("xcrun simctl exited with code $exitCode: $output")
                return emptyList()
            }
            return parseSimctlJson(output)
        } catch (e: Exception) {
            LOG.warn("Failed to run xcrun simctl", e)
            return emptyList()
        }
    }

    fun parseSimctlJson(json: String): List<Simulator> {
        val root = Json.parseToJsonElement(json).jsonObject
        val devices = root["devices"]?.jsonObject ?: return emptyList()
        val result = mutableListOf<Simulator>()

        for ((runtimeKey, deviceArray) in devices) {
            val runtime = extractRuntimeName(runtimeKey)
            val deviceList = deviceArray as? JsonArray ?: continue
            for (deviceElement in deviceList) {
                val device = deviceElement.jsonObject
                val udid = device["udid"]?.jsonPrimitive?.contentOrNull ?: continue
                val name = device["name"]?.jsonPrimitive?.contentOrNull ?: continue
                val state = device["state"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                result.add(Simulator(udid, name, runtime, state))
            }
        }

        return result.sortedWith(compareBy({ it.runtime }, { it.name }))
    }

    private fun extractRuntimeName(runtimeKey: String): String {
        val match = RUNTIME_REGEX.find(runtimeKey) ?: return runtimeKey
        val parts = match.groupValues[1].split("-")
        if (parts.size < 2) return parts.joinToString("-")
        val platform = parts.first()
        val version = parts.drop(1).joinToString(".")
        return "$platform $version"
    }
}
