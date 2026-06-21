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

## Structure

- `src/main/java/org/infernus/idea/checkstyle/` — plugin code (actions, checker, config, model, ui, etc.)
- `src/main/resources/META-INF/plugin.xml` — plugin descriptor (services, actions, extensions)
- `src/main/resources/checkstyle-idea.properties` — supported Checkstyle versions + mappings
- `src/csaccess/java/` — code isolated behind per-version classloaders (compiled against base Checkstyle 10.0)
- `src/csaccessTest/java/` — tests for csaccess; `xTest` runs against all supported versions
- `buildSrc/` — custom Gradle plugin: source sets, artifact gathering, cross-version test tasks, JaCoCo (60% min for csaccess)
- `build.gradle.kts` — main build config; IntelliJ Platform Gradle Plugin 2.16.0; IDEA Community 2024.3.7

## Key Concepts

**Classloader isolation:** `csaccess` code is loaded in separate classloaders per Checkstyle version at runtime via `CheckstyleClassLoaderContainer`. Static state is duplicated per loader; class identity differs across loaders.

**Adding a Checkstyle version:** Add to `checkstyle.versions.supported` in `checkstyle-idea.properties`, run `./gradlew gatherCheckstyleArtifacts`, run `./gradlew xTest`, update CHANGELOG.md.

**Tests:** JUnit 5 (Jupiter) + Hamcrest + Mockito. `jvmArgs("-Xshare:off")`. Sandbox must have artifacts copied before tests run.

**Services:** Registered in `plugin.xml`, accessed via `project.getService(...)`. Key: `CheckstyleProjectService`, `StaticScanner`.

**Debug logging:** IDEA Help > Debug Log Settings > `#org.infernus.idea.checkstyle`

**Sandbox:** `build/idea-sandbox/` — not auto-cleaned; delete manually if stale.

**Plugin requires IDEA restart** (`require-restart="true"`).

**Eclipse-CS variables supported:** `basedir`, `project_loc`, `workspace_loc`, `config_loc`, `samedir`. Property substitution (`${prop}`) resolved before passing config to Checkstyle.

**Release:** Tag and push (e.g. `git tag 26.0.0 && git push origin 26.0.0`). CI builds, creates GitHub release, publishes to JetBrains marketplace.

## Contributing

1. Follow existing code style; no wildcard imports; standard IntelliJ annotations
2. Add tests; run `./gradlew build`; run `./gradlew xTest` if touching csaccess
3. Test with `./gradlew runIde`
4. Update CHANGELOG.md

## Known Non-Issues

Do not re-raise these as bugs:

- **`ConfigurationLocation.resolve()` — `reset()` without `mark()`**: Intentional; caught `IOException` triggers fresh stream via `resolveFile()`.
- **`StaticScanner.checksInProgress` — unbounded growth**: All exit paths call `checkComplete()` which removes futures.
- **`CheckerFactory.blockAndShow*` methods**: Already share logic via `blockAnd()` helper; bodies differ meaningfully.
- **`CheckStyleInspection.checkFile()` — nested thread**: Intentional polling loop for cancellation support; Checkstyle scanning is non-cooperative.
- **`FindChildFiles.visitFile()` — no `super` call**: Base `visitFile()` is a no-op returning `true`.
- **`setForkEvery(1)`**: Not actually set in the build; only `jvmArgs("-Xshare:off")` and `useJUnitPlatform()`.
- **`CheckerFactoryCacheTest` — 8 failures**: Pre-existing; `DependencyValidationManager.getInstance` returns null without full platform services. Needs `BasePlatformTestCase` or mock refactor.
- **`PsiFileValidator.isInNamedScopeIfPresent()`**: Was a real bug (empty stream from null scopes returned `false`), now fixed.
