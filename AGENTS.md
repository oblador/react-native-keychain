# AGENTS.md

Instructions for coding agents working on `react-native-keychain`, a React Native
library for secure credential storage through Apple Keychain and Android Keystore.

## Sources of truth

- `src/index.ts`, `src/types.ts`, and `src/enums.ts`: public exports, signatures,
  options, return values, and platform annotations. Inspect named exports and the
  default export separately; they are not currently identical.
- `src/NativeKeychainManager.ts`: TurboModule contract and optional native methods.
- `README.md` and `website/docs/`: installation and application-developer guidance.
  API documentation is generated from the TypeScript source and its TSDoc.
- `package.json`, `.node-version`, `mise.toml`, and workspace manifests: versions,
  toolchain, scripts, and dependency constraints.
- `RNKeychain.podspec`, `android/gradle.properties`, and the example's native
  configuration: platform targets. Library defaults and example requirements can differ.
- `.github/workflows/` and `KeychainExample/.detoxrc.js`: CI setup, native builds,
  device configuration, and test execution.

This checkout uses the New Architecture and TurboModules exclusively. Read the
current peer dependencies for supported React Native versions; do not add a
legacy bridge fallback. The package version can lag unreleased changes on
`master`; do not infer released-version support from it alone.

## Where to work

| Concern                                    | Start here                                                                                                                                   |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Public API and option normalization        | `src/index.ts`, `src/types.ts`, `src/enums.ts`, `src/normalizeOptions.ts`                                                                    |
| Native contract and registration           | `src/NativeKeychainManager.ts`, `ios/RNKeychainManager/RNKeychainManager.h`, `android/src/main/java/com/oblador/keychain/KeychainPackage.kt` |
| Apple Keychain queries and authentication  | `ios/RNKeychainManager/RNKeychainManager.mm`                                                                                                 |
| Android API, cipher selection, and errors  | `android/src/main/java/com/oblador/keychain/KeychainModule.kt`                                                                               |
| Android persistence and migration          | `PrefsStorageBase.kt`, `DataStorePrefsStorage.kt` under the same Android package                                                             |
| Android encryption and authentication flow | `cipherStorage/`, `resultHandler/`, `DeviceAvailability.kt` under the same Android package                                                   |
| Example UI and regression coverage         | `KeychainExample/src/App.tsx`, `KeychainExample/e2e/testCases/`, `KeychainExample/e2e/utils/`                                                |
| Documentation and site configuration       | `website/docs/`, `website/.vitepress/config.ts`                                                                                              |

Edit sources, not generated output: `lib/`, native Codegen output, Pods, native
build directories, `website/docs/api/`, `website/.vitepress/dist/`, or `website/.vitepress/cache/`.
Do not hand-edit lockfiles or regenerate them for unrelated changes.

## Contracts to preserve

### Public API and native bridge

- Keep TypeScript wrappers, the Codegen spec, and both native implementations
  aligned. TypeScript casts do not validate native result shapes. Preserve the
  distinction between missing credentials, unsupported capabilities, and rejected
  operations; do not turn authentication or storage errors into a missing-item result.
- Credential set/get wrappers apply `normalizeAuthPrompt()`. Preserve its defaults
  and caller overrides without mutating caller options. Other public methods may
  intentionally pass options through unchanged.
- Optional native methods have explicit fallbacks in `src/index.ts`. Preserve
  platform-specific behavior rather than assuming every method exists everywhere.
- When adding an API or enum, update the relevant types, spec, native handling,
  intended named/default exports, and example controls as applicable. Enum string
  values cross the native boundary; some also identify persisted storage.
- Error mappings live in `src/enums.ts`, Android's `KeychainModule.Errors` and
  `exceptions/`/`resultHandler/`, and iOS's `laErrorCode`, `secErrorInfo`, and
  `rejectWithError`. Check the affected path end to end; mappings and availability
  differ by platform, and shared-web-credential paths also return native error codes.

### Credential storage and authentication

- Existing credentials must remain readable after an upgrade. Treat service/server
  aliases, cipher names, preference keys, and ciphertext encodings as persisted
  formats. Format or default-selection changes need an explicit compatibility or
  migration strategy and coverage using data written by the previous behavior.
- Android stores encrypted values in DataStore and keys in Keystore. Preserve
  `RN_KEYCHAIN`, the `:u`/`:p`/`:c` preference keys, and the SharedPreferences
  migration unless deliberately implementing and verifying a migration.
- Android reads use the entry's recorded cipher. Deprecating AES-CBC for new writes
  does not authorize removing its reader. A migration must persist the replacement
  successfully before deleting old key material and must not lower protection.
- Do not silently weaken requested access control or security level to make an
  operation succeed. Keep authenticated and unauthenticated storage distinct.
  Do not add plaintext credential or key logging, including in failure paths.
- iOS generic and internet credentials use different Keychain classes. Preserve the
  relevant service/server, access-group, and synchronization scope when changing
  reads, existence checks, replacements, or deletes; do not broaden a query as a fix.
- For iOS credential operations, omitted `cloudSync` and `false` select
  non-synchronizable items; `true` selects a separate set. A replacement or reset
  in one set must leave the other intact. Existence checks must not prompt for
  authentication; an existing protected item can report interaction-not-allowed.
- Keep Apple platform guards and availability checks around platform-specific APIs.
  Review Core Foundation ownership when changing Security.framework calls.
- Android cipher operations are serialized by the module mutex; cipher instances
  are not thread-safe. Preserve synchronization and module-invalidation cleanup.
  Show biometric UI on the main thread and keep blocking waits off it. Authentication
  callbacks must settle each operation once, including cancellation and failure.
  Inspect the manual-retry handler when changing the shared prompt lifecycle.

## Setup and commands

Use the Node version in `.node-version` and the Yarn version in
`package.json#packageManager`. Install with `yarn install --immutable` from the
repository root. For intentional dependency changes, update through Yarn and
review the lockfile diff. Worktrees need their own dependency/native setup;
do not assume ignored build artifacts were copied from another checkout.

Run from the repository root unless a directory is specified:

| Purpose                           | Command                                                                         |
| --------------------------------- | ------------------------------------------------------------------------------- |
| Library TypeScript checks         | `yarn typecheck`                                                                |
| ESLint                            | `yarn lint`                                                                     |
| Package JS and declaration build  | `yarn prepare`                                                                  |
| Check formatting of changed files | `yarn prettier --check <changed-files>`                                         |
| Format selected files             | `yarn prettier --write <changed-files>`                                         |
| Documentation build               | `yarn workspace website build`                                                  |
| Website TypeScript checks         | `yarn workspace website typecheck`                                              |
| Example Metro server              | `yarn workspace KeychainExample start`                                          |
| Run example on a device           | `yarn workspace KeychainExample ios` / `yarn workspace KeychainExample android` |

Prefer formatting explicit paths; the root `format` script has limited glob
coverage. Root `typecheck` covers the library, not the example or website.
There is no root `yarn test` script; Jest runs the Detox suite in the example.

For iOS, run `bundle install` in `KeychainExample/`, then
`bundle exec pod install` in `KeychainExample/ios/`. Follow the existing Podfile's
React Native setup. Consult CI for Detox's additional simulator prerequisites.

For release E2E builds, run these from `KeychainExample/`, as CI does:

```sh
# iOS, after installing pods
yarn build:e2e:ios
yarn test:ios:build
python3 ../.github/scripts/run-ios-native-tests.py
yarn test:ios:run

# Android, after configuring the SDK and emulator
yarn build:e2e:android
yarn test:android:build
yarn test:android:run
```

The `build:e2e:ios` and `build:e2e:android` scripts prepare the dedicated test bundles;
`test:*:build` builds the native test apps. The `test:ios`/`test:android`
shortcuts combine the native build and run steps only. Rebuild affected artifacts
before testing; reusing an old release binary does not verify edited sources.

## Verification matched to the change

- **TypeScript/API:** run `yarn typecheck` and `yarn lint`; run `yarn prepare`
  for changes to exports, declarations, or package output. A changed native spec
  also needs Codegen/native builds on the affected platforms.
- **Native behavior:** build the affected example platform and run the relevant
  Detox cases. Shared API or credential behavior normally needs both platforms.
  A successful JS check or mocked test is not evidence of native authentication.
- **Documentation:** check formatting and referenced paths/commands. Build the
  website for site content, configuration, or API-documentation changes; check its
  types when changing website TypeScript. An `AGENTS.md`-only edit does not need
  native builds.
- Extend an existing test for a concrete regression or observable contract. Avoid
  tests that repeat implementation details, trivial checks, or duplicate coverage.
  Keep fixtures and validation proportional to the change.

Use `KeychainExample/TESTING.md` for the coverage split and native test commands.
Start storage/query regressions in Android's `src/androidTest` or the example's
`ios/KeychainNativeTests`. Keep `e2e/testCases/publicApi.spec.js` focused on the
public JS/native boundary, restart persistence and Android authentication. The
physical-device iOS cancellation test lives in `ios/KeychainUITests`; it explicitly
skips Simulator, which does not enforce Keychain biometric access controls.

For storage/query changes, check missing items, overwrite, existence, reset,
restart persistence, and isolation of unrelated entries. For authentication
changes, include relevant cancellation, failed authentication, unavailable or
unenrolled biometrics, passcode fallback, lockout, and invalidated-key cases.
For asynchronous changes, inspect concurrent calls and teardown during a prompt.
These are scenarios to select by impact, not claims that every case is already
automated.

Use the devices configured in `KeychainExample/.detoxrc.js`. Android auth helpers
expect PIN `1111` and enrolled fingerprint ID `1`; CI shows enrollment setup.
iOS enrollment is enabled in `KeychainExample/e2e/setup.ts`. Prefer the existing auth helpers,
including the wait for authentication validity to expire, over arbitrary sleeps.
Explicitly reset test credentials: reinstalling an iOS app may leave Keychain
items behind. Simulator tests for `cloudSync` verify query matching, not actual
cross-device iCloud synchronization; emulator results do not establish real
hardware-backed security.

## Documentation and completion

- Update user documentation only for public API, observable behavior, setup, or an
  explanation made inaccurate by the change. A fix restoring documented behavior
  does not automatically need new documentation.
- Make the smallest correction in the existing guide or TSDoc. Use realistic
  examples and current behavior; keep debugging narratives and implementation
  checklists in code comments, tests, or the PR description.
- Edit current docs in `website/docs/`. VitePress publishes one current version;
  do not add versioned docs or a version selector. API pages are regenerated from
  TypeScript before each build. The Docs workflow publishes on `release.published`
  and `workflow_dispatch`; routine changes should be validated without publishing.
- Before handing off, review the diff for unrelated edits and generated files.
  Report the behavior changed, checks actually run, and unverified platforms or
  scenarios. If native tools or devices are unavailable, state the limitation and
  the remaining check; do not present an attempted or skipped check as passing.
- Keep this guide focused on durable rules and useful entry points. Verify paths
  and commands when updating it; prefer configuration references over copied
  version lists and API inventories.
