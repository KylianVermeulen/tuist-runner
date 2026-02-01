# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an IntelliJ Platform plugin project for tuist-runner, built using the IntelliJ Platform Plugin Template. The plugin is developed in Kotlin and targets IntelliJ IDEA 2025.2.5+.

**Package structure**: `com.github.kylianvermeulen.tuistrunner`

## Build & Development Commands

### Building
- `./gradlew buildPlugin` - Build the plugin and create ZIP distribution
- `./gradlew buildSearchableOptions` - Build UI component index
- `./gradlew composedJar` - Create final JAR with instrumented classes

### Testing
- `./gradlew test` - Run test suite
- `./gradlew check` - Run all checks (tests + verification)
- `./gradlew runIdeForUiTests` - Launch IDE for UI testing with Robot Server plugin

### Code Quality
- `./gradlew verifyPlugin` - Run plugin structure verification and IntelliJ Plugin Verifier
- `./gradlew koverXmlReport` - Generate XML coverage report (used by CI)
- `./gradlew koverHtmlReport` - Generate HTML coverage report

### Running the Plugin
- `./gradlew runIde` - Launch IDE with plugin installed in sandbox environment

### Publishing
- `./gradlew publishPlugin` - Publish to JetBrains Marketplace (requires PUBLISH_TOKEN)
- `./gradlew patchChangelog` - Update changelog after release

## Project Configuration

### Key Files
- **gradle.properties**: Plugin metadata (group, name, version, platform version)
  - Current platform: IntelliJ IDEA 2025.2.5
  - Since build: 252
  - JVM: Java 21
- **plugin.xml**: Plugin configuration at `src/main/resources/META-INF/plugin.xml`
  - Plugin ID: `com.github.kylianvermeulen.tuistrunner`
  - Dependencies and extension points defined here

### Plugin Description
The plugin description in README.md between `<!-- Plugin description -->` and `<!-- Plugin description end -->` comments is automatically extracted and injected into plugin.xml during build.

## Architecture

### Core Components
1. **Tool Window** (`MyToolWindowFactory`): Creates custom tool window UI
2. **Project Service** (`MyProjectService`): Project-scoped service for business logic
3. **Startup Activity** (`MyProjectActivity`): Executes on project startup
4. **Resource Bundle** (`MyBundle`): Internationalization support via `messages.MyBundle`

### Extension Points
- Tool window registered in plugin.xml with ID "MyToolWindow"
- Post-startup activity for initialization tasks

## CI/CD Pipeline

### GitHub Actions Workflows
- **Build** (build.yml): Runs on push to main and all PRs
  - Build plugin
  - Run tests with code coverage (Kover → CodeCov)
  - Qodana code inspections
  - Plugin verification
  - Creates draft release on main branch

- **Release** (release.yml): Triggered when draft release is published
  - Publishes to JetBrains Marketplace
  - Updates changelog

- **UI Tests** (run-ui-tests.yml): Automated UI testing

### Required Secrets
- `CODECOV_TOKEN`: For coverage reports
- `PUBLISH_TOKEN`: For JetBrains Marketplace publishing
- `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`: For plugin signing

## Development Notes

### Gradle Configuration
- Uses Gradle 9.2.1 with configuration cache enabled
- Build cache enabled for faster builds
- Kotlin stdlib not bundled (opt-out flag set)
- Uses Gradle version catalog for dependency management

### Plugin Verification
The `verifyPlugin` task runs the IntelliJ Plugin Verifier against recommended IDE versions to ensure compatibility. Results are saved to `build/reports/pluginVerifier`.

### Sandbox Environment
- `prepareSandbox` creates an isolated IDE environment with the plugin installed
- Separate sandbox environments for different run configurations (IDE, tests, UI tests)

### Release Process
1. Update version in gradle.properties
2. Update CHANGELOG.md with new version
3. Push to main - CI creates draft release
4. Review and publish draft release - triggers marketplace publication