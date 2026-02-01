package com.github.kylianvermeulen.tuistrunner.execution

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ProgramParametersConfigurator
import com.intellij.openapi.components.service
import com.github.kylianvermeulen.tuistrunner.detection.TuistProjectService
import com.github.kylianvermeulen.tuistrunner.runconfig.TuistTestRunConfiguration

class TuistTestCommandLineState(
    environment: ExecutionEnvironment,
    private val configuration: TuistTestRunConfiguration,
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val project = environment.project
        val service = project.service<TuistProjectService>()
        val tuistPath = service.tuistExecutablePath ?: "tuist"

        val commandLine = GeneralCommandLine().apply {
            exePath = tuistPath
            addParameter("xcodebuild")
            addParameter("test")
            addParameter("--scheme")
            addParameter(configuration.schemeName)

            val additionalArgs = configuration.additionalArguments.trim()
            if (additionalArgs.isNotEmpty()) {
                addParameters(ProgramParametersConfigurator.expandMacrosAndParseParameters(additionalArgs))
            }

            withWorkDirectory(project.basePath)
            withParentEnvironmentType(ParentEnvironmentType.CONSOLE)
        }

        val handler = ColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(handler)
        return handler
    }
}
