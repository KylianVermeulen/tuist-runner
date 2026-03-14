package com.github.kylianvermeulen.tuistrunner.detection

import org.junit.Assert.*
import org.junit.Test

class SimulatorProviderTest {

    private fun loadFixture(name: String): String =
        javaClass.classLoader.getResource("fixtures/$name")!!.readText()

    @Test
    fun `parseSimctlJson parses iOS simulators`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        val iPhoneNames = simulators.filter { it.runtime.startsWith("iOS") }.map { it.name }
        assertTrue("Should contain iPhone 16", "iPhone 16" in iPhoneNames)
        assertTrue("Should contain iPhone 16 Pro Max", "iPhone 16 Pro Max" in iPhoneNames)
        assertTrue("Should contain iPad Pro 13-inch (M4)", "iPad Pro 13-inch (M4)" in iPhoneNames)
    }

    @Test
    fun `parseSimctlJson parses tvOS simulators`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        val tvSimulators = simulators.filter { it.runtime.startsWith("tvOS") }
        assertEquals("Should have 1 tvOS simulator", 1, tvSimulators.size)
        assertEquals("Apple TV 4K (3rd generation)", tvSimulators.first().name)
    }

    @Test
    fun `parseSimctlJson parses visionOS simulators`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        val visionSimulators = simulators.filter { it.runtime.startsWith("visionOS") }
        assertEquals("Should have 1 visionOS simulator", 1, visionSimulators.size)
        assertEquals("Apple Vision Pro", visionSimulators.first().name)
    }

    @Test
    fun `parseSimctlJson skips empty runtime arrays`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        val watchSimulators = simulators.filter { it.runtime.startsWith("watchOS") }
        assertTrue("watchOS should have no simulators", watchSimulators.isEmpty())
    }

    @Test
    fun `parseSimctlJson extracts correct UDIDs`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        val iPhone16 = simulators.first { it.name == "iPhone 16" }
        assertEquals("A1B2C3D4-E5F6-7890-ABCD-EF1234567890", iPhone16.udid)
    }

    @Test
    fun `parseSimctlJson extracts state`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        val booted = simulators.first { it.name == "iPhone 16 Pro Max" }
        assertEquals("Booted", booted.state)

        val shutdown = simulators.first { it.name == "iPhone 16" }
        assertEquals("Shutdown", shutdown.state)
    }

    @Test
    fun `parseSimctlJson converts runtime keys to readable names`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        val runtimes = simulators.map { it.runtime }.toSet()
        assertTrue("Should contain iOS 18.2", "iOS 18.2" in runtimes)
        assertTrue("Should contain tvOS 18.2", "tvOS 18.2" in runtimes)
        assertTrue("Should contain visionOS 2.2", "visionOS 2.2" in runtimes)
    }

    @Test
    fun `parseSimctlJson returns sorted by runtime then name`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        val runtimeNamePairs = simulators.map { it.runtime to it.name }
        val expected = runtimeNamePairs.sortedWith(compareBy({ it.first }, { it.second }))
        assertEquals("Simulators should be sorted by runtime then name", expected, runtimeNamePairs)
    }

    @Test
    fun `parseSimctlJson returns empty list for empty devices`() {
        val json = """{"devices": {}}"""
        val simulators = SimulatorProvider.parseSimctlJson(json)
        assertTrue("Should return empty list", simulators.isEmpty())
    }

    @Test
    fun `parseSimctlJson returns empty list for missing devices key`() {
        val json = """{}"""
        val simulators = SimulatorProvider.parseSimctlJson(json)
        assertTrue("Should return empty list", simulators.isEmpty())
    }

    @Test
    fun `Simulator displayName combines name and runtime`() {
        val sim = Simulator("test-udid", "iPhone 16", "iOS 18.2", "Booted")
        assertEquals("iPhone 16 (iOS 18.2)", sim.displayName)
    }

    @Test
    fun `Simulator destinationString uses id format`() {
        val sim = Simulator("A1B2C3D4-E5F6-7890-ABCD-EF1234567890", "iPhone 16", "iOS 18.2", "Booted")
        assertEquals("id=A1B2C3D4-E5F6-7890-ABCD-EF1234567890", sim.destinationString)
    }

    @Test
    fun `parseSimctlJson total device count`() {
        val json = loadFixture("simctl_devices.json")
        val simulators = SimulatorProvider.parseSimctlJson(json)

        // 3 iOS + 1 tvOS + 0 watchOS + 1 visionOS = 5
        assertEquals("Should have 5 simulators total", 5, simulators.size)
    }
}
