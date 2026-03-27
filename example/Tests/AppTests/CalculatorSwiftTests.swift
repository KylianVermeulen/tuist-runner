import Testing
@testable import App

@Suite("Calculator Suite")
struct CalculatorSwiftTests {
    @Test
    func addition() {
        let calculator = Calculator()
        #expect(calculator.add(2, 3) == 5)
    }

    @Test("Subtraction works correctly")
    func subtraction() {
        let calculator = Calculator()
        #expect(calculator.subtract(5, 3) == 2)
    }

    func helperMethod() {
        // Not a test
    }
}

struct MultiplicationTests {
    @Test
    func basicMultiplication() {
        let calculator = Calculator()
        #expect(calculator.multiply(3, 4) == 12)
    }

    @Test(arguments: [1, 2, 3, 4, 5])
    func multiplyByZero(value: Int) {
        let calculator = Calculator()
        #expect(calculator.multiply(value, 0) == 0)
    }
}
