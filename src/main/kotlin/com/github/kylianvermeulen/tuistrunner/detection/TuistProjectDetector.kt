package com.github.kylianvermeulen.tuistrunner.detection

import com.intellij.openapi.diagnostic.logger
import java.io.File

object TuistProjectDetector {

    private val LOG = logger<TuistProjectDetector>()

    fun isTuistProject(basePath: String): Boolean {
        val tuistDir = File(basePath, "Tuist")
        val projectSwift = File(basePath, "Project.swift")
        return tuistDir.isDirectory || projectSwift.isFile
    }

    fun findTuistExecutable(): String? {
        // Try `which tuist` first
        try {
            val process = ProcessBuilder("which", "tuist")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode == 0 && output.isNotEmpty()) {
                return output
            }
        } catch (e: Exception) {
            LOG.debug("'which tuist' failed", e)
        }

        // Fall back to common paths
        val commonPaths = listOf(
            "/usr/local/bin/tuist",
            "${System.getProperty("user.home")}/.local/bin/tuist",
            "/opt/homebrew/bin/tuist",
        )
        for (path in commonPaths) {
            if (File(path).canExecute()) {
                return path
            }
        }

        return null
    }
}
