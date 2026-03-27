package com.github.kylianvermeulen.tuistrunner.actions

import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.github.kylianvermeulen.tuistrunner.TuistBundle
import com.github.kylianvermeulen.tuistrunner.detection.TuistProjectService
import com.github.kylianvermeulen.tuistrunner.runconfig.TuistTestConfigurationFactory
import com.github.kylianvermeulen.tuistrunner.runconfig.TuistTestConfigurationType
import com.github.kylianvermeulen.tuistrunner.runconfig.TuistTestRunConfiguration

class TuistRunAllTestsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.service<TuistProjectService>()
        val schemes = service.availableSchemes
        if (schemes.isEmpty()) return

        if (schemes.size == 1) {
            runAllTests(e, schemes[0].name)
        } else {
            val schemeNames = schemes.map { it.name }
            JBPopupFactory.getInstance()
                .createPopupChooserBuilder(schemeNames)
                .setTitle(TuistBundle.message("action.runAllTests.chooseScheme"))
                .setItemChosenCallback { schemeName -> runAllTests(e, schemeName) }
                .createPopup()
                .showInBestPositionFor(e.dataContext)
        }
    }

    private fun runAllTests(e: AnActionEvent, schemeName: String) {
        val project = e.project ?: return
        val service = project.service<TuistProjectService>()
        val runManager = RunManager.getInstance(project)
        val type = ConfigurationTypeUtil.findConfigurationType(TuistTestConfigurationType::class.java)
        val factory = type.configurationFactories[0]

        val settings = runManager.createConfiguration(schemeName, factory)
        val configuration = settings.configuration as TuistTestRunConfiguration
        configuration.schemeName = schemeName
        configuration.testTarget = null
        configuration.testClass = null
        configuration.testMethod = null

        val simulators = service.availableSimulators
        val defaultSimulator = simulators.firstOrNull { it.state == "Booted" }
            ?: simulators.firstOrNull { it.runtime.startsWith("iOS") }
        configuration.destinationUdid = defaultSimulator?.udid

        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings

        val executor = ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID)
            ?: return
        ProgramRunnerUtil.executeConfiguration(settings, executor)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val visible = project != null && project.service<TuistProjectService>().isTuistProject
        e.presentation.isEnabledAndVisible = visible
    }
}
