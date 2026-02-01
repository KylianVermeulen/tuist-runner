package com.github.kylianvermeulen.tuistrunner.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.module.Module
import com.github.kylianvermeulen.tuistrunner.runconfig.TuistTestRunConfiguration

class TuistRerunFailedTestsAction(
    consoleView: ConsoleView,
    properties: TuistTestConsoleProperties,
) : AbstractRerunFailedTestsAction(consoleView) {

    init {
        init(properties)
    }

    override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile {
        val configuration = myConsoleProperties.configuration as TuistTestRunConfiguration
        val project = environment.project

        val failedTests = getFailedTests(project)
        val onlyTestingArgs = failedTests.mapNotNull { test ->
            val locationUrl = test.locationUrl ?: return@mapNotNull null
            // Location URL format: tuist-test://Target/ClassName/method
            val protocolSuffix = "://"
            val protocolIndex = locationUrl.indexOf(protocolSuffix)
            if (protocolIndex < 0) return@mapNotNull null
            locationUrl.substring(protocolIndex + protocolSuffix.length)
        }.distinct()

        return TuistRerunProfile(configuration, onlyTestingArgs)
    }

    private class TuistRerunProfile(
        private val originalConfig: TuistTestRunConfiguration,
        private val onlyTestingArgs: List<String>,
    ) : MyRunProfile(originalConfig) {

        override fun getModules(): Array<Module> = emptyArray()

        override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
            if (onlyTestingArgs.isEmpty()) {
                throw ExecutionException("No failed tests to rerun")
            }

            val rerunConfig = originalConfig.clone() as TuistTestRunConfiguration
            rerunConfig.testTarget = null
            rerunConfig.testClass = null
            rerunConfig.testMethod = null

            val existingArgs = rerunConfig.additionalArguments.trim()
            val onlyTestingFlags = onlyTestingArgs.joinToString(" ") { "-only-testing:$it" }
            rerunConfig.additionalArguments = if (existingArgs.isNotEmpty()) {
                "$existingArgs $onlyTestingFlags"
            } else {
                onlyTestingFlags
            }

            return TuistTestCommandLineState(environment, rerunConfig)
        }
    }
}
