# CheckStyle-IDEA: AI Onboarding Overview

## What this plugin is

CheckStyle-IDEA is a JetBrains plugin that runs Checkstyle in IntelliJ-based IDEs and surfaces results as:
- real-time inspection warnings in editor (`CheckStyleInspection`)
- manual/static scans via actions and tool window
- optional pre-commit scan integration

The plugin targets IntelliJ Platform 2024.3+ and uses Java 21 + Gradle.

## Core architecture

- **Plugin descriptor:** `src/main/resources/META-INF/plugin.xml`
  - Registers project services, one application service, module service, settings pages, inspection, tool window, actions.
- **Project-level config backbone:** `PluginConfigurationManager` + `ProjectConfigurationState`
  - Stores active rules locations, Checkstyle version, scan scope, classpath, and related flags.
- **Application-level config:** `ApplicationConfigurationState`
  - Currently only stores artifact repository base URL override for Checkstyle runtime downloads.
- **Module override layer:** `ModuleConfigurationState`
  - Optional per-module active rules override or exclusion from scanning.

## Runtime model (important)

- Checkstyle runtime access is handled by `CheckstyleProjectService`.
- `csaccess` code is loaded through separate classloaders per Checkstyle version (`CheckstyleClassLoaderContainer`).
- Classloader isolation is intentional: static state and class identity are per-loader.
- Most versions are supported; newer/bundled behavior is driven by `checkstyle-idea.properties`.

## Configuration model

- Main user config UI: **Settings > Tools > Checkstyle** (`CheckStyleConfigurable` + `CheckStyleConfigPanel`).
- Rules can come from local files, URLs, or bundled configs; multiple locations are supported with active toggles.
- Module settings can override project-active rules.
- Effective location resolution uses `ConfigurationLocationSource`.

## Scan flow (high level)

1. Configuration is read from `PluginConfigurationManager`.
2. Effective rules for file/module/project are resolved (`ConfigurationLocationSource` + module overrides).
3. `CheckerFactory`/scan pipeline runs Checkstyle via versioned classloader.
4. Results are published to inspection/tool window/actions.

## Build/test/release quick context

- Build: `./gradlew clean build`
- Unit + base csaccess tests: `./gradlew test`
- Cross-version csaccess tests: `./gradlew xTest`
- Launch sandbox IDE: `./gradlew runIde`
- Build plugin ZIP: `./gradlew buildPlugin`

## Repository landmarks

- Plugin code: `src/main/java/org/infernus/idea/checkstyle/`
- Plugin resources: `src/main/resources/`
- Version matrix + mapping: `src/main/resources/checkstyle-idea.properties`
- Checkstyle-isolated API layer: `src/csaccess/java/`
- Cross-version tests: `src/csaccessTest/java/`
- Custom Gradle build logic: `buildSrc/`

## Practical maintenance notes

- If adding a Checkstyle version: update `checkstyle-idea.properties`, run artifact gather + `xTest`, update changelog.
- Sandbox state can become stale: `build/idea-sandbox/` may need manual cleanup.
- Debug logging category: `#org.infernus.idea.checkstyle`.
- Plugin requires IDE restart (`require-restart="true"` in `plugin.xml`).
