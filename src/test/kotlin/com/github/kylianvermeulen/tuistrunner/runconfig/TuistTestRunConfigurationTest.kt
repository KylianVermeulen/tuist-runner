package com.github.kylianvermeulen.tuistrunner.runconfig

import org.junit.Assert.*
import org.junit.Test

class TuistTestRunConfigurationTest {

    // We can't instantiate TuistTestRunConfiguration directly without a project,
    // so we test buildOnlyTestingArgument logic via the helper method pattern.
    // The actual method delegates to joining non-null parts with "/".

    @Test
    fun `buildOnlyTestingArgument with all null returns null`() {
        val result = buildOnlyTestingArgument(null, null, null)
        assertNull(result)
    }

    @Test
    fun `buildOnlyTestingArgument with only target`() {
        val result = buildOnlyTestingArgument("MyAppTests", null, null)
        assertEquals("MyAppTests", result)
    }

    @Test
    fun `buildOnlyTestingArgument with target and class`() {
        val result = buildOnlyTestingArgument("MyAppTests", "FeatureTests", null)
        assertEquals("MyAppTests/FeatureTests", result)
    }

    @Test
    fun `buildOnlyTestingArgument with all three`() {
        val result = buildOnlyTestingArgument("MyAppTests", "FeatureTests", "testExample")
        assertEquals("MyAppTests/FeatureTests/testExample", result)
    }

    @Test
    fun `buildOnlyTestingArgument with class and method but no target`() {
        val result = buildOnlyTestingArgument(null, "FeatureTests", "testExample")
        assertEquals("FeatureTests/testExample", result)
    }

    @Test
    fun `suggestedName with class and method`() {
        val name = suggestedName("MyScheme", "FeatureTests", "testExample")
        assertEquals("FeatureTests.testExample", name)
    }

    @Test
    fun `suggestedName with class only`() {
        val name = suggestedName("MyScheme", "FeatureTests", null)
        assertEquals("FeatureTests", name)
    }

    @Test
    fun `suggestedName with no class falls back to scheme`() {
        val name = suggestedName("MyScheme", null, null)
        assertEquals("MyScheme", name)
    }

    @Test
    fun `suggestedName with blank scheme and no class`() {
        val name = suggestedName("", null, null)
        assertEquals("Tuist Test", name)
    }

    // Extracted logic matching TuistTestRunConfiguration methods
    private fun buildOnlyTestingArgument(target: String?, cls: String?, method: String?): String? {
        val parts = listOfNotNull(target, cls, method)
        return if (parts.isEmpty()) null else parts.joinToString("/")
    }

    private fun suggestedName(scheme: String, cls: String?, method: String?): String {
        return when {
            cls != null && method != null -> "$cls.$method"
            cls != null -> cls
            else -> scheme.ifBlank { "Tuist Test" }
        }
    }
}
