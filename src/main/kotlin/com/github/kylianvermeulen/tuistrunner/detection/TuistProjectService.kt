package com.github.kylianvermeulen.tuistrunner.detection

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class TuistProjectService(private val project: Project) {

    private val LOG = logger<TuistProjectService>()

    var isTuistProject: Boolean = false
        private set

    var tuistExecutablePath: String? = null
        private set

    var availableSchemes: List<TuistScheme> = emptyList()
        private set

    fun detectProject() {
        val basePath = project.basePath ?: return
        isTuistProject = TuistProjectDetector.isTuistProject(basePath)
        if (!isTuistProject) return

        tuistExecutablePath = TuistProjectDetector.findTuistExecutable()
        if (tuistExecutablePath == null) {
            LOG.warn("Tuist project detected but CLI not found")
            return
        }

        refreshSchemes()
    }

    fun refreshSchemes() {
        val basePath = project.basePath ?: return
        val executable = tuistExecutablePath ?: return
        availableSchemes = TuistSchemeProvider.findTestSchemes(basePath, executable)
        LOG.info("Discovered ${availableSchemes.size} test schemes: ${availableSchemes.map { it.name }}")
    }
}
