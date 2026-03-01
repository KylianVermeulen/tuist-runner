<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# tuist-runner Changelog

## [Unreleased]

## [0.0.1]
### Added
- Automatic Tuist project detection (looks for `Tuist/` directory or `Project.swift`)
- Tuist CLI discovery with notification when CLI is not found
- Custom run configuration type for running tests via `tuist xcodebuild test`
- Scheme discovery from Tuist dependency graph (`tuist graph -f json`)
- Simulator selection and destination support for test runs
- Gutter run icons on XCTest (`func test*()`) and Swift Testing (`@Suite`/`@Test`) declarations
- Context-aware run configuration producer (right-click or cursor-based)
- Xcodebuild output parser with support for both XCTest and Swift Testing output formats
- Test result integration with IntelliJ's built-in test runner UI
- Clickable test results with source navigation via custom `tuist-suite://` and `tuist-test://` protocols
- Rerun failed tests action
- Configuration editor with scheme, simulator, and additional arguments fields
