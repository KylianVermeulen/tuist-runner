import Testing

struct NetworkTests {
    @Test
    func fetchData() {
        #expect(true)
    }

    @Test(arguments: [1, 2, 3])
    func parameterized(value: Int) {
        #expect(value > 0)
    }
}
