package org.infernus.idea.checkstyle.model;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Properties the plugin supplies itself when running Checkstyle. They are never offered as editable
 * properties, though a non-blank stored value still overrides the built-in. Names are case-sensitive.
 */
public final class BuiltInProperties {

    public static final String BASEDIR = "basedir";
    public static final String PROJECT_LOC = "project_loc";
    public static final String WORKSPACE_LOC = "workspace_loc";
    public static final String CONFIG_LOC = "config_loc";
    public static final String SAMEDIR = "samedir";

    public static final Set<String> NAMES = Set.of(BASEDIR, PROJECT_LOC, WORKSPACE_LOC, CONFIG_LOC, SAMEDIR);

    private BuiltInProperties() {
    }

    public static boolean isBuiltIn(@NotNull final String propertyName) {
        return NAMES.contains(propertyName);
    }
}
