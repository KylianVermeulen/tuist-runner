import Testing

@Suite
struct FeatureSuiteTests {
    @Test
    func addition() {
        #expect(1 + 1 == 2)
    }

    @Test("Subtraction works correctly")
    func subtraction() {
        #expect(5 - 3 == 2)
    }

    func helperMethod() {}
}
