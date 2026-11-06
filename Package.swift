// swift-tools-version: 6.0

import PackageDescription

// React Native's SPM autolinker resolves this package through
// <app>/ios/build/generated/autolinking/libs/RNKeychain. Dependency paths
// below are relative to that symlink, not the npm package's install directory.
let package = Package(
    name: "RNKeychain",
    platforms: [.iOS("15.1")],
    products: [
        .library(name: "RNKeychain", targets: ["RNKeychain"]),
    ],
    dependencies: [
        .package(name: "ReactNative", path: "../../../../xcframeworks"),
        .package(name: "React-GeneratedCode", path: "../../../ios"),
    ],
    targets: [
        .target(
            name: "RNKeychain",
            dependencies: [
                .product(name: "ReactHeaders", package: "ReactNative"),
                .product(name: "ReactNativeHeaders", package: "ReactNative"),
                .product(name: "ReactNativeDependenciesHeaders", package: "ReactNative"),
                .product(name: "ReactAppHeaders", package: "React-GeneratedCode"),
            ],
            path: "ios/RNKeychainManager",
            publicHeadersPath: ".",
            cSettings: [
                .define("RCT_NEW_ARCH_ENABLED", to: "1"),
                .headerSearchPath("../../spm-headers"),
            ],
            cxxSettings: [
                .define("RCT_NEW_ARCH_ENABLED", to: "1"),
                .headerSearchPath("../../spm-headers"),
                // Match the configuration-dependent ABI of prebuilt React Native.
                .define("DEBUG", .when(configuration: .debug)),
                .define("NDEBUG", .when(configuration: .release)),
            ],
            linkerSettings: [
                .linkedFramework("Security"),
                .linkedFramework("LocalAuthentication"),
            ]
        ),
    ],
    cxxLanguageStandard: .cxx20
)
