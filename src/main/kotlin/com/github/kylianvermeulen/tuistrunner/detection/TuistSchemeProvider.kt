package com.github.kylianvermeulen.tuistrunner.detection

import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.json.*
import java.io.File

data class TuistScheme(
    val name: String,
    val testTargets: List<String> = emptyList(),
)

object TuistSchemeProvider {

    private val LOG = logger<TuistSchemeProvider>()

    private val TEST_SUFFIXES = listOf(
        "IntegrationTests",
        "SnapshotTests",
        "UITests",
        "Tests",
    )

    fun findTestSchemes(projectBasePath: String, tuistExecutable: String): List<TuistScheme> {
        val graphFile = File(projectBasePath, "graph.json")
        try {
            val process = ProcessBuilder(tuistExecutable, "graph", "-f", "json")
                .directory(File(projectBasePath))
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                LOG.warn("tuist graph exited with code $exitCode: $output")
                return emptyList()
            }

            if (!graphFile.exists()) {
                LOG.warn("tuist graph did not produce graph.json")
                return emptyList()
            }

            val jsonText = graphFile.readText()
            return parseGraphJson(jsonText)
        } catch (e: Exception) {
            LOG.warn("Failed to run tuist graph", e)
            return emptyList()
        } finally {
            graphFile.delete()
        }
    }

    fun parseGraphJson(json: String): List<TuistScheme> {
        val root = Json.parseToJsonElement(json).jsonObject
        val schemeMap = mutableMapOf<String, MutableSet<String>>()

        val projects = root["projects"]?.jsonObject ?: return emptyList()

        for ((_, projectElement) in projects) {
            val project = projectElement.jsonObject

            // Collect explicit schemes with test actions
            val projectSchemes = project["schemes"]?.jsonArray ?: JsonArray(emptyList())
            for (schemeElement in projectSchemes) {
                val scheme = schemeElement.jsonObject
                val testAction = scheme["testAction"]
                if (testAction != null && testAction !is JsonNull) {
                    val schemeName = scheme["name"]?.jsonPrimitive?.contentOrNull
                    if (schemeName != null) {
                        val testTargets = extractTestTargetsFromScheme(schemeElement)
                        schemeMap.getOrPut(schemeName) { mutableSetOf() }.addAll(testTargets)
                    }
                }
            }

            // Infer schemes from test targets
            val targets = project["targets"]?.jsonObject ?: continue
            for ((targetName, targetElement) in targets) {
                val target = targetElement.jsonObject
                val product = target["product"]?.jsonPrimitive?.contentOrNull
                if (product == "unitTests" || product == "uiTests") {
                    val inferredScheme = inferSchemeName(targetName)
                    if (inferredScheme != null) {
                        schemeMap.getOrPut(inferredScheme) { mutableSetOf() }.add(targetName)
                    }
                }
            }
        }

        return schemeMap.map { (name, targets) ->
            TuistScheme(name, targets.sorted())
        }.sortedBy { it.name }
    }

    private fun extractTestTargetsFromScheme(schemeElement: JsonElement): List<String> {
        val testAction = schemeElement.jsonObject["testAction"]?.jsonObject ?: return emptyList()
        val targets = testAction["targets"]?.jsonArray ?: return emptyList()
        return targets.mapNotNull { targetElement ->
            targetElement.jsonObject["target"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
        }
    }

    private fun inferSchemeName(testTargetName: String): String? {
        for (suffix in TEST_SUFFIXES) {
            if (testTargetName.endsWith(suffix) && testTargetName.length > suffix.length) {
                return testTargetName.removeSuffix(suffix)
            }
        }
        return null
    }
}
