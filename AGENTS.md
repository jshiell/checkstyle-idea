# CheckStyle-IDEA Plugin

IntelliJ IDEA plugin providing real-time Checkstyle feedback. Java, JDK 21, Gradle wrapper.

## Commands

```bash
./gradlew clean build        # Build
./gradlew test               # Unit + csaccess tests (base version)
./gradlew xTest              # csaccess tests against ALL Checkstyle versions
./gradlew runIde             # Launch sandbox IDEA with plugin
./gradlew buildPlugin        # Build distributable ZIP
./gradlew publishPlugin      # Publish (needs JETBRAINS_PLUGIN_REPO_TOKEN)
./gradlew csaccessTest_cs_13.0.0  # Test specific Checkstyle version
```

### Gradle DMG/hdiutil: fixed for build/test/buildPlugin/runIde; verifyPlugin is the one exception

`build.gradle.kts`'s `intellijIdeaCommunity(...)` sets `useInstaller = false`. Confirmed by disassembling
`intellij-platform-gradle-plugin-2.18.1.jar`: `IntelliJPlatformDependencyConfiguration.useInstaller` switches
between the IDE installer (a `.dmg` on macOS, Maven coordinate group `idea`) and the plain archive (`.zip`,
group `com.jetbrains.intellij.idea`) from the IntelliJ Maven repository. With it `false`, Gradle downloads and
unpacks `ideaIC-<version>.zip` directly — no `hdiutil attach` at all. Verified 2026-09-14 entirely inside the
`nono` sandbox: `./gradlew test` (781 + 75 csaccess tests), `buildPlugin`, and an actual `runIde` launch
(Welcome Frame shown, AWT event loop running, no errors) all succeeded with zero `hdiutil` invocations.

**`verifyPlugin` still needs `hdiutil` and must run outside the sandbox.** It resolves its own IDEs
independently of the main `intellijIdeaCommunity` dependency. Confirmed by disassembling
`IntelliJPlatformExtension.PluginVerification.Ides`: `recommended()` / `defaultRecommended()` / `latest()` all
route through `createInstallerDependencies(...)`, whose Kotlin default-args bridge hardcodes `useInstaller =
true` (`iconst_1`) whenever the caller doesn't override it — there is no DSL knob on these convenience methods
to change that. The only escape is replacing `recommended()` with explicit
`ides { create(IntelliJPlatformType.IdeaCommunity, "<version>") { useInstaller = false } }` entries per version,
which was rejected as too invasive (loses auto-tracking of base + next-major, needs a manual update every
version bump). So `verifyPlugin` alone still hits the `.dmg` path below and needs a real terminal.

For `verifyPlugin`'s `.dmg` extraction: `hdiutil` must create a mount point under `/Volumes`, which advertises
`readwrite` in the sandbox capability list but enforces read-only (the inherited `system_read_macos` group's
read grant wins over an explicit `/Volumes` `readwrite` grant — `nono why --path /Volumes --op write`
misreports this as allowed). `hdiutil imageinfo` confirms the DMG itself is fine; `hdiutil attach -nomount` and
an explicit `-mountpoint` outside `/Volumes` fail the same way — the block is on attaching at all. Neither
`dangerouslyDisableSandbox` nor running `! ./gradlew …` from the prompt gets around it (both still execute
inside the same sandboxed `claude` process). Re-extraction has to happen in a **real terminal outside Claude
Code**; one `./gradlew verifyPlugin` there repopulates the cache and every later sandboxed run of it hits that
cache — the result lives in `~/.gradle/caches/<gradle-version>/transforms/*/transformed/ideaIC-*`, and a
configuration-cache hit skips re-extraction. **A failed attempt wipes and recreates the transform's
`transformed/` directory**, so every later run fails until an unsandboxed run re-extracts it.

**A Gradle daemon started before a sandbox profile grant change keeps enforcing the old Seatbelt profile for
its child processes even after the grant is added — this bites more than just the `hdiutil` case above.**
Observed 2026-09-14: after a new `~/Library/Application Support/JetBrains` read/write grant was added
mid-session, `buildSearchableOptions` kept failing with `FileAlreadyExistsException` on that exact path
(`java.nio.file.FileAlreadyExistsException` from `PathManager.getCommonDataPath`) until `./gradlew --stop` was
run; the very next `buildPlugin` invocation then succeeded cleanly. This supersedes an earlier note here that
attributed the identical failure to something subagent-specific and "mechanism unconfirmed" — it reproduced
identically and repeatedly from the top-level session too, and the real cause was a missing sandbox grant plus
a stale daemon, not anything about subagents. **Always run `./gradlew --stop` after any sandbox profile/grant
change**, not only after repairing a poisoned DMG transform cache.

Driving JUnit directly with `javac`, as a way to test without Gradle, **does not work for anything that
extends `LightPlatformTestCase`** in 2024.3. The platform is split across `lib/modules/*.jar` v2 content
modules, and a flat classpath collides duplicates: startup dies with `NoSuchFieldError: … JavaStubIndexKeys …
IMPLICIT_CLASSES` and `Index data initialization failed`. Plain unit tests that need no platform fixture do
still compile and run this way. Note that the IC distribution *does* ship `plugins/maven/`, and the Maven test
framework is available as `com.jetbrains.intellij.maven:maven-test-framework` — a compile failure on
`com.intellij.maven.testFramework` means that artifact is missing from your hand-built classpath, not from the
distribution.

## Structure

- `src/main/java/org/infernus/idea/checkstyle/` — plugin code (actions, checker, config, model, ui, etc.)
- `src/main/resources/META-INF/plugin.xml` — plugin descriptor (services, actions, extensions)
- `src/main/resources/checkstyle-idea.properties` — supported Checkstyle versions + mappings
- `src/csaccess/java/` — code isolated behind per-version classloaders (compiled against base Checkstyle 10.0)
- `src/csaccessTest/java/` — tests for csaccess; `xTest` runs against all supported versions
- `buildSrc/` — custom Gradle plugin: source sets, artifact gathering, cross-version test tasks, JaCoCo (60% min for csaccess)
- `build.gradle.kts` — main build config; IntelliJ Platform Gradle Plugin 2.18.1; IDEA Community 2025.1.7.2

## Key Concepts

**Classloader isolation:** `csaccess` code is loaded in separate classloaders per Checkstyle version at runtime via `CheckstyleClassLoaderContainer`. Static state is duplicated per loader; class identity differs across loaders.

**Adding a Checkstyle version:** Add to `checkstyle.versions.supported` in `checkstyle-idea.properties`, run `./gradlew gatherCheckstyleArtifacts`, run `./gradlew xTest`, update CHANGELOG.md. A weekly workflow (`.github/workflows/check-checkstyle-version.yml`) files/updates a `checkstyle-update` tracking issue when upstream is ahead, and closes it automatically once `checkstyle.versions.supported` or `checkstyle.versions.map` catches up.

**Tests:** JUnit 5 (Jupiter) + Hamcrest + Mockito. `jvmArgs("-Xshare:off")`. Sandbox must have artifacts copied before tests run.

**Never mock the application in `src/test`:** `ApplicationManager.setApplication` is global and the whole source
set shares one JVM, so a mock leaks into every test that runs afterwards and they fail with
`ClassCastException: Application$MockitoMock cannot be cast to ApplicationEx`. Registering a `Disposable` that is
never disposed does not undo it. Test through a seam that does not need an application — e.g. `CheckerFactory`
throws rather than notifying when the module is null. `src/csaccessTest` gets away with it only because it runs
in its own JVM.

**Reset global platform state in `tearDown`:** the application-mock leak above is one instance of a general
rule. Anything installed into a static platform holder — `ApplicationManager.setApplication`, a registered
service or extension point, a `ServiceContainerUtil` replacement, a swapped `Disposable` parent — outlives the
test method and the test class, because the source set shares one JVM. Whatever a test installs, the same test
must undo in `tearDown`, unconditionally, so it also runs when the test fails partway through. The damage does
not surface where it is caused: the guilty test passes and some unrelated class fails later, in an order that
depends on how the tests were selected. That is why **a green run of the class you touched proves nothing about
this bug**. Before committing, run the full suite (`./gradlew build`, plus `./gradlew xTest` if you touched
csaccess) — never just the test class you edited. If a test only passes in isolation, treat that as the leak,
not as flakiness.

**A platform test fails on any thread's uncaught exception, not just the test thread's:** JUnit5 platform tests
run under `UncaughtExceptionExtension`/`TestUncaughtExceptionHandler`, which fails the test if *any* thread
throws uncaught during it — including a helper thread the test itself spawns to exercise cross-thread
behaviour (e.g. interrupting a caller thread from another thread). A pre-existing, expected exception path on
that helper thread will fail the test with `AssertionFailedError: N uncaught exceptions` instead of the
assertion you meant to check — catch it explicitly inside the thread's `Runnable`.

**Verifying a fix to a threading/listener/polling test:** a single green run proves little for this class of
bug. After changing a test or the code it exercises for a race, ordering, or polling issue, rerun that test
10x on its own before trusting it, in addition to the full-suite run already required before committing.

**Services:** Registered in `plugin.xml`, accessed via `project.getService(...)`. Key: `CheckstyleProjectService`, `StaticScanner`.

**Using a class from a v2 content module:** classes that live only in `lib/modules/*.jar` (e.g.
`BooleanCommitOption`, in `intellij.platform.vcs.impl`) need *two* declarations — `bundledModule("<name>")` in
`build.gradle.kts` for the compile classpath, and a `<dependencies><module name="<name>"/></dependencies>`
block in `plugin.xml` for the runtime classloader. A v1 `<depends>` tag does not grant access to a v2 content
module, and omitting the `plugin.xml` half compiles cleanly but fails at runtime with `NoClassDefFoundError`.
`./gradlew test` will not catch it either: tests run on a flat classpath. Use the plugin verifier (below).

**Verifying platform API behaviour:** the resolved IDE distribution lives at
`~/.gradle/caches/<gradle-version>/transforms/*/transformed/ideaIC-*/lib/*.jar` (plus `lib/modules/*.jar` for
v2 content modules). To confirm real behaviour rather than assume it: `unzip -o -j <jar> '<path/To/Class.class>'
-d <tmpdir>` then `javap -p -v <Class>.class` — annotations like `@ApiStatus.Internal` show as
`RuntimeInvisibleAnnotations` in `-v` output. Most core platform classes are in `lib/app-client.jar`,
`lib/util.jar`, or `lib/util-8.jar`; check those directly before looping `find` across the ~500+ jars in the
distribution, which can silently return zero matches even when the class exists.

**`CheckStyleBundle.message(key, params)` and apostrophes:** any call with a non-empty `params` routes
through `java.text.MessageFormat` (confirmed by disassembling `BundleBase.format` in `lib/util-8.jar`),
which treats a lone `'` as an unterminated quote — it swallows everything after it, including `{0}`,
until the next `''` or end of string. Every parameterized entry in `CheckStyleBundle.properties` needs
literal apostrophes doubled (`rule''s`), matching the convention already at lines 11 and 16. A zero-arg
key is unaffected — empty `params` skips `MessageFormat` entirely. Verify a new entry by actually
rendering it (`MessageFormat.format(pattern, args)`), not by inspection; a test that compares
`CheckStyleBundle.message(key, arg)` against itself proves nothing about the properties file.

**Debug logging:** IDEA Help > Debug Log Settings > `#org.infernus.idea.checkstyle`

**Sandbox:** `build/idea-sandbox/` — not auto-cleaned; delete manually if stale.

**`./gradlew verifyPlugin` works** — as of the 2025.1 base version with IntelliJ Platform Gradle Plugin
2.18.1. It previously aborted on the descriptor check (`The plugin name 'CheckStyle-IDEA' should not include
the word 'IDEA'`) before reaching class resolution; `pluginVerification { freeArgs = listOf("-mute",
"TemplateWordInPluginName") }` in `build.gradle.kts` settles that. It resolves its own IDEs, so no `ides { }`
block is needed, and it verifies against both the base version and the next major — useful because
`untilBuild` is null.

This is the only automated check for a missing v2 module dependency; `./gradlew test` cannot catch one
because tests run on a flat classpath.

Read `build/reports/pluginVerifier/<IDE>/plugins/CheckStyle-IDEA/<version>/verification-verdict.txt`.
**Blocking:** any unresolved class or method reference, or any internal-API usage. **Not blocking:** a
changed count of deprecated or experimental usages — those move with every platform bump. As of 26.16.0 both
IC-251 and IC-252 report `Compatible`, with no deprecated usages and 12 experimental ones (see Known
Non-Issues).

If you ever do need the CLI directly, `-Dplugin.verifier.home.dir="$TMPDIR/pv-home"` is required under the
sandbox — the default `~/.pluginVerifier` is not writable.

**Plugin supports dynamic load/unload** (no `require-restart` attribute) as of the 2026-08-23 hot-reload work.
`require-restart="true"` was removed from `plugin.xml` after: (1) leak fixes shipped first
(`CheckstyleClassLoaderContainer.close()`; `CheckstyleProjectService`/`CheckStyleToolWindowPanel`/`StaticScanner`
now `Disposable`; listener deregistration in `PluginConfigurationManager`; cancellation propagation in
`CheckerFactory`), so a plugin update or disable/enable doesn't tear down dirty runtime state; (2)
`DynamicUnloadEligibilityTest` — which calls the platform's own `DynamicPlugins.checkCanUnloadWithoutRestart`
against the real loaded descriptor — confirmed via a throwaway spike (`require-restart` temporarily flipped to
`false`, then reverted) that the attribute was the *only* remaining blocker; (3) the manual `./gradlew runIde`
sandbox pass below was run and passed. `PluginDescriptorDynamicUnloadTripwireTest` now asserts the attribute
is absent, and `DynamicUnloadEligibilityTest` asserts the platform reports no blocker at all.

If field reports of load/unload trouble arrive after release, re-adding `require-restart="true"` is a
one-attribute patch release — treat that as an expected, cheap rollback, not a sign the leak fixes need
unwinding too.

The manual verification procedure, for reference (`./gradlew runIde`; registry
`ide.plugins.allow.unload.from.sources=true`, `ide.plugins.snapshot.on.unload.fail=true`; debug log
`#com.intellij.ide.plugins` and `#org.infernus.idea.checkstyle`): open a project, run a scan and record the
violation count, open the tool window, change the Checkstyle version (forces a second loader), run Reload
Rules Files, open/Cancel Settings a few times, import a code style scheme, import a Maven project. Then,
three times each: (1) disable/re-enable the plugin; (2) toggle the Maven plugin while CheckStyle-IDEA is
loaded, then unload CheckStyle-IDEA with Maven disabled; (3) **the actual #539 regression** —
`buildPlugin` at version N, install into a clean sandbox from disk, exercise as above, `buildPlugin` at N+1,
install over the top *without restarting*, confirm the old plugin directory was actually replaced. Scenario
(1) never replaces the plugin directory, so only (3) can reproduce the original open-handle failure. Pass
criteria: no "is not unload-safe" / "was not unloaded" in `idea.log`, no memory snapshot captured, no
`Throwable` from `org.infernus.idea.checkstyle` anywhere in the log, and a post-reload re-scan matches the
recorded violation count.

**Gradle settings import (#439) — manual verification procedure, for reference:** no automated Gradle-sync
integration test framework exists in this project's tooling (confirmed: no `TestFrameworkType.Plugin.Gradle`
in `org.jetbrains.intellij.platform:intellij-platform-gradle-plugin:2.18.1`, unlike `TestFrameworkType.Plugin.Maven`)
— the project already accepts this class of gap for Maven multi-module; the same applies here. Before release,
with "Import settings from Gradle" toggled both on and off: (1) Groovy DSL, single module, `checkstyle {
configFile = file(...); configProperties = [...] }`; (2) the Kotlin DSL equivalent; (3) an actual Android/AGP
sample project using the raw `task checkstyle(type: Checkstyle) {}` pattern, not just a plain-Gradle stand-in;
(4) a multi-module project with more than one subproject configured, and a `subprojects { apply plugin:
'checkstyle' }` shape with no root configuration; (5) no `checkstyle` plugin applied at all → no-op, no
exceptions; (6) a config file referencing `${config_loc}` — Gradle injects `config_loc`/`configDirectory` at
task *execution* time, not onto the extension, so it never appears in the imported `configProperties`; confirm
the plugin's own built-in `config_loc` resolution still covers it; (7) delete the `checkstyle {}` block and
re-sync → the previously-imported location is removed; (8) toggle the opt-in off after a previous import → the
imported location is left in place, not removed; (9) sync, disable the plugin, restart with the external-system
data cache still populated, confirm no `ClassNotFoundException`/deserialization error in `idea.log` for
`CheckstyleGradleModuleData`; (10) repeat scenario 1 against the oldest and newest Gradle versions this project
intends to support — relevant regardless of IDE version, since `CheckstyleGradleModelBuilder` runs inside the
*project's* Gradle daemon, not the IDE's. Pass criteria: `.idea/checkstyle-idea.xml` gets the expected
location/properties/version; no `Throwable` from `org.infernus.idea.checkstyle` in `idea.log`.

The `gradleTooling` source set (`CheckstyleGradleModelBuilder`, injected into the target project's Gradle
daemon via IntelliJ's generated initscript) must compile against Gradle's own API only, never IntelliJ platform
classes — `GradleInitScriptUtil` explicitly excludes `lib/app.jar` etc. from injection. `bundledPlugin("com.intellij.gradle")`
resolves the right `gradle-api-*.jar`/`gradle-tooling-extension-api.jar` via the ordinary Gradle dependency
mechanism (filtered out of the leaf configuration `intellijPlatformBundledPlugins`) — no hardcoded
`~/.gradle/caches/.../transformed/...` path needed. The IntelliJ Platform Gradle Plugin patches *every*
registered `Jar` task in the project to also embed `META-INF/plugin.xml`; `gradleToolingJar` explicitly
`exclude("META-INF/plugin.xml")`s so the injected jar carries nothing beyond its own classes.

**Eclipse-CS variables supported:** `basedir`, `project_loc`, `workspace_loc`, `config_loc`, `samedir`, built per-module in `CheckerFactory`. References in the rules file (`${prop}`) are resolved by Checkstyle itself, via `ListPropertyResolver`. Checkstyle's resolution is single-pass, so references appearing in *user property values* are expanded plugin-side by `PropertyExpander` before the built-ins are merged in - this is what lets one property resolve differently per module. Unresolvable references are left verbatim.

**Release:** Tag and push (e.g. `git tag 26.0.0 && git push origin 26.0.0`). CI builds, creates GitHub release, publishes to JetBrains marketplace.

## Contributing

1. Follow existing code style; no wildcard imports; standard IntelliJ annotations
2. Add tests; run the **full** suite with `./gradlew build` before committing — not just the test
   class you touched, which cannot catch leaked global platform state; run `./gradlew xTest` if
   touching csaccess
3. Test with `./gradlew runIde`
4. Update CHANGELOG.md and, for a user-visible change, the `<change-notes>` block in `plugin.xml` — both are
   added under the **current** `version` from `build.gradle.kts`. This repo does not use an `Unreleased`
   heading; pending changes accumulate under that not-yet-tagged version until it is actually released
   (`git tag` + push, per Release below). Never add an entry under a version number that isn't already the
   current `version` in `build.gradle.kts` — check that file first rather than guessing the next number.
5. Planning for an issue produces a `plan-<issue>.md` file at repo root (gitignored via `/plan-*.md`, not
   committed) — this is the working convention for `/plan-issue`-style sessions, not a build artifact.

## Known Non-Issues

Do not re-raise these as bugs:

- **`ConfigurationLocation.resolve()` — `reset()` without `mark()`**: Intentional; caught `IOException` triggers fresh stream via `resolveFile()`.
- **`StaticScanner.checksInProgress` — unbounded growth**: All exit paths call `checkComplete()` which removes futures.
- **`CheckerFactory.blockAndShow*` methods**: Already share logic via `blockAnd()` helper; bodies differ meaningfully.
- **`CheckStyleInspection.checkFile()` — nested thread**: Intentional polling loop for cancellation support; Checkstyle scanning is non-cooperative.
- **`FindChildFiles.visitFile()` — no `super` call**: Base `visitFile()` is a no-op returning `true`.
- **`setForkEvery(1)`**: Not actually set in the build; only `jvmArgs("-Xshare:off")` and `useJUnitPlatform()`.
- **`CheckerFactoryCacheTest`**: Was documented here as 8 pre-existing failures. No longer true — as of 2026-08-18 it runs 8 tests, 0 failures under `./gradlew build`. Do not reinstate it as a known-failure baseline; a failure there now is a real regression.
- **`PsiFileValidator.isInNamedScopeIfPresent()`**: Was a real bug (empty stream from null scopes returned `false`), now fixed.
- **Experimental API usages reported by `verifyPlugin`**: The 12 remaining ones are all
  `MavenAfterImportConfigurator` and `MavenWorkspaceConfigurator.MavenProjectWithModules`. JetBrains marks
  the whole Maven importing API `@ApiStatus.Experimental` and offers no alternative extension point for
  post-import configuration, so these cannot be removed without dropping the Maven settings import.
