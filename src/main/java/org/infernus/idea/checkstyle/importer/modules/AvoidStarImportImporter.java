package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.PackageEntry;
import com.intellij.psi.codeStyle.PackageEntryTable;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class AvoidStarImportImporter extends ModuleImporter {

    private static final String EXCLUDES = "excludes";
    private static final String ALLOW_CLASS_STAR_IMPORT = "allowClassImports";
    private static final String ALLOW_STATIC_STAR_IMPORT = "allowStaticMemberImports";

    private boolean allowClassStarImports;
    private boolean allowStaticStarImports;
    private String[] excludes;

    @Override
    protected void handleAttribute(@NotNull String attrName, @NotNull String attrValue) {
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
        }

    }

    @Override
    public void importTo(@NotNull CodeStyleSettings settings) {
        if (!allowClassStarImports) {
            settings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 999;
        }

        if (!allowStaticStarImports) {
            settings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND = 999;
        }

        PackageEntryTable excludeTable = new PackageEntryTable();
        if (excludes != null) {
            for (String exclude : excludes) {
                excludeTable.addEntry(new PackageEntry(false, exclude, false));
            }
        }
        settings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.copyFrom(excludeTable);
    }
}
