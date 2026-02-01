package com.github.kylianvermeulen.tuistrunner.runconfig

import com.github.kylianvermeulen.tuistrunner.runconfig.SwiftTestContextParser.TestContext
import com.github.kylianvermeulen.tuistrunner.runconfig.SwiftTestContextParser.TestElement
import org.junit.Assert.*
import org.junit.Test

class SwiftTestContextParserTest {

    private fun loadFixture(name: String): String =
        javaClass.classLoader.getResource("fixtures/$name")!!.readText()

    // --- detectTestContext ---

    @Test
    fun `cursor inside test method returns className and methodName`() {
        val text = loadFixture("swift_test_class.swift")
        // Find the offset of "testExample" inside the file
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
        // helperMethod doesn't start with "test", so no method detected
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

    // --- findAllTestElements ---

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
}
