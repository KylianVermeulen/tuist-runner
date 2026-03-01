package com.github.kylianvermeulen.tuistrunner.runconfig

import com.github.kylianvermeulen.tuistrunner.runconfig.SwiftTestContextParser.TestContext
import com.github.kylianvermeulen.tuistrunner.runconfig.SwiftTestContextParser.TestElement
import org.junit.Assert.*
import org.junit.Test

class SwiftTestContextParserTest {

    private fun loadFixture(name: String): String =
        javaClass.classLoader.getResource("fixtures/$name")!!.readText()

    // --- XCTestCase: detectTestContext ---

    @Test
    fun `cursor inside test method returns className and methodName`() {
        val text = loadFixture("swift_test_class.swift")
        val offset = text.indexOf("func testExample")
        assertTrue("Fixture should contain testExample", offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("FeatureTests", context!!.className)
        assertEquals("testExample", context.methodName)
    }

    @Test
    fun `cursor on class declaration returns className without methodName`() {
        val text = loadFixture("swift_test_class.swift")
        val offset = text.indexOf("class FeatureTests")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("FeatureTests", context!!.className)
        assertNull(context.methodName)
    }

    @Test
    fun `cursor in non-test class returns null`() {
        val text = loadFixture("swift_no_tests.swift")
        val offset = text.indexOf("func doSomething")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNull(context)
    }

    @Test
    fun `cursor in helper method returns className only without method`() {
        val text = loadFixture("swift_test_class.swift")
        val offset = text.indexOf("func helperMethod")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("FeatureTests", context!!.className)
        assertNull(context.methodName)
    }

    @Test
    fun `multiple XCTestCase subclasses detects correct class`() {
        val text = loadFixture("swift_multiple_classes.swift")
        val offset = text.indexOf("func testB")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("SecondTests", context!!.className)
        assertEquals("testB", context.methodName)
    }

    @Test
    fun `cursor in first class of multiple classes`() {
        val text = loadFixture("swift_multiple_classes.swift")
        val offset = text.indexOf("func testA")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("FirstTests", context!!.className)
        assertEquals("testA", context.methodName)
    }

    @Test
    fun `cursor outside any class returns null`() {
        val text = loadFixture("swift_multiple_classes.swift")
        val offset = text.indexOf("import XCTest")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNull(context)
    }

    @Test
    fun `class with multiple inheritance`() {
        val text = "class Foo: XCTestCase, SomeProtocol {\n    func testBar() {}\n}\n"
        val offset = text.indexOf("func testBar")

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("Foo", context!!.className)
        assertEquals("testBar", context.methodName)
    }

    // --- XCTestCase: findAllTestElements ---

    @Test
    fun `findAllTestElements returns all classes and methods`() {
        val text = loadFixture("swift_test_class.swift")
        val elements = SwiftTestContextParser.findAllTestElements(text)

        val classes = elements.filterIsInstance<TestElement.TestClass>()
        assertEquals(1, classes.size)
        assertEquals("FeatureTests", classes[0].className)

        val methods = elements.filterIsInstance<TestElement.TestMethod>()
        assertEquals(2, methods.size)
        assertEquals("testExample", methods[0].methodName)
        assertEquals("FeatureTests", methods[0].className)
        assertEquals("testAnother", methods[1].methodName)
        assertEquals("FeatureTests", methods[1].className)
    }

    @Test
    fun `findAllTestElements with multiple classes`() {
        val text = loadFixture("swift_multiple_classes.swift")
        val elements = SwiftTestContextParser.findAllTestElements(text)

        val classes = elements.filterIsInstance<TestElement.TestClass>()
        assertEquals(2, classes.size)
        assertEquals("FirstTests", classes[0].className)
        assertEquals("SecondTests", classes[1].className)

        val methods = elements.filterIsInstance<TestElement.TestMethod>()
        assertEquals(3, methods.size)
        assertEquals("testA", methods[0].methodName)
        assertEquals("FirstTests", methods[0].className)
        assertEquals("testB", methods[1].methodName)
        assertEquals("SecondTests", methods[1].className)
        assertEquals("testC", methods[2].methodName)
        assertEquals("SecondTests", methods[2].className)
    }

    @Test
    fun `findAllTestElements returns empty for non-test file`() {
        val text = loadFixture("swift_no_tests.swift")
        val elements = SwiftTestContextParser.findAllTestElements(text)
        assertTrue(elements.isEmpty())
    }

    @Test
    fun `findAllTestElements offsets are correct`() {
        val text = loadFixture("swift_test_class.swift")
        val elements = SwiftTestContextParser.findAllTestElements(text)

        val classElement = elements.filterIsInstance<TestElement.TestClass>().first()
        assertEquals(text.indexOf("class FeatureTests"), classElement.offset)

        val firstMethod = elements.filterIsInstance<TestElement.TestMethod>().first()
        assertEquals(text.indexOf("func testExample"), firstMethod.offset)
    }

    // --- Swift Testing (@Suite / @Test): detectTestContext ---

    @Test
    fun `detects @Suite struct as test container`() {
        val text = loadFixture("swift_testing_suite.swift")
        val offset = text.indexOf("@Suite")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("FeatureSuiteTests", context!!.className)
        assertNull(context.methodName)
    }

    @Test
    fun `cursor on @Test method in @Suite struct returns method name`() {
        val text = loadFixture("swift_testing_suite.swift")
        val offset = text.indexOf("func addition")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("FeatureSuiteTests", context!!.className)
        assertEquals("addition", context.methodName)
    }

    @Test
    fun `detects @Test method with display name argument`() {
        val text = loadFixture("swift_testing_suite.swift")
        val offset = text.indexOf("func subtraction")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("FeatureSuiteTests", context!!.className)
        assertEquals("subtraction", context.methodName)
    }

    @Test
    fun `helper method in @Suite struct returns class only`() {
        val text = loadFixture("swift_testing_suite.swift")
        val offset = text.indexOf("func helperMethod")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("FeatureSuiteTests", context!!.className)
        assertNull(context.methodName)
    }

    @Test
    fun `detects @Test methods in struct without @Suite annotation`() {
        val text = loadFixture("swift_testing_no_suite.swift")
        val offset = text.indexOf("func fetchData")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("NetworkTests", context!!.className)
        assertEquals("fetchData", context.methodName)
    }

    @Test
    fun `detects @Test with arguments parameter`() {
        val text = loadFixture("swift_testing_no_suite.swift")
        val offset = text.indexOf("func parameterized")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("NetworkTests", context!!.className)
        assertEquals("parameterized", context.methodName)
    }

    @Test
    fun `cursor outside @Test struct with no @Suite returns null`() {
        val text = loadFixture("swift_testing_no_suite.swift")
        val offset = text.indexOf("import Testing")
        assertTrue(offset >= 0)

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNull(context)
    }

    // --- Swift Testing: findAllTestElements ---

    @Test
    fun `findAllTestElements detects @Suite struct and @Test methods`() {
        val text = loadFixture("swift_testing_suite.swift")
        val elements = SwiftTestContextParser.findAllTestElements(text)

        val classes = elements.filterIsInstance<TestElement.TestClass>()
        assertEquals(1, classes.size)
        assertEquals("FeatureSuiteTests", classes[0].className)

        val methods = elements.filterIsInstance<TestElement.TestMethod>()
        assertEquals(2, methods.size)
        assertEquals("addition", methods[0].methodName)
        assertEquals("subtraction", methods[1].methodName)
    }

    @Test
    fun `findAllTestElements detects @Test methods without @Suite`() {
        val text = loadFixture("swift_testing_no_suite.swift")
        val elements = SwiftTestContextParser.findAllTestElements(text)

        val classes = elements.filterIsInstance<TestElement.TestClass>()
        assertEquals(1, classes.size)
        assertEquals("NetworkTests", classes[0].className)

        val methods = elements.filterIsInstance<TestElement.TestMethod>()
        assertEquals(2, methods.size)
        assertEquals("fetchData", methods[0].methodName)
        assertEquals("parameterized", methods[1].methodName)
    }

    // --- Mixed XCTestCase + Swift Testing ---

    @Test
    fun `findAllTestElements handles mixed XCTestCase and Swift Testing`() {
        val text = loadFixture("swift_testing_mixed.swift")
        val elements = SwiftTestContextParser.findAllTestElements(text)

        val classes = elements.filterIsInstance<TestElement.TestClass>()
        assertEquals(2, classes.size)
        assertEquals("LegacyTests", classes[0].className)
        assertEquals("ModernTests", classes[1].className)

        val methods = elements.filterIsInstance<TestElement.TestMethod>()
        assertEquals(2, methods.size)
        assertEquals("testOldStyle", methods[0].methodName)
        assertEquals("LegacyTests", methods[0].className)
        assertEquals("newStyle", methods[1].methodName)
        assertEquals("ModernTests", methods[1].className)
    }

    @Test
    fun `detectTestContext in XCTest class of mixed file`() {
        val text = loadFixture("swift_testing_mixed.swift")
        val offset = text.indexOf("func testOldStyle")

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("LegacyTests", context!!.className)
        assertEquals("testOldStyle", context.methodName)
    }

    @Test
    fun `detectTestContext in @Suite struct of mixed file`() {
        val text = loadFixture("swift_testing_mixed.swift")
        val offset = text.indexOf("func newStyle")

        val context = SwiftTestContextParser.detectTestContext(text, offset)
        assertNotNull(context)
        assertEquals("ModernTests", context!!.className)
        assertEquals("newStyle", context.methodName)
    }
}
