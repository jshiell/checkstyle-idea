package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

/**
 * Test-only fixture, not a real Checkstyle module importer. Resolved via
 * {@code ModuleImporterFactory}'s reflection lookup for module name
 * "WarningOrderingFixture", it emits two distinct warnings in a fixed order so
 * {@code CodeStyleImporterTest} can prove that {@code CheckStyleCodeStyleImporter}
 * surfaces warnings in config order rather than an unspecified set order.
 */
public class WarningOrderingFixtureImporter extends ModuleImporter {

    public WarningOrderingFixtureImporter() {
        warn("first-warning");
        warn("second-warning");
    }

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
    }
}
