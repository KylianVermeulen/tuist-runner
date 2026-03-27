import XCTest
@testable import App

class CalculatorXCTests: XCTestCase {
    func testAddition() {
        let calculator = Calculator()
        XCTAssertEqual(calculator.add(2, 3), 5)
    }

    func testSubtraction() {
        let calculator = Calculator()
        XCTAssertEqual(calculator.subtract(5, 3), 2)
    }

    func helperSetup() {
        // Not a test -- should not get a gutter icon
    }
}
