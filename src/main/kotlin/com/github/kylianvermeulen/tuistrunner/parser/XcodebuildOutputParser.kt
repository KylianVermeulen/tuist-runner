package com.github.kylianvermeulen.tuistrunner.parser

/**
 * Stateful line-by-line parser for xcodebuild test output.
 * Pure Kotlin with no IntelliJ dependencies, making it easily unit-testable.
 */
class XcodebuildOutputParser {

    sealed class TestEvent {
        data class SuiteStarted(val name: String, val locationHint: String) : TestEvent()
        data class SuiteFinished(val name: String) : TestEvent()
        data class TestStarted(
            val name: String,
            val className: String,
            val targetName: String?,
            val locationHint: String,
        ) : TestEvent()
        data class TestFinished(val name: String, val durationMs: Long) : TestEvent()
        data class TestFailed(
            val name: String,
            val className: String,
            val targetName: String?,
            val message: String,
            val details: String,
            val durationMs: Long,
        ) : TestEvent()
    }

    private val pendingFailures = mutableListOf<FailureInfo>()
    private var currentTargetName: String? = null
    private var currentClassName: String? = null

    private data class FailureInfo(
        val filePath: String,
        val lineNumber: Int,
        val message: String,
    )

    companion object {
        private val SUITE_STARTED = Regex("""^\s*Test Suite '([^']+)' started at .+$""")
        private val SUITE_FINISHED = Regex("""^\s*Test Suite '([^']+)' (passed|failed) at .+\.$""")
        private val TEST_STARTED = Regex("""^\s*Test Case '-\[(\S+)\s+(\S+)\]' started\.$""")
        private val TEST_PASSED = Regex("""^\s*Test Case '-\[(\S+)\s+(\S+)\]' passed \((\d+\.\d+) seconds\)\.$""")
        private val TEST_FAILED = Regex("""^\s*Test Case '-\[(\S+)\s+(\S+)\]' failed \((\d+\.\d+) seconds\)\.$""")
        private val FAILURE_MESSAGE = Regex("""^(.+):(\d+): error: -\[(\S+)\s+(\S+)\] : (.+)$""")
    }

    /**
     * Process a single line of xcodebuild output.
     *
     * @return list of events to fire, or null if the line was not recognized
     *         (should be forwarded as uncaptured output). An empty list means
     *         the line was recognized but produced no events (e.g., filtered suite,
     *         accumulated failure message).
     */
    fun processLine(line: String): List<TestEvent>? {
        if (line.isBlank()) return null

        return handleSuiteStarted(line)
            ?: handleSuiteFinished(line)
            ?: handleTestStarted(line)
            ?: handleTestPassed(line)
            ?: handleTestFailed(line)
            ?: handleFailureMessage(line)
    }

    private fun handleSuiteStarted(line: String): List<TestEvent>? {
        val match = SUITE_STARTED.matchEntire(line) ?: return null
        val suiteName = match.groupValues[1]
        if (shouldSkipSuite(suiteName)) return emptyList()

        return listOf(TestEvent.SuiteStarted(suiteName, "tuist-suite://$suiteName"))
    }

    private fun handleSuiteFinished(line: String): List<TestEvent>? {
        val match = SUITE_FINISHED.matchEntire(line) ?: return null
        val suiteName = match.groupValues[1]
        if (shouldSkipSuite(suiteName)) return emptyList()

        return listOf(TestEvent.SuiteFinished(suiteName))
    }

    private fun handleTestStarted(line: String): List<TestEvent>? {
        val match = TEST_STARTED.matchEntire(line) ?: return null
        val (targetName, className) = extractTargetAndClass(match.groupValues[1])
        val methodName = match.groupValues[2]

        currentTargetName = targetName
        currentClassName = className
        pendingFailures.clear()

        val locationPath = if (targetName != null) "$targetName/$className/$methodName" else "$className/$methodName"
        return listOf(TestEvent.TestStarted(methodName, className, targetName, "tuist-test://$locationPath"))
    }

    private fun handleTestPassed(line: String): List<TestEvent>? {
        val match = TEST_PASSED.matchEntire(line) ?: return null
        val methodName = match.groupValues[2]
        val durationMs = parseDurationMs(match.groupValues[3])

        pendingFailures.clear()
        return listOf(TestEvent.TestFinished(methodName, durationMs))
    }

    private fun handleTestFailed(line: String): List<TestEvent>? {
        val match = TEST_FAILED.matchEntire(line) ?: return null
        val methodName = match.groupValues[2]
        val durationMs = parseDurationMs(match.groupValues[3])

        val failureMessage = if (pendingFailures.isNotEmpty()) {
            pendingFailures.joinToString("\n") { it.message }
        } else {
            "Test failed"
        }

        val failureDetails = if (pendingFailures.isNotEmpty()) {
            pendingFailures.joinToString("\n") { "${it.filePath}:${it.lineNumber}: ${it.message}" }
        } else {
            ""
        }

        val events = listOf(
            TestEvent.TestFailed(
                methodName,
                currentClassName ?: extractTargetAndClass(match.groupValues[1]).second,
                currentTargetName,
                failureMessage,
                failureDetails,
                durationMs,
            ),
            TestEvent.TestFinished(methodName, durationMs),
        )
        pendingFailures.clear()
        return events
    }

    private fun handleFailureMessage(line: String): List<TestEvent>? {
        val match = FAILURE_MESSAGE.matchEntire(line) ?: return null
        pendingFailures.add(
            FailureInfo(
                filePath = match.groupValues[1],
                lineNumber = match.groupValues[2].toIntOrNull() ?: 0,
                message = match.groupValues[5],
            )
        )
        return emptyList()
    }

    private fun extractTargetAndClass(qualifiedName: String): Pair<String?, String> {
        val dotIndex = qualifiedName.indexOf('.')
        return if (dotIndex >= 0) {
            val targetName = qualifiedName.substring(0, dotIndex)
            val className = qualifiedName.substring(dotIndex + 1).substringAfterLast('.')
            Pair(targetName, className)
        } else {
            Pair(null, qualifiedName)
        }
    }

    private fun parseDurationMs(seconds: String): Long =
        (seconds.toDouble() * 1000).toLong()

    private fun shouldSkipSuite(name: String): Boolean =
        name == "AllTests" || name.endsWith(".xctest")
}
