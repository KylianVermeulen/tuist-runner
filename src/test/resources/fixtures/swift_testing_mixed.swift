import XCTest
import Testing

class LegacyTests: XCTestCase {
    func testOldStyle() {}
}

@Suite("Modern tests")
struct ModernTests {
    @Test
    func newStyle() {}
}
