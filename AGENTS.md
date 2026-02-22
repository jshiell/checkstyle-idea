# Agent Guide: CheckStyle-IDEA Plugin

## Project Overview

This is a plugin for JetBrains IntelliJ IDEA that provides real-time Checkstyle feedback. It integrates Checkstyle 10-13 with IDEA 2024-2025 to provide code inspection and static scanning.

**Key characteristics:**
- IntelliJ IDEA plugin built with Gradle and the IntelliJ Platform Gradle Plugin
- Written in Java (162 source files in main)
- Supports multiple Checkstyle versions dynamically via custom classloading
- Uses custom Gradle plugin in `buildSrc/` for specialized build logic
- Requires JDK 21 (minimum runtime and compilation target)
- Developed on macOS but should work on Linux/Windows

## Project Structure

```
checkstyle-idea/
├── src/
│   ├── main/                    # Main plugin code
│   │   ├── java/org/infernus/idea/checkstyle/
│   │   │   ├── actions/         # IDE actions (scan, expand/collapse, etc.)
│   │   │   ├── checker/         # Checkstyle checker integration
│   │   │   ├── checks/          # Custom check implementations
│   │   │   ├── config/          # Configuration management
│   │   │   ├── csapi/           # API for csaccess isolation
│   │   │   ├── exception/       # Plugin exceptions
│   │   │   ├── handlers/        # VCS checkin handlers
│   │   │   ├── importer/        # Code style importers
│   │   │   ├── model/           # Configuration locations and models
│   │   │   ├── startup/         # Startup activities
│   │   │   ├── toolwindow/      # Tool window UI
│   │   │   ├── ui/              # Configuration UI panels
│   │   │   └── util/            # Utilities
│   │   └── resources/
│   │       ├── META-INF/plugin.xml  # Plugin descriptor
│   │       └── checkstyle-idea.properties  # Supported versions config
│   ├── csaccess/                # Checkstyle version isolation layer
│   │   └── java/org/infernus/idea/checkstyle/service/
│   ├── csaccessTest/            # Tests for csaccess layer
│   ├── test/                    # Main test sources
│   └── ...
├── buildSrc/                    # Custom Gradle plugin for build logic
│   └── src/main/java/org/infernus/idea/checkstyle/build/
│       ├── GradlePluginMain.java           # Main plugin class
│       ├── CheckstyleVersions.java         # Reads supported versions
│       ├── CustomSourceSetCreator.java     # Creates csaccess sourcesets
│       ├── GatherCheckstyleArtifactsTask.java  # Bundles Checkstyle JARs
│       └── CsaccessTestTask.java           # Cross-version testing
├── build.gradle.kts             # Main build configuration
├── gradle.properties            # Gradle settings (configuration-cache enabled)
├── settings.gradle.kts          # Gradle settings
├── mise.toml                    # Mise tool version (Java liberica-21)
├── CHANGELOG.md                 # Version history
└── README.md                    # Project documentation
```

## Essential Commands

### Build & Test

```bash
# Clean build
./gradlew clean build

# Run tests (includes unit tests and csaccess tests)
./gradlew test

# Run csaccess tests against base Checkstyle version only
./gradlew csaccessTest

# Run csaccess tests against ALL supported Checkstyle versions (cross-version testing)
./gradlew xTest

# Generate code coverage report for csaccess
./gradlew jacocoCsaccessReport

# Verify coverage meets minimum threshold (60% for csaccess)
./gradlew jacocoCsaccessCoverageVerification
```

### Development & Debugging

```bash
# Run IDEA with the plugin in a sandbox
./gradlew runIde

# Build the plugin ZIP for distribution
./gradlew buildPlugin

# Verify plugin structure and compatibility
./gradlew verifyPlugin

# Build searchable options index
./gradlew buildSearchableOptions
```

### IntelliJ Platform Tasks

```bash
# Prepare sandbox with plugin and dependencies
./gradlew prepareSandbox
./gradlew prepareTestSandbox

# Copy Checkstyle artifacts to sandbox
./gradlew copyCheckstyleArtifactsToSandbox
./gradlew copyCheckstyleArtifactsToTestSandbox

# Copy csaccess classes to sandbox
./gradlew copyClassesToSandbox
./gradlew copyClassesToTestSandbox

# Patch plugin.xml with build information
./gradlew patchPluginXml
```

### Publishing (requires token)

```bash
# Publish to JetBrains Plugin Repository
./gradlew publishPlugin
# Requires: JETBRAINS_PLUGIN_REPO_TOKEN environment variable
```

### List All Tasks

```bash
./gradlew tasks --all
```

## Build Configuration

### Java Version Requirements

- **Java 21** is required (both for compilation and runtime)
- Uses Java toolchain with `languageVersion = 21`
- Source/target compatibility: Java 21
- Minimum IDEA runtime: Java 21

### Gradle Configuration

- Uses Gradle configuration cache (enabled in `gradle.properties`)
- Uses Gradle wrapper (run `./gradlew` not `gradle`)
- Custom Gradle plugin defined in `buildSrc/`
- IntelliJ Platform Gradle Plugin 2.10.5

### Dependencies

**Main dependencies:**
- IntelliJ IDEA Community 2024.3.7
- Bundled Java plugin (`com.intellij.java`)
- commons-io 2.20.0
- commons-codec 1.19.0
- Checkstyle (base version 10.0 for compilation)

**Test dependencies:**
- JUnit 4.13.2
- Hamcrest 3.0
- Mockito 5.18.0

## Custom Source Sets

This project uses **custom Gradle source sets** for Checkstyle version isolation:

### `csaccess` Source Set

**Purpose:** Isolates code that directly interacts with Checkstyle APIs. This code is loaded in separate classloaders for each Checkstyle version.

**Location:** `src/csaccess/java/`

**Key files:**
- `CheckstyleActionsImpl.java` - Implements `CheckstyleActions` interface
- Various command classes in `service/cmd/` package

**Dependencies:** Compiled against Checkstyle base version (10.0)

### `csaccessTest` Source Set

**Purpose:** Tests for the csaccess layer that can run against multiple Checkstyle versions.

**Location:** `src/csaccessTest/java/`

**How it works:**
- Tests are run via `CsaccessTestTask`
- Default task runs against base version only
- `xTest` task runs against ALL supported Checkstyle versions
- Each version gets its own task (e.g., `csaccessTest_cs_13.0.0`)

### Coverage Requirements

- csaccess source set has minimum 60% test coverage requirement
- Verified via `jacocoCsaccessCoverageVerification` task

## Checkstyle Version Management

### Supported Versions

Defined in `src/main/resources/checkstyle-idea.properties`:

```properties
checkstyle.versions.supported = \
    10.0, 10.1, 10.2, 10.3.4, 10.4, 10.5.0, 10.6.0, 10.7.0, 10.8.1, 10.9.3, \
    10.10.0, 10.12.7, 10.13.0, 10.14.2, 10.15.0, 10.16.0, 10.17.0, 10.18.2, 10.19.0, \
    10.20.2, 10.21.3, 10.22.0, 10.23.0, 10.24.0, 10.25.1, 10.26.1, \
    11.0.1, 11.1.0, \
    12.0.1, 12.1.2, 12.2.0, 12.3.1, \
    13.0.0

baseVersion = 10.0
```

### Version Mapping

Unsupported versions are automatically mapped to supported alternatives:

```properties
checkstyle.versions.map = \
    8.0 -> 10.0, 8.1 -> 10.0, ...
    9.0 -> 10.0, 9.2 -> 10.0
```

### Adding New Checkstyle Versions

1. Add version to `checkstyle.versions.supported` in `checkstyle-idea.properties`
2. Run `./gradlew gatherCheckstyleArtifacts` to download dependencies
3. Run `./gradlew xTest` to verify compatibility across versions
4. Update CHANGELOG.md
5. Update plugin.xml change-notes if needed

### How Version Isolation Works

1. `GatherCheckstyleArtifactsTask` downloads all Checkstyle versions and their dependencies
2. Artifacts are bundled in `build/checkstyle/lib/`
3. At runtime, `CheckstyleClassLoaderContainer` creates separate classloaders per version
4. Each classloader loads `CheckstyleActionsImpl` from the csaccess classes
5. Commands are executed in the context of the specific Checkstyle version

## Code Style & Conventions

### Package Organization

- **Package prefix:** `org.infernus.idea.checkstyle`
- **No wildcard imports:** Imports are explicit
- **Standard IntelliJ annotations:** `@NotNull`, `@Nullable`, `@Override`

### Naming Conventions

- Classes use PascalCase
- Methods use camelCase
- Constants use UPPER_SNAKE_CASE
- Test classes end with `Test`
- Action classes often end with action name (e.g., `AnalyseCurrentFile`)

### IntelliJ-Specific Patterns

#### Services

Project services are defined in `plugin.xml`:

```xml
<projectService serviceImplementation="org.infernus.idea.checkstyle.CheckstyleProjectService"/>
<projectService serviceImplementation="org.infernus.idea.checkstyle.StaticScanner"/>
```

Access via:
```java
project.getService(CheckstyleProjectService.class)
```

#### Actions

Actions extend IntelliJ action classes:

```java
public class AnalyseCurrentFile extends ScanCurrentFile {
    @Override
    protected VirtualFile selectedFile(Project project, AnActionEvent event) {
        return event.getDataContext().getData(VIRTUAL_FILE);
    }
    
    @Override
    public void update(AnActionEvent event) {
        // Update presentation based on context
    }
}
```

Registered in `plugin.xml`:

```xml
<action id="AnalyseCurrentFileAction"
        class="org.infernus.idea.checkstyle.actions.AnalyseCurrentFile"
        text="Scan with Checkstyle..."
        description="...">
    <add-to-group group-id="AnalyzeMenu" anchor="last"/>
</action>
```

#### Configuration

- Project config: `ProjectConfigurationState` (persistent)
- Module config: `ModuleConfigurationState` (persistent)
- UI: `CheckStyleConfigurable` implements `Configurable`

#### Tool Window

```xml
<toolWindow id="CheckStyle"
            anchor="bottom"
            canCloseContents="false"
            factoryClass="org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowFactory"
            icon="/org/infernus/idea/checkstyle/images/checkstyle.svg"/>
```

### Error Handling

Custom exceptions inherit from `CheckStylePluginException`:
- `CheckStylePluginParseException`
- `CheckstyleServiceException`
- `CheckstyleToolException`

Logging:
```java
private static final Logger LOG = Logger.getInstance(ClassName.class);
LOG.debug("Debug message");
LOG.warn("Warning message", throwable);
```

Enable debug logging in IDEA via **Help** → **Debug Log Settings**:
```
#org.infernus.idea.checkstyle
```

## Testing Patterns

### Unit Tests

- Framework: JUnit 4
- Assertions: Hamcrest matchers
- Mocking: Mockito

Example:
```java
@Test
public void testSomething() {
    // Arrange
    Project project = Mockito.mock(Project.class);
    Mockito.when(project.getName()).thenReturn("test");
    
    // Act
    String result = underTest.doSomething(project);
    
    // Assert
    Assert.assertEquals("expected", result);
}
```

### Test Configuration

```kotlin
tasks.withType<Test> {
    setForkEvery(1)           // Fork new JVM for each test
    jvmArgs("-Xshare:off")    // Disable class data sharing
}
```

Test logging configured to show:
- Failed tests only
- Stack traces
- Full exception format

### Test Resources

- Test configs in `src/test/resources/`
- Empty files for edge case testing
- Broken properties files for error handling tests

## Build System Details

### buildSrc Custom Plugin

The `buildSrc/` directory contains a custom Gradle plugin that handles:

1. **Custom Source Sets**
   - Creates `csaccess` and `csaccessTest` source sets
   - Configures dependencies and compilation

2. **Checkstyle Artifact Gathering**
   - `GatherCheckstyleArtifactsTask` downloads all supported Checkstyle versions
   - Generates `checkstyle-classpaths.properties` mapping versions to JARs
   - Copies artifacts to sandbox directories

3. **Cross-Version Testing**
   - Creates test tasks for each Checkstyle version
   - `xTest` aggregates all cross-version tests
   - Each task name: `csaccessTest_cs_<version>`

4. **Coverage Verification**
   - JaCoCo coverage for csaccess source set
   - Minimum 60% coverage requirement

5. **IntelliJ Plugin Integration**
   - Wires custom tasks into IntelliJ Platform plugin lifecycle
   - Copies classes and artifacts to sandbox before running/testing

### Important Build Tasks Flow

**For development:**
```
runIde
  ← prepareSandbox
    ← copyCheckstyleArtifactsToSandbox
      ← gatherCheckstyleArtifacts
    ← copyClassesToSandbox
      ← csaccessClasses
```

**For testing:**
```
test
  ← csaccessTest (base version only)
    ← copyCheckstyleArtifactsToTestSandbox
    ← copyClassesToTestSandbox

check
  ← test
  ← xTest (all versions)
    ← csaccessTest_cs_10.1
    ← csaccessTest_cs_10.2
    ← ...
```

**For building plugin:**
```
buildPlugin
  ← prepareSandbox
  ← buildSearchableOptions
  ← jarSearchableOptions
```

## Plugin Descriptor (plugin.xml)

Location: `src/main/resources/META-INF/plugin.xml`

**Key elements:**

- **Dependencies:** Requires platform, lang, xml, vcs, and java modules
- **Services:** Project and module services for state and functionality
- **Extensions:** Inspections, tool windows, config panels, importers
- **Actions:** Menu items for scanning files
- **Startup Activities:** Disables Checkstyle's internal logging

**Plugin ID:** `CheckStyle-IDEA` (must match `build.gradle.kts`)

**Versioning:** Set in `build.gradle.kts`, patched into plugin.xml during build

## Common Development Tasks

### Debug the Plugin in IDEA

1. Import project into IntelliJ IDEA as Gradle project
2. Run Gradle task `runIde` in debug mode
3. Or create Run Configuration:
   - Type: Gradle
   - Task: `runIde`
   - Debug as needed

### Add a New Action

1. Create class extending appropriate IntelliJ action base class
2. Register in `plugin.xml` under `<actions>`
3. Add to relevant action group
4. Implement `actionPerformed()` and `update()` methods

### Add a New Project Service

1. Create service class with project-level logic
2. Register in `plugin.xml`:
   ```xml
   <projectService serviceImplementation="com.example.MyService"/>
   ```
3. Access via `project.getService(MyService.class)`

### Modify Configuration UI

1. UI panels are in `ui/` package
2. Main configuration: `CheckStyleConfigurable`
3. Panels: `CompletePanel`, `LocationPanel`, `PropertiesPanel`, etc.
4. State persisted via `ProjectConfigurationState`

### Change Supported IDEA Versions

1. Update version in `build.gradle.kts`:
   ```kotlin
   intellijIdeaCommunity("2024.3.7")
   ```
2. Update README and CHANGELOG
3. Test thoroughly with new IDEA version
4. Update comment in `GradlePluginMain.java` if version changed

## Release Process

Based on `.github/workflows/release.yml`:

1. **Tag the release:**
   ```bash
   git tag 26.0.0
   git push origin 26.0.0
   ```

2. **GitHub Actions automatically:**
   - Builds the plugin (`./gradlew build buildPlugin`)
   - Extracts changelog for this version from CHANGELOG.md
   - Creates GitHub release with ZIP artifact
   - Publishes to JetBrains Plugin Repository (`./gradlew publishPlugin`)

3. **Manual steps:**
   - Ensure CHANGELOG.md is updated before tagging
   - Version in `build.gradle.kts` should match tag
   - Update plugin.xml change-notes if needed

## CI/CD

**CI Workflow:** `.github/workflows/ci.yml`
- Runs on: push to `main`, pull requests to `main`
- Java: Liberica JDK 21
- Command: `./gradlew build`

**Release Workflow:** `.github/workflows/release.yml`
- Runs on: tag push
- Builds, creates GitHub release, publishes to JetBrains marketplace

## Important Files to Know

| File | Purpose |
|------|---------|
| `checkstyle-idea.properties` | Supported Checkstyle versions and mappings |
| `plugin.xml` | Plugin descriptor (services, actions, extensions) |
| `build.gradle.kts` | Main build configuration |
| `buildSrc/src/main/java/.../GradlePluginMain.java` | Custom Gradle plugin entry point |
| `CheckstyleProjectService.java` | Main service coordinating Checkstyle integration |
| `CheckstyleClassLoaderContainer.java` | Manages version-specific classloaders |
| `CheckstyleActionsImpl.java` (csaccess) | Version-isolated Checkstyle operations |
| `StaticScanner.java` | Orchestrates file scanning |
| `CheckStyleInspection.java` | IntelliJ inspection implementation |

## Gotchas & Non-Obvious Patterns

### 1. Classloader Isolation

The plugin loads multiple Checkstyle versions simultaneously. Code in `csaccess/` is compiled once but loaded in different classloaders. Be careful about:
- Static state (will be duplicated per classloader)
- Class identity checks (same class in different loaders ≠ equal)
- Serialization across classloader boundaries

### 2. Test Forking

Tests fork a new JVM for each test class (`setForkEvery(1)`). This prevents state leakage but:
- Makes tests slower
- Each test gets fresh IDEA environment
- Cannot share state between tests

### 3. Configuration Cache

Gradle configuration cache is enabled. When modifying build logic:
- Clean build cache: `./gradlew clean --no-configuration-cache`
- Some dynamic configuration may need adjustment

### 4. Sandbox Directory

The plugin runs in a sandbox during development:
- Location: `build/idea-sandbox/`
- Contains separate directories for run vs test
- Checkstyle artifacts copied there by custom tasks
- Not cleaned automatically—delete manually if needed

### 5. Resource Bundling

Resources from `src/main/resources/` are bundled into the plugin JAR. Large files or many files increase plugin size. Checkstyle JARs are bundled separately in the plugin's `lib/` directory.

### 6. Plugin Restart Required

The plugin is marked `require-restart="true"` in plugin.xml. Changes to the plugin require IDEA restart; hot reload is not supported.

### 7. Version Compatibility

When supporting new IDEA versions:
- API changes may require code updates
- Bundled plugins may change
- Test thoroughly with new version
- Check deprecation warnings during compilation

### 8. Base Directory and Variables

The plugin supports Eclipse-CS variables:
- `basedir` → module file location or project directory
- `project_loc`, `workspace_loc` → project directory
- `config_loc`, `samedir` → rules file directory

These are resolved at runtime and passed to Checkstyle.

### 9. Property Substitution

Configuration files may contain properties like `${propertyName}`. The plugin:
- Allows user to define properties in UI
- Substitutes them before passing config to Checkstyle
- Does not validate if all required properties are set (Checkstyle does this)

### 10. Third-Party Checks

Users can add third-party check JARs (e.g., sevntu, checkstyle-addons):
- JARs added to Checkstyle's classpath
- Loaded in same classloader as Checkstyle version
- Must be compatible with user's Checkstyle version

## Debugging Tips

### Enable Plugin Debug Logging

In IDEA: **Help** → **Debug Log Settings** → Add:
```
#org.infernus.idea.checkstyle
```

Check `idea.log` for debug output.

### Inspect Sandbox

Check the sandbox directory:
```bash
ls -la build/idea-sandbox/IC-2024.3.7/plugins/checkstyle-idea/
```

Verify:
- Plugin classes are present
- Checkstyle JARs are in `checkstyle/lib/`
- csaccess classes are in `checkstyle/classes/`

### Test a Specific Checkstyle Version

Run csaccess tests for a specific version:
```bash
./gradlew csaccessTest_cs_13.0.0
```

### Check Classloader Issues

If Checkstyle version detection fails:
- Check `checkstyle-classpaths.properties` in resources
- Verify JARs are downloaded: `build/checkstyle/lib/`
- Enable debug logging to see classloader creation

### Build Plugin Locally

```bash
./gradlew buildPlugin
ls -lh build/distributions/checkstyle-idea-*.zip
```

Install manually in IDEA: **Settings** → **Plugins** → **⚙️** → **Install Plugin from Disk**

## Performance Considerations

- **File Scanning:** Plugin scans files on-demand or continuously (inspection)
- **Classloader Caching:** Classloaders are cached per Checkstyle version to avoid repeated loading
- **Module vs Project:** Checkstyle configuration can be per-module; use module context when available
- **Background Threads:** Scanning happens on background threads; use IntelliJ's threading APIs correctly

## Resources

- **Plugin Repository:** https://github.com/jshiell/checkstyle-idea
- **Issue Tracker:** GitHub Issues
- **Checkstyle Docs:** https://checkstyle.sourceforge.io
- **IntelliJ Platform SDK:** https://plugins.jetbrains.com/docs/intellij/

## Contributing

When making changes:

1. Follow existing code style (see IntelliJ's Java code style settings)
2. Add tests for new functionality
3. Update CHANGELOG.md with changes
4. Ensure `./gradlew build` passes
5. Run `./gradlew xTest` if changing csaccess code
6. Test with actual IDEA instance (`./gradlew runIde`)
7. Update this AGENTS.md if adding new patterns or changing structure

## Summary for Agents

**Most important things to know:**

1. Use `./gradlew` not `gradle`
2. Java 21 required
3. Run `./gradlew runIde` to test plugin
4. Custom source sets: `csaccess` isolates Checkstyle version-specific code
5. Supported Checkstyle versions defined in `checkstyle-idea.properties`
6. Tests fork per class; csaccess tests can run against all Checkstyle versions
7. Plugin built with IntelliJ Platform Gradle Plugin 2.10.5
8. Custom Gradle plugin in `buildSrc/` handles special build logic
9. Enable debug logging: `#org.infernus.idea.checkstyle`
10. Plugin requires IDEA restart after installation/update

## Known Non-Issues

The following patterns have been investigated and confirmed to be working correctly. Do not re-raise them as bugs.

### `ConfigurationLocation.resolve()` — `InputStream.reset()` without `markSupported()` check
**File:** `src/main/java/org/infernus/idea/checkstyle/model/ConfigurationLocation.java`

`resolve()` calls `is.reset()` on the stream after extracting properties, but does **not** call `is.mark()` first. This looks like a bug but is intentional: the `reset()` call is wrapped in a `try/catch (IOException)`, and the catch block recovers by calling `resolveFile()` again to obtain a fresh stream. The comment in the catch block says "JAR IS doesn't support this, for instance". Both paths (reset succeeds / reset fails and stream is re-opened) produce a valid, readable stream for the caller.

### `StaticScanner.checksInProgress` — potential unbounded growth
**File:** `src/main/java/org/infernus/idea/checkstyle/StaticScanner.java`

`checksInProgress` is a `Set<Future<?>>` used to track in-flight scans. It appears that completed futures could accumulate, but every exit path from `ScanFiles.call()` — including the catch-all `Throwable` handler — fires either `scanCompletedSuccessfully()` or `scanFailedWithError()` on all listeners, which triggers `ScanCompletionTracker.checkComplete()` and removes the future from the set. Additionally, `checkInProgress()` guards against inserting an already-done future. There is no leak.

### `CheckerFactory` — `blockAndShow*` method proliferation
**File:** `src/main/java/org/infernus/idea/checkstyle/checker/CheckerFactory.java`

`blockAndShowMessage()` and `blockAndShowException()` appear repetitive but already share their common structure via the `blockAnd()` helper. Their bodies differ meaningfully (message-key notification vs exception display/rethrow). `blockAndShowMessageFromException()` is a dispatcher with distinct logic. There is no useful consolidation to be made.

### `CheckStyleInspection` — nested thread inside `checkFile()`
**File:** `src/main/java/org/infernus/idea/checkstyle/CheckStyleInspection.java`

`checkFile()` calls `asyncResultOf()`, which submits work to a second pooled thread and polls with `ProgressManager.checkCanceled()` every 50ms. This is intentional: Checkstyle's scan is a blocking, non-cooperative operation with no cancellation hooks. The polling loop on the calling thread is the only way to honour IDEA's cancellation contract and enforce the 5-second timeout. Removing it would make inspections uncancellable and unbounded in duration.

### `VirtualFileVisitor.visitFile()` — `super.visitFile()` not called
**File:** `src/main/java/org/infernus/idea/checkstyle/checker/ScanFiles.java`

`FindChildFiles.visitFile()` does not call `super.visitFile()`. The base class `visitFile()` simply returns `true` (continue traversal) with no side effects. The override already returns `true` explicitly. Calling super would be a no-op. The suggestion to return `CONTINUE` instead is based on a misreading of the API: `CONTINUE` is a `VirtualFileVisitor.Result` used by `visitFileEx()`, not by the `boolean visitFile()` override used here.

### `setForkEvery(1)` — test suite forking
**File:** `build.gradle.kts`

`setForkEvery(1)` is not set anywhere in the build. The `withType<Test>` block at line 59 only sets `jvmArgs("-Xshare:off")` and `useJUnitPlatform()`. There is no per-class JVM forking.

### `CheckerFactoryCacheTest` — 8 pre-existing test failures
**File:** `src/test/java/org/infernus/idea/checkstyle/checker/CheckerFactoryCacheTest.java`

All 8 tests in this class fail with `IllegalStateException: @NotNull method DependencyValidationManager.getInstance must not return null`. This occurs because the test creates a `StringConfigurationLocation`, whose constructor calls `NamedScopeHelper.getDefaultScope(project)` → `DependencyValidationManager.getInstance(project)`, which returns null in a plain JUnit context that does not have the full IntelliJ platform services loaded. These failures pre-exist and are not regressions. The tests require a `BasePlatformTestCase` environment or a mock-based approach that avoids constructing `ConfigurationLocation` directly.

### `PsiFileValidator.isInNamedScopeIfPresent()` — named scope stream semantics
**File:** `src/main/java/org/infernus/idea/checkstyle/checker/PsiFileValidator.java`

The method's Javadoc states "If no NamedScope is provided, true will be returned." The original implementation used `flatMap(Optional::stream).anyMatch(...)`: when all active locations have `namedScope = null` (which occurs when `DependencyValidationManager.getScope("All")` returns null in some environments), the stream is empty and `anyMatch` returns `false`, contradicting the documented contract. This was a real bug, fixed by collecting non-empty scopes first and returning `true` if that collection is empty.
