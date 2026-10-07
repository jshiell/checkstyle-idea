package org.infernus.idea.checkstyle.model;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Properties the plugin supplies itself when running Checkstyle. They are never offered as editable
 * properties, though a non-blank stored value still overrides the built-in. Names are case-sensitive.
 */
public final class BuiltInProperties {

    public static final Set<String> NAMES = Set.of("basedir", "project_loc", "workspace_loc", "config_loc", "samedir");

    private BuiltInProperties() {
    }

    public static boolean isBuiltIn(@NotNull final String propertyName) {
        return NAMES.contains(propertyName);
    }
}
