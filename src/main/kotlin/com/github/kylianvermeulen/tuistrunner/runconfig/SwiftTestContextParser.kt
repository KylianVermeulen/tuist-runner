package com.github.kylianvermeulen.tuistrunner.runconfig

/**
 * Pure Kotlin utility (no IntelliJ dependencies) that detects test context from Swift file text.
 * Identifies both XCTestCase subclasses (func test*) and Swift Testing (@Suite/@Test) declarations.
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

    // XCTestCase patterns
    private val XCTEST_CLASS_PATTERN = Regex("""class\s+(\w+)\s*:\s*[^{]*\bXCTestCase\b[^{]*\{""")
    private val XCTEST_METHOD_PATTERN = Regex("""\bfunc\s+(test\w+)\s*\(""")

    // Swift Testing patterns
    private val SUITE_ANNOTATION_PATTERN = Regex("""@Suite\b[^\n]*\n\s*(?:(?:final|public|internal|private|open|actor)\s+)*(?:struct|class|enum)\s+(\w+)""")
    private val STRUCT_CLASS_DECL_PATTERN = Regex("""(?:(?:final|public|internal|private|open|actor)\s+)*(?:struct|class|enum)\s+(\w+)\s*[^{]*\{""")
    private val TEST_ANNOTATION_FUNC_PATTERN = Regex("""@Test\b[^\n]*\n\s*(?:(?:public|internal|private|mutating)\s+)*(func\s+(\w+)\s*\()""")

    // General func pattern for determining method boundaries
    private val ANY_FUNC_PATTERN = Regex("""\bfunc\s+\w+\s*\(""")

    /**
     * Detect the test context at the given cursor offset within the file text.
     *
     * @return TestContext if the offset is inside a test container, null otherwise
     */
    fun detectTestContext(fileText: String, offset: Int): TestContext? {
        val allContainers = findAllContainerRanges(fileText)

        for ((className, classOffset, classEnd, testMethodFinder) in allContainers) {
            if (offset < classOffset || offset > classEnd) continue

            val classBody = fileText.substring(classOffset, classEnd)

            // Find all func declarations to determine method boundaries
            val allFuncOffsets = ANY_FUNC_PATTERN.findAll(classBody)
                .map { classOffset + it.range.first }
                .toList()

            // Find the test method whose scope contains the cursor
            for ((methodName, methodRelativeOffset) in testMethodFinder(classBody)) {
                val methodAbsoluteOffset = classOffset + methodRelativeOffset

                // The method scope ends at the next func declaration or class end
                val nextFuncOffset = allFuncOffsets.firstOrNull { it > methodAbsoluteOffset }
                val methodScopeEnd = nextFuncOffset ?: classEnd

                if (offset >= methodAbsoluteOffset && offset < methodScopeEnd) {
                    return TestContext(className, methodName, classOffset, methodAbsoluteOffset)
                }
            }

            // Cursor is inside the container but not inside any test method
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
        val allContainers = findAllContainerRanges(fileText)

        for ((className, classOffset, classEnd, testMethodFinder) in allContainers) {
            elements.add(TestElement.TestClass(className, classOffset))

            val classBody = fileText.substring(classOffset, classEnd)
            for ((methodName, methodRelativeOffset) in testMethodFinder(classBody)) {
                val methodAbsoluteOffset = classOffset + methodRelativeOffset
                elements.add(TestElement.TestMethod(className, methodName, methodAbsoluteOffset))
            }
        }

        return elements
    }

    private data class ContainerInfo(
        val className: String,
        val startOffset: Int,
        val endOffset: Int,
        val testMethodFinder: (String) -> List<Pair<String, Int>>,
    )

    /**
     * Find all test containers (XCTestCase subclasses and @Suite types) with their ranges
     * and appropriate method finders.
     */
    private fun findAllContainerRanges(fileText: String): List<ContainerInfo> {
        val containers = mutableListOf<ContainerInfo>()

        // XCTestCase subclasses: find func test*() methods
        for (match in XCTEST_CLASS_PATTERN.findAll(fileText)) {
            val className = match.groupValues[1]
            val classStart = match.range.first
            val classEnd = findMatchingBrace(fileText, match.range.last)
            containers.add(ContainerInfo(className, classStart, classEnd) { body ->
                XCTEST_METHOD_PATTERN.findAll(body).map { m ->
                    Pair(m.groupValues[1], m.range.first)
                }.toList()
            })
        }

        // @Suite annotated types: find @Test methods
        for (match in SUITE_ANNOTATION_PATTERN.findAll(fileText)) {
            val className = match.groupValues[1]
            val classStart = match.range.first

            // Find the opening brace of this type declaration
            val declStart = fileText.indexOf('{', match.range.last)
            if (declStart < 0) continue
            val classEnd = findMatchingBrace(fileText, declStart)

            // Skip if this range overlaps with an already-found XCTestCase container
            if (containers.any { it.startOffset == classStart || (classStart >= it.startOffset && classStart < it.endOffset) }) continue

            containers.add(ContainerInfo(className, classStart, classEnd, ::findSwiftTestingMethods))
        }

        // Standalone @Test methods in types that don't have @Suite annotation.
        // Look for any struct/class/enum that contains @Test methods but isn't already tracked.
        for (match in STRUCT_CLASS_DECL_PATTERN.findAll(fileText)) {
            val className = match.groupValues[1]
            val classStart = match.range.first

            // Skip if this struct/class falls within an already-tracked container
            if (containers.any { classStart >= it.startOffset && classStart < it.endOffset }) continue

            val bracePos = fileText.indexOf('{', match.range.first + match.value.length - 1)
            if (bracePos < 0) continue
            val classEnd = findMatchingBrace(fileText, bracePos)
            val classBody = fileText.substring(classStart, classEnd)

            // Only include if it contains @Test annotated methods
            val testMethods = findSwiftTestingMethods(classBody)
            if (testMethods.isNotEmpty()) {
                containers.add(ContainerInfo(className, classStart, classEnd, ::findSwiftTestingMethods))
            }
        }

        return containers.sortedBy { it.startOffset }
    }

    /**
     * Find @Test annotated methods within a class/struct body.
     * Returns pairs of (methodName, relativeOffset) where the offset points to the func keyword
     * (not the @Test annotation), so that method boundary detection works consistently.
     */
    private fun findSwiftTestingMethods(body: String): List<Pair<String, Int>> {
        return TEST_ANNOTATION_FUNC_PATTERN.findAll(body).map { m ->
            // group 2 = method name, group 1 = "func methodName(" -- use its start for offset
            val funcOffset = m.range.first + m.value.indexOf(m.groupValues[1])
            Pair(m.groupValues[2], funcOffset)
        }.toList()
    }

    /**
     * Find the position after the matching closing brace, starting from an opening brace.
     */
    private fun findMatchingBrace(fileText: String, openBracePos: Int): Int {
        var depth = 1
        var i = openBracePos + 1
        while (i < fileText.length && depth > 0) {
            when (fileText[i]) {
                '{' -> depth++
                '}' -> depth--
            }
            i++
        }
        return i
    }
}
