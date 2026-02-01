package com.github.kylianvermeulen.tuistrunner.execution

import com.intellij.execution.Executor
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.github.kylianvermeulen.tuistrunner.parser.XcodebuildTestEventsConverter
import com.github.kylianvermeulen.tuistrunner.runconfig.TuistTestRunConfiguration
import com.github.kylianvermeulen.tuistrunner.testlocator.TuistTestLocator

class TuistTestConsoleProperties(
    configuration: TuistTestRunConfiguration,
    executor: Executor,
) : SMTRunnerConsoleProperties(configuration, FRAMEWORK_NAME, executor),
    SMCustomMessagesParsing {

    companion object {
        const val FRAMEWORK_NAME = "TuistTest"
    }

    init {
        isIdBasedTestTree = false
    }

    override fun getTestLocator(): SMTestLocator = TuistTestLocator.INSTANCE

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: TestConsoleProperties,
    ): OutputToGeneralTestEventsConverter =
        XcodebuildTestEventsConverter(testFrameworkName, consoleProperties)
}
