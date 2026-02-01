package com.github.kylianvermeulen.tuistrunner.parser

import com.github.kylianvermeulen.tuistrunner.parser.XcodebuildOutputParser.TestEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class XcodebuildOutputParserTest {

    private lateinit var parser: XcodebuildOutputParser

    @Before
    fun setUp() {
        parser = XcodebuildOutputParser()
    }

    private fun loadFixture(name: String): List<String> =
        javaClass.classLoader.getResource("fixtures/$name")!!.readText().lines()

    private fun parseFixture(name: String): List<TestEvent> =
        loadFixture(name).mapNotNull { parser.processLine(it) }.flatten()

    // --- Suite filtering ---

    @Test
    fun `filters out AllTests suite`() {
        val result = parser.processLine("Test Suite 'AllTests' started at 2025-01-15 10:30:00.000")
        assertNotNull("Should match the pattern", result)
        assertTrue("Should produce no events (filtered)", result!!.isEmpty())
    }

    @Test
    fun `filters out xctest bundle suite`() {
        val result = parser.processLine("Test Suite 'MyAppTests.xctest' started at 2025-01-15 10:30:00.000")
        assertNotNull("Should match the pattern", result)
        assertTrue("Should produce no events (filtered)", result!!.isEmpty())
    }

    @Test
    fun `emits SuiteStarted for class-level suite`() {
        val result = parser.processLine("Test Suite 'FeatureTests' started at 2025-01-15 10:30:00.000")
        assertNotNull(result)
        assertEquals(1, result!!.size)
        val event = result[0] as TestEvent.SuiteStarted
        assertEquals("FeatureTests", event.name)
        assertEquals("tuist-suite://FeatureTests", event.locationHint)
    }

    @Test
    fun `emits SuiteFinished for class-level suite`() {
        val result = parser.processLine("Test Suite 'FeatureTests' passed at 2025-01-15 10:30:00.100.")
        assertNotNull(result)
        assertEquals(1, result!!.size)
        val event = result[0] as TestEvent.SuiteFinished
        assertEquals("FeatureTests", event.name)
    }

    @Test
    fun `handles failed suite finish`() {
        val result = parser.processLine("Test Suite 'FeatureTests' failed at 2025-01-15 10:30:00.100.")
        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertTrue(result[0] is TestEvent.SuiteFinished)
    }

    // --- Test case start ---

    @Test
    fun `emits TestStarted with correct name and location`() {
        val result = parser.processLine("Test Case '-[MyAppTests.FeatureTests testExample]' started.")
        assertNotNull(result)
        assertEquals(1, result!!.size)
        val event = result[0] as TestEvent.TestStarted
        assertEquals("testExample", event.name)
        assertEquals("FeatureTests", event.className)
        assertEquals("tuist-test://FeatureTests/testExample", event.locationHint)
    }

    @Test
    fun `extracts class name from qualified Obj-C identifier`() {
        val result = parser.processLine("Test Case '-[SomeModule.SubModule.MyTests testFoo]' started.")
        assertNotNull(result)
        val event = result!![0] as TestEvent.TestStarted
        assertEquals("MyTests", event.className)
        assertEquals("testFoo", event.name)
    }

    // --- Test case pass ---

    @Test
    fun `emits TestFinished for passing test with duration`() {
        parser.processLine("Test Case '-[MyAppTests.FeatureTests testExample]' started.")
        val result = parser.processLine("Test Case '-[MyAppTests.FeatureTests testExample]' passed (0.001 seconds).")
        assertNotNull(result)
        assertEquals(1, result!!.size)
        val event = result[0] as TestEvent.TestFinished
        assertEquals("testExample", event.name)
        assertEquals(1L, event.durationMs)
    }

    @Test
    fun `converts duration correctly`() {
        parser.processLine("Test Case '-[X.Y testA]' started.")
        val result = parser.processLine("Test Case '-[X.Y testA]' passed (0.042 seconds).")
        val event = result!![0] as TestEvent.TestFinished
        assertEquals(42L, event.durationMs)
    }

    @Test
    fun `converts large duration correctly`() {
        parser.processLine("Test Case '-[X.Y testSlow]' started.")
        val result = parser.processLine("Test Case '-[X.Y testSlow]' passed (12.345 seconds).")
        val event = result!![0] as TestEvent.TestFinished
        assertEquals(12345L, event.durationMs)
    }

    // --- Test case failure ---

    @Test
    fun `emits TestFailed and TestFinished for failing test`() {
        parser.processLine("Test Case '-[MyAppTests.FeatureTests testFailure]' started.")
        parser.processLine("/path/File.swift:42: error: -[MyAppTests.FeatureTests testFailure] : XCTAssertEqual failed: (\"a\") is not equal to (\"b\")")
        val result = parser.processLine("Test Case '-[MyAppTests.FeatureTests testFailure]' failed (0.005 seconds).")

        assertNotNull(result)
        assertEquals(2, result!!.size)

        val failEvent = result[0] as TestEvent.TestFailed
        assertEquals("testFailure", failEvent.name)
        assertEquals("XCTAssertEqual failed: (\"a\") is not equal to (\"b\")", failEvent.message)
        assertTrue(failEvent.details.contains("/path/File.swift:42"))
        assertEquals(5L, failEvent.durationMs)

        val finishEvent = result[1] as TestEvent.TestFinished
        assertEquals("testFailure", finishEvent.name)
        assertEquals(5L, finishEvent.durationMs)
    }

    @Test
    fun `accumulates multiple failure messages`() {
        parser.processLine("Test Case '-[X.Y testMulti]' started.")
        parser.processLine("/path/A.swift:10: error: -[X.Y testMulti] : XCTAssertTrue failed")
        parser.processLine("/path/A.swift:11: error: -[X.Y testMulti] : XCTAssertEqual failed: (\"1\") is not equal to (\"2\")")
        val result = parser.processLine("Test Case '-[X.Y testMulti]' failed (0.003 seconds).")

        val failEvent = result!![0] as TestEvent.TestFailed
        assertTrue("Should contain first failure", failEvent.message.contains("XCTAssertTrue failed"))
        assertTrue("Should contain second failure", failEvent.message.contains("XCTAssertEqual failed"))
        assertTrue("Details should contain both file locations", failEvent.details.contains("/path/A.swift:10"))
        assertTrue("Details should contain both file locations", failEvent.details.contains("/path/A.swift:11"))
    }

    @Test
    fun `uses default message when no failure details accumulated`() {
        parser.processLine("Test Case '-[X.Y testNoDetails]' started.")
        val result = parser.processLine("Test Case '-[X.Y testNoDetails]' failed (0.001 seconds).")

        val failEvent = result!![0] as TestEvent.TestFailed
        assertEquals("Test failed", failEvent.message)
    }

    @Test
    fun `failure messages are cleared between tests`() {
        parser.processLine("Test Case '-[X.Y testFirst]' started.")
        parser.processLine("/path/A.swift:10: error: -[X.Y testFirst] : first error")
        parser.processLine("Test Case '-[X.Y testFirst]' failed (0.001 seconds).")

        parser.processLine("Test Case '-[X.Y testSecond]' started.")
        val result = parser.processLine("Test Case '-[X.Y testSecond]' failed (0.001 seconds).")

        val failEvent = result!![0] as TestEvent.TestFailed
        assertEquals("Test failed", failEvent.message)
        assertFalse("Should not contain first test's failure", failEvent.message.contains("first error"))
    }

    // --- Failure message accumulation ---

    @Test
    fun `failure message line returns empty list`() {
        parser.processLine("Test Case '-[X.Y testFoo]' started.")
        val result = parser.processLine("/path/File.swift:42: error: -[X.Y testFoo] : assertion failed")
        assertNotNull("Should match the pattern", result)
        assertTrue("Should produce no events (accumulating)", result!!.isEmpty())
    }

    // --- Unmatched lines ---

    @Test
    fun `unmatched line returns null`() {
        val result = parser.processLine("Build settings from command line:")
        assertNull("Unmatched line should return null", result)
    }

    @Test
    fun `blank line returns null`() {
        val result = parser.processLine("")
        assertNull("Blank line should return null", result)
    }

    @Test
    fun `whitespace-only line returns null`() {
        val result = parser.processLine("   ")
        assertNull("Whitespace line should return null", result)
    }

    // --- Full fixture integration tests ---

    @Test
    fun `parses passing fixture into correct event sequence`() {
        val events = parseFixture("xcodebuild_output_passing.txt")

        val suiteStarted = events.filterIsInstance<TestEvent.SuiteStarted>()
        assertEquals("Should have 1 class suite started", 1, suiteStarted.size)
        assertEquals("FeatureTests", suiteStarted[0].name)

        val testStarted = events.filterIsInstance<TestEvent.TestStarted>()
        assertEquals("Should have 2 tests started", 2, testStarted.size)
        assertEquals("testExample", testStarted[0].name)
        assertEquals("testAnother", testStarted[1].name)

        val testFinished = events.filterIsInstance<TestEvent.TestFinished>()
        assertEquals("Should have 2 tests finished", 2, testFinished.size)

        val testFailed = events.filterIsInstance<TestEvent.TestFailed>()
        assertTrue("Should have no failures", testFailed.isEmpty())

        val suiteFinished = events.filterIsInstance<TestEvent.SuiteFinished>()
        assertEquals("Should have 1 class suite finished", 1, suiteFinished.size)
    }

    @Test
    fun `parses failing fixture into correct event sequence`() {
        val events = parseFixture("xcodebuild_output_failing.txt")

        val testFailed = events.filterIsInstance<TestEvent.TestFailed>()
        assertEquals("Should have 1 failure", 1, testFailed.size)
        assertEquals("testFailure", testFailed[0].name)
        assertTrue(testFailed[0].message.contains("XCTAssertEqual failed"))
    }

    @Test
    fun `parses mixed fixture with multiple suites`() {
        val events = parseFixture("xcodebuild_output_mixed.txt")

        val suiteStarted = events.filterIsInstance<TestEvent.SuiteStarted>()
        assertEquals("Should have 2 class suites", 2, suiteStarted.size)
        assertEquals("FeatureTests", suiteStarted[0].name)
        assertEquals("NetworkTests", suiteStarted[1].name)

        val testStarted = events.filterIsInstance<TestEvent.TestStarted>()
        assertEquals("Should have 3 tests", 3, testStarted.size)

        val testFailed = events.filterIsInstance<TestEvent.TestFailed>()
        assertEquals("Should have 1 failure", 1, testFailed.size)
        assertEquals("testFailing", testFailed[0].name)
        assertTrue("Should have 2 accumulated errors", testFailed[0].message.contains("XCTAssertTrue failed"))
        assertTrue("Should have 2 accumulated errors", testFailed[0].message.contains("XCTAssertEqual failed"))
    }
}
