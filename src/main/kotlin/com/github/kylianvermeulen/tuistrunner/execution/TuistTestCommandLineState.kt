package com.github.kylianvermeulen.tuistrunner.execution

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.util.ProgramParametersConfigurator
import com.intellij.openapi.components.service
import com.github.kylianvermeulen.tuistrunner.detection.TuistProjectService
import com.github.kylianvermeulen.tuistrunner.runconfig.TuistTestRunConfiguration

class TuistTestCommandLineState(
    environment: ExecutionEnvironment,
    private val configuration: TuistTestRunConfiguration,
) : CommandLineState(environment) {

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler = startProcess()
        val properties = TuistTestConsoleProperties(configuration, executor)
        val console = SMTestRunnerConnectionUtil.createAndAttachConsole(
            TuistTestConsoleProperties.FRAMEWORK_NAME,
            processHandler,
            properties,
        )
        return DefaultExecutionResult(console, processHandler)
    }

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

            configuration.buildOnlyTestingArgument()?.let { onlyTesting ->
                addParameter("-only-testing:$onlyTesting")
            }

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
