package com.github.kylianvermeulen.tuistrunner.runconfig

/**
 * Pure Kotlin utility (no IntelliJ dependencies) that detects test context from Swift file text.
 * Identifies XCTestCase subclasses and test methods for run configuration producers and gutter icons.
 */
object SwiftTestContextParser {

    data class TestContext(
        val className: String,
        val methodName: String?,
        val classOffset: Int,
        val methodOffset: Int?,
    )

    sealed class TestElement {
        data class TestClass(val className: String, val offset: Int) : TestElement()
        data class TestMethod(val className: String, val methodName: String, val offset: Int) : TestElement()
    }

    private val CLASS_PATTERN = Regex("""class\s+(\w+)\s*:\s*[^{]*\bXCTestCase\b[^{]*\{""")
    private val TEST_METHOD_PATTERN = Regex("""\bfunc\s+(test\w+)\s*\(""")
    private val ANY_FUNC_PATTERN = Regex("""\bfunc\s+\w+\s*\(""")

    /**
     * Detect the test context at the given cursor offset within the file text.
     *
     * @return TestContext if the offset is inside an XCTestCase subclass, null otherwise
     */
    fun detectTestContext(fileText: String, offset: Int): TestContext? {
        val classRanges = findClassRanges(fileText)

        for ((className, classOffset, classEnd) in classRanges) {
            if (offset < classOffset || offset > classEnd) continue

            val classBody = fileText.substring(classOffset, classEnd)

            // Find all func declarations to determine method boundaries
            val allFuncOffsets = ANY_FUNC_PATTERN.findAll(classBody)
                .map { classOffset + it.range.first }
                .toList()

            // Find the test method whose scope contains the cursor
            for (match in TEST_METHOD_PATTERN.findAll(classBody)) {
                val methodAbsoluteOffset = classOffset + match.range.first
                val methodName = match.groupValues[1]

                // The method scope ends at the next func declaration or class end
                val nextFuncOffset = allFuncOffsets.firstOrNull { it > methodAbsoluteOffset }
                val methodScopeEnd = nextFuncOffset ?: classEnd

                if (offset >= methodAbsoluteOffset && offset < methodScopeEnd) {
                    return TestContext(className, methodName, classOffset, methodAbsoluteOffset)
                }
            }

            // Cursor is inside the class but not inside any test method
            return TestContext(className, null, classOffset, null)
        }

        return null
    }

    /**
     * Find all test elements (classes and methods) in the file text.
     * Used by the line marker contributor for gutter icon placement.
     */
    fun findAllTestElements(fileText: String): List<TestElement> {
        val elements = mutableListOf<TestElement>()
        val classRanges = findClassRanges(fileText)

        for ((className, classOffset, classEnd) in classRanges) {
            elements.add(TestElement.TestClass(className, classOffset))

            val classBody = fileText.substring(classOffset, classEnd)
            for (match in TEST_METHOD_PATTERN.findAll(classBody)) {
                val methodName = match.groupValues[1]
                val methodAbsoluteOffset = classOffset + match.range.first
                elements.add(TestElement.TestMethod(className, methodName, methodAbsoluteOffset))
            }
        }

        return elements
    }

    /**
     * Find ranges of XCTestCase subclasses: (className, startOffset, endOffset).
     * Uses brace counting to determine where each class body ends.
     */
    private fun findClassRanges(fileText: String): List<Triple<String, Int, Int>> {
        val ranges = mutableListOf<Triple<String, Int, Int>>()

        for (match in CLASS_PATTERN.findAll(fileText)) {
            val className = match.groupValues[1]
            val classStart = match.range.first
            val braceStart = match.range.last // position of the opening '{'

            var depth = 1
            var i = braceStart + 1
            while (i < fileText.length && depth > 0) {
                when (fileText[i]) {
                    '{' -> depth++
                    '}' -> depth--
                }
                i++
            }

            ranges.add(Triple(className, classStart, i))
        }

        return ranges
    }
}
