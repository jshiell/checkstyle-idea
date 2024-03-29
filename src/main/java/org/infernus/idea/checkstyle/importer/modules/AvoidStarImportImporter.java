package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.psi.codeStyle.PackageEntry;
import com.intellij.psi.codeStyle.PackageEntryTable;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class AvoidStarImportImporter extends ModuleImporter {
    private static final Logger LOG = Logger.getInstance(AvoidStarImportImporter.class);

    private static final String EXCLUDES = "excludes";
    private static final String ALLOW_CLASS_STAR_IMPORT = "allowClassImports";
    private static final String ALLOW_STATIC_STAR_IMPORT = "allowStaticMemberImports";
    private static final int MAXIMUM_INPUTS = 999;

    private boolean allowClassStarImports;
    private boolean allowStaticStarImports;
    private String[] excludes;

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        switch (attrName) {
            case EXCLUDES:
                excludes = attrValue.split(",");
                break;
            case ALLOW_CLASS_STAR_IMPORT:
                allowClassStarImports = Boolean.parseBoolean(attrValue);
                break;
            case ALLOW_STATIC_STAR_IMPORT:
                allowStaticStarImports = Boolean.parseBoolean(attrValue);
                break;
            default:
                LOG.warn("Unexpected avoid star import policy: " + attrValue);
                break;
        }

    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        JavaCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (!allowClassStarImports) {
            javaSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = MAXIMUM_INPUTS;
        }

        if (!allowStaticStarImports) {
            javaSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND = MAXIMUM_INPUTS;
        }

        PackageEntryTable excludeTable = new PackageEntryTable();
        if (excludes != null) {
            for (String exclude : excludes) {
                excludeTable.addEntry(new PackageEntry(false, exclude, false));
            }
        }
        javaSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.copyFrom(excludeTable);
    }
}
