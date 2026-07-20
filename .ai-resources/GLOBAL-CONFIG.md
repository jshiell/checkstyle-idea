# IDE-Wide Checkstyle XML Rules

## Status

Implemented.

Global Checkstyle rule locations can now be configured once at **IDE scope** and reused across projects in the same IDE installation.

## Behavior

1. Rules configured in **Settings > Tools > Checkstyle** (application-level) are stored in `ApplicationConfigurationState`.
2. Active global rules are used as fallback when project/module rules are not active.
3. Global rule locations are also exposed in the **Scan tool window -> Rules** override dropdown, so they can be selected directly for a scan.

## Precedence

Effective resolution order is:

1. Module override (if set)
2. Project active rules
3. IDE-wide active global rules (when enabled)

## Notes

- Scope is one IDE installation (not automatic cross-machine sync).
- Updating global settings invalidates project caches so open projects pick up the updated global rules and dropdown entries.
