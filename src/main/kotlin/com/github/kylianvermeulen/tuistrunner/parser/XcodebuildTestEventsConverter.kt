package com.github.kylianvermeulen.tuistrunner.parser

import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.events.*
import com.intellij.openapi.util.Key

class XcodebuildTestEventsConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties,
) : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    private val parser = XcodebuildOutputParser()

    override fun processConsistentText(text: String, outputType: Key<*>) {
        val trimmed = text.trimEnd('\n', '\r')
        val events = parser.processLine(trimmed)

        if (events == null) {
            fireOnUncapturedOutput(text, outputType)
            return
        }

        val processor = getProcessor() ?: return

        for (event in events) {
            when (event) {
                is XcodebuildOutputParser.TestEvent.SuiteStarted ->
                    processor.onSuiteStarted(TestSuiteStartedEvent(event.name, event.locationHint))

                is XcodebuildOutputParser.TestEvent.SuiteFinished ->
                    processor.onSuiteFinished(TestSuiteFinishedEvent(event.name))

                is XcodebuildOutputParser.TestEvent.TestStarted ->
                    processor.onTestStarted(TestStartedEvent(event.name, event.locationHint))

                is XcodebuildOutputParser.TestEvent.TestFinished ->
                    processor.onTestFinished(TestFinishedEvent(event.name, event.durationMs))

                is XcodebuildOutputParser.TestEvent.TestFailed ->
                    processor.onTestFailure(TestFailedEvent(event.name, event.message, event.details, false, null, null))
            }
        }
    }
}
