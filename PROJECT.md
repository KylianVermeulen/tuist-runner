# PROJECT.md — Tuist Test Execution Plugin

## Vision

An IntelliJ Platform plugin that enables iOS/macOS developers to discover, run, and debug tests in [Tuist](https://tuist.io/) projects directly from JetBrains IDEs — without requiring Xcode.

The plugin wraps `tuist xcodebuild test` and maps its output into IntelliJ's native test runner UI, providing gutter run icons, a test tree, click-through to source, re-run failed tests, and granular execution at the scheme/target/class/method level.

---

## Scope

### In Scope

- Detect Tuist projects (presence of `Tuist/` directory or `Project.swift`)
- Discover test schemes and targets via `tuist graph --format json`
- Execute tests via `tuist xcodebuild test` with scheme, target, class, and method granularity
- Parse `xcodebuild` test output into IntelliJ's test runner tree (SMTestRunner)
- Gutter icons on Swift test functions/classes for inline execution
- Test results panel with pass/fail/skip/duration
- Click-through from test results to source locations
- Re-run failed tests
- Run configuration UI (scheme, target, device/simulator, extra args)
- Simulator/device selection

### Out of Scope (for now)

- Swift language support (syntax highlighting, code completion, navigation) — users pair this with existing Swift plugins
- Code coverage visualization
- Test plan support
- Debugging (LLDB integration)
- Tuist project generation (`tuist generate`)
- Non-test Tuist commands (build, run, clean, etc.)

---

## Architecture

### Package Structure

All source code lives under `com.github.kylianvermeulen.tuistrunner`.

```
com.github.kylianvermeulen.tuistrunner
├── detection/          # Tuist project detection and scheme discovery
├── execution/          # Run configurations, command-line state, process handling
├── parser/             # xcodebuild output parsing → SMTestRunner events
├── runconfig/          # Run configuration type, factory, editor UI, options
├── testlocator/        # Mapping test identifiers back to source PSI elements
├── linemarker/         # Gutter icon provider for Swift test functions
├── toolwindow/         # (future) Custom tool window for Tuist-specific UI
└── TuistBundle.kt      # i18n resource bundle
```

### Core Components

#### 1. Project Detection (`detection/`)

**`TuistProjectDetector`** — Determines whether the opened project is a Tuist project.

- Checks for `Tuist/` directory or `Project.swift` at the project root
- Optionally verifies `tuist` binary is available on PATH
- Provides `findTestSchemes(projectRoot): List<TuistScheme>` by executing `tuist graph --format json` and parsing the dependency graph for test targets

#### 2. Run Configuration (`runconfig/`)

**`TuistTestConfigurationType`** — Registers a new "Tuist Test" run configuration type in the IDE.

**`TuistTestRunConfiguration`** — Stores execution parameters:

```kotlin
data class TuistTestConfigurationOptions(
    var projectPath: String = "",
    var schemeName: String = "",
    var testTarget: String? = null,
    var testClass: String? = null,
    var testMethod: String? = null,
    var additionalArguments: String = "",
    var deviceId: String? = null,
    var platform: TuistPlatform = TuistPlatform.MACOS
)
```

**`TuistTestConfigurationEditor`** — Swing/Kotlin UI DSL form for editing run configuration options (scheme dropdown, target, device selector, extra args).

**`TuistTestRunConfigurationProducer`** — Automatically creates run configurations from context (right-click on test file, gutter icon click, etc.).

#### 3. Test Execution (`execution/`)

**`TuistTestCommandLineState`** — Builds and launches the `tuist xcodebuild test` process:

```bash
# Full scheme
tuist xcodebuild test --scheme MyApp

# Specific test class
tuist xcodebuild test --scheme MyApp -only-testing:MyAppTests/FeatureTests

# Specific test method
tuist xcodebuild test --scheme MyApp -only-testing:MyAppTests/FeatureTests/testExample
```

**`TuistTestProcessHandler`** — Wraps `OSProcessHandler` for the tuist process, wiring stdout/stderr to the output parser.

#### 4. Output Parsing (`parser/`)

**`XcodebuildTestOutputParser`** — Parses xcodebuild's test output format and emits ServiceMessage events for IntelliJ's SMTestRunner:

```
Test Suite 'FeatureTests' started at 2025-01-15 10:30:00.000.
Test Case '-[MyAppTests.FeatureTests testExample]' started.
Test Case '-[MyAppTests.FeatureTests testExample]' passed (0.001 seconds).
Test Suite 'FeatureTests' passed at 2025-01-15 10:30:00.001.
```

Maps to:
- `testSuiteStarted` / `testSuiteFinished`
- `testStarted` / `testFinished` / `testFailed` / `testIgnored`

**`TuistTestConsoleProperties`** — Configures the test console (enables navigation, sorting, tracking).

#### 5. Test Locator (`testlocator/`)

**`TuistTestLocator`** — Implements `SMTestLocator` to resolve test identifiers from xcodebuild output back to PSI elements in Swift source files. Parses the Objective-C style test identifier (`-[Module.Class method]`) and locates the corresponding Swift file and function.

#### 6. Gutter Icons (`linemarker/`)

**`TuistTestRunLineMarkerContributor`** — Provides run/debug gutter icons next to Swift test functions and test classes. Detects test functions by:

- Functions starting with `test` in classes inheriting from `XCTestCase`
- Functions annotated with `@Test` (Swift Testing framework)

Clicking the gutter icon creates and executes a `TuistTestRunConfiguration` scoped to that specific test.

### Extension Points (plugin.xml)

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- Run configuration -->
    <configurationType implementation="...TuistTestConfigurationType"/>
    <runConfigurationProducer implementation="...TuistTestRunConfigurationProducer"/>

    <!-- Test framework integration -->
    <testLocator implementation="...TuistTestLocator"/>

    <!-- Gutter icons -->
    <runLineMarkerContributor language="Swift"
        implementation="...TuistTestRunLineMarkerContributor"/>

    <!-- Project detection -->
    <postStartupActivity implementation="...TuistProjectDetector"/>
</extensions>
```

---

## Implementation Phases

### Phase 1: Basic Scheme Execution

**Goal:** Run `tuist xcodebuild test` for an entire scheme and display raw output.

- Implement `TuistProjectDetector` to identify Tuist projects
- Create `TuistTestConfigurationType` and `TuistTestRunConfiguration`
- Implement `TuistTestCommandLineState` to execute `tuist xcodebuild test --scheme <name>`
- Register the run configuration type in `plugin.xml`
- Basic configuration editor with scheme selection

**Deliverable:** User can select a scheme and run all tests, seeing pass/fail in the run window.

### Phase 2: Test Tree Navigation

**Goal:** Structured test results with click-through to source.

- Implement `XcodebuildTestOutputParser` to parse xcodebuild output
- Wire parser into `TuistTestConsoleProperties` for SMTestRunner integration
- Implement `TuistTestLocator` for navigating from results to source
- Test suite/case hierarchy in the test runner panel

**Deliverable:** Test results appear as a navigable tree. Clicking a test navigates to its source.

### Phase 3: Granular Execution

**Goal:** Run individual test classes and methods.

- Implement `-only-testing:` argument construction for class/method targeting
- Implement `TuistTestRunConfigurationProducer` for context-based configs
- Add `TuistTestRunLineMarkerContributor` for gutter icons
- Re-run failed tests functionality

**Deliverable:** Gutter icons on test functions. Right-click to run a single test. Re-run failed button works.

### Phase 4: Advanced Features

**Goal:** Simulator selection and polish.

- Device/simulator query and selection UI
- Additional arguments pass-through
- Improved error handling and user-facing messages
- Settings/preferences page for default scheme, tuist binary path, etc.

**Deliverable:** Full-featured test runner with simulator selection.

---

## Technical Constraints

- **macOS only** — Tuist and xcodebuild are macOS-exclusive tools
- **Tuist CLI required** — The plugin invokes `tuist xcodebuild` as an external process; Tuist must be installed and on PATH
- **Swift plugin dependency** — Gutter icons and test locator require a Swift language plugin to provide PSI for `.swift` files. Without it, these features degrade gracefully (run config still works manually)
- **IntelliJ 2025.2.5+** — Minimum platform version (build 252)
- **Java 21** — JVM target

---

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Test output format | xcodebuild stdout parsing | Well-documented format, battle-tested by AppCode |
| Test execution | `tuist xcodebuild test` CLI wrapper | Leverages Tuist's project generation and scheme management |
| Test framework | SMTestRunner (ServiceMessage protocol) | Standard IntelliJ test integration, supports tree view and navigation |
| Granular execution | `-only-testing:` xcodebuild flag | Official xcodebuild mechanism, passed directly through `tuist xcodebuild test` |
| Project detection | File-system heuristic (`Tuist/` dir or `Project.swift`) | Lightweight, no process execution needed |
| Scheme discovery | `tuist graph --format json` | Structured output, includes test target metadata |

---

## Dependencies

### Platform

- IntelliJ Platform SDK 2025.2.5
- Kotlin 2.2.x

### Optional Plugin Dependencies

- Swift language plugin (for PSI-based features like gutter icons and test locator)

### External Tools (runtime)

- `tuist` CLI (user-installed)
- Xcode Command Line Tools (provides `xcodebuild`, invoked by Tuist)
