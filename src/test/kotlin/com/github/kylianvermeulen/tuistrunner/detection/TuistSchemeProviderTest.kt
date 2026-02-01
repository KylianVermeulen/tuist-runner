package com.github.kylianvermeulen.tuistrunner.detection

import org.junit.Assert.*
import org.junit.Test

class TuistSchemeProviderTest {

    private fun loadFixture(name: String): String =
        javaClass.classLoader.getResource("fixtures/$name")!!.readText()

    @Test
    fun `parseGraphJson discovers schemes from test targets`() {
        val json = loadFixture("graph_with_test_targets.json")
        val schemes = TuistSchemeProvider.parseGraphJson(json)

        val schemeNames = schemes.map { it.name }
        // CoreTests -> Core, CoreIntegrationTests -> Core (merged), LiftixTests -> Liftix, LiftixUITests -> Liftix (merged)
        assertTrue("Should contain Core scheme", "Core" in schemeNames)
        assertTrue("Should contain Liftix scheme", "Liftix" in schemeNames)
        assertEquals("Should have exactly 2 schemes", 2, schemes.size)

        val coreScheme = schemes.first { it.name == "Core" }
        assertTrue("Core should include CoreTests", "CoreTests" in coreScheme.testTargets)
        assertTrue("Core should include CoreIntegrationTests", "CoreIntegrationTests" in coreScheme.testTargets)
    }

    @Test
    fun `parseGraphJson discovers explicit schemes with test actions`() {
        val json = loadFixture("graph_with_explicit_schemes.json")
        val schemes = TuistSchemeProvider.parseGraphJson(json)

        val schemeNames = schemes.map { it.name }
        assertTrue("Should contain App scheme from explicit schemes", "App" in schemeNames)
        assertFalse("Should not contain AppRelease (no testAction)", "AppRelease" in schemeNames)
    }

    @Test
    fun `parseGraphJson extracts test targets from explicit schemes`() {
        val json = loadFixture("graph_with_explicit_schemes.json")
        val schemes = TuistSchemeProvider.parseGraphJson(json)

        val appScheme = schemes.first { it.name == "App" }
        assertTrue("Should have AppTests target", "AppTests" in appScheme.testTargets)
        assertTrue("Should have AppUITests target", "AppUITests" in appScheme.testTargets)
    }

    @Test
    fun `parseGraphJson returns empty list for project with no test targets`() {
        val json = loadFixture("graph_empty.json")
        val schemes = TuistSchemeProvider.parseGraphJson(json)

        assertTrue("Should return empty list", schemes.isEmpty())
    }

    @Test
    fun `parseGraphJson deduplicates schemes`() {
        val json = loadFixture("graph_with_explicit_schemes.json")
        val schemes = TuistSchemeProvider.parseGraphJson(json)

        // "App" appears both as explicit scheme and inferred from AppTests target
        val appCount = schemes.count { it.name == "App" }
        assertEquals("App scheme should appear only once", 1, appCount)
    }

    @Test
    fun `parseGraphJson returns sorted results`() {
        val json = loadFixture("graph_with_test_targets.json")
        val schemes = TuistSchemeProvider.parseGraphJson(json)

        val names = schemes.map { it.name }
        assertEquals("Schemes should be sorted", names.sorted(), names)
    }
}
