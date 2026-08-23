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

### Gradle needs to mount a DMG (the nono sandbox blocks it)

On macOS the IntelliJ Platform plugin extracts `ideaIC-*.dmg` by running `hdiutil attach`. Under the `nono`
sandbox this fails during dependency resolution with `Process 'command 'hdiutil'' finished with non-zero exit
value 1` (`hdiutil: attach failed - Operation not permitted`).

The cause is specifically that **`hdiutil` must create a mount point under `/Volumes`, and `/Volumes` is
granted read-only**. Diagnose it with `nono why --path /Volumes --op write`. Beware: a profile granting
`filesystem.allow: ["/Volumes"]` is *not* sufficient — the inherited `system_read_macos` group also covers
`/Volumes` with read access and wins, so the capability list advertises `readwrite` while enforcement is
read-only. Use `hdiutil imageinfo` to confirm the DMG itself is fine — it works under the sandbox and rules
out corruption. `hdiutil attach -nomount` does **not** work despite needing no mount point: it checksums the
image, then still fails with `attach failed - Operation not permitted`. Passing an explicit `-mountpoint`
outside `/Volumes` fails the same way, so the block is on attaching at all, not on writing to `/Volumes`.

Two things that do **not** get around this: the `dangerouslyDisableSandbox` tool flag (nono wraps the whole
`claude` process, so the flag is irrelevant to it), and running `! ./gradlew …` from the prompt (that executes
in the same session, inside the same sandbox). Re-extraction has to happen in a **real terminal outside
Claude Code**; one `./gradlew build` there repopulates the cache and every later sandboxed build hits it.

This only bites when the extraction has to run again — the result lives in
`~/.gradle/caches/<gradle-version>/transforms/*/transformed/ideaIC-*`, and a configuration cache hit skips it.
Adding a task option such as `--tests` misses that cache and triggers it, and so does a task set that has no
cache entry yet. **A failed attempt wipes and recreates the transform's `transformed/` directory, so every
later build fails until an unsandboxed `./gradlew build` re-extracts it.**

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

**Adding a Checkstyle version:** Add to `checkstyle.versions.supported` in `checkstyle-idea.properties`, run `./gradlew gatherCheckstyleArtifacts`, run `./gradlew xTest`, update CHANGELOG.md.

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
   (`git tag` + push, per Release below).

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
