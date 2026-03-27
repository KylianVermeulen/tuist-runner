import ProjectDescription

let project = Project(
    name: "TuistRunnerExample",
    targets: [
        .target(
            name: "App",
            destinations: [.iPhone, .mac],
            product: .framework,
            bundleId: "com.example.tuistrunner.app",
            sources: ["Sources/App/**"]
        ),
        .target(
            name: "AppTests",
            destinations: [.iPhone, .mac],
            product: .unitTests,
            bundleId: "com.example.tuistrunner.apptests",
            sources: ["Tests/AppTests/**"],
            dependencies: [
                .target(name: "App"),
            ]
        ),
    ],
    schemes: [
        .scheme(
            name: "TuistRunnerExample",
            buildAction: .buildAction(targets: [
                .target("App"),
            ]),
            testAction: .targets([
                .testableTarget(target: .target("AppTests")),
            ])
        ),
    ]
)
