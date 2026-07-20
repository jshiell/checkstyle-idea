# Proposed Change: IDE-Wide Checkstyle XML Rules

## Short answer

**Yes** — this codebase can be modified so one Checkstyle XML configuration is set once in IDE settings and used by all projects in the same IDE installation.

## Current limitation

- Rules are currently persisted at **project scope** in `ProjectConfigurationState` (`checkstyle-idea.xml` in project config).
- Runtime consumers read via `PluginConfigurationManager` (project service), so effective rule locations are project-owned by design.
- Application scope exists (`ApplicationConfigurationState`) but currently stores only artifact download override.

## Proposed implementation direction (no code yet)

## 1) Add application-level global rules model

Extend `ApplicationConfigurationState` to persist a new global rules payload, for example:
- global rules locations (same location model shape as project config)
- active global location IDs
- optional global checkstyle version default
- optional flag: “Use global rules for all projects by default”

Storage remains IDE-wide (`checkstyle-idea-app.xml`).

## 2) Add UI in application settings

Extend `CheckStyleApplicationConfigurable` with a rules editor panel (can reuse/adapt `CheckStyleConfigPanel` behavior):
- add/edit/remove rule locations
- select active global location(s)
- validate/preview where appropriate

This becomes the one-time setup point in IDE settings.

## 3) Resolve effective configuration with precedence

Introduce deterministic precedence in config resolution:
1. module override (if set)
2. project override (if enabled and set)
3. application global rules (fallback/default)

Primary integration points:
- `ConfigurationLocationSource`
- `PluginConfigurationManager.getCurrent()` assembly path (or a dedicated merged/effective config resolver service)

## 4) Backward compatibility and migration

- Keep existing project config behavior valid.
- New installs can default to global-mode on.
- Existing projects can either:
  - keep project-local rules unchanged, or
  - opt into global rules.

## 5) Runtime impact considerations

- `CheckstyleProjectService` is project-scoped and should continue to be, but should read effective merged configuration.
- Global configuration changes should trigger per-project cache invalidation (existing `ConfigurationInvalidator` pattern can be extended).

## Scope clarification

This proposal satisfies: **all projects in one IDE installation**.

It does **not** by itself synchronize rules across multiple IDE installations/machines. Cross-install sync would need additional export/import or JetBrains Settings Sync integration.
