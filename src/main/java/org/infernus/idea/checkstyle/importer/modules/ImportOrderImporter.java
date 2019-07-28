package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.psi.codeStyle.PackageEntry;
import com.intellij.psi.codeStyle.PackageEntryTable;
import java.util.HashSet;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ImportOrderImporter extends ModuleImporter {

    private static final String GROUPS = "groups";
    private static final String SEPARATED = "separated";
    private static final String OPTION = "option";

    private String[] groups;
    private boolean separated = false;
    private StaticImportPosition staticPosition;

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        switch (attrName) {
            case GROUPS:
                groups = attrValue.split(",");
                break;
            case SEPARATED:
                separated = Boolean.parseBoolean(attrValue);
                break;
            case OPTION:
                staticPosition = StaticImportPosition.valueOf(attrValue.toUpperCase());
                break;
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        JavaCodeStyleSettings customSettings = settings.getCustomSettings(JavaCodeStyleSettings.class);

        if (groups != null) {
            processGroupsAttribute(groups, customSettings);
        }

        if (staticPosition == null) {
            processSeparatedAttribute(separated, customSettings);
        } else {
            if (staticPosition == StaticImportPosition.ABOVE || staticPosition == StaticImportPosition.UNDER) {
                // we are applying the attributes in reverse so that we do not separate blocks containing static and
                // non-static imports for the same package
                processSeparatedAttribute(separated, customSettings);
                processOptionAttribute(staticPosition, customSettings);
            } else {
                processOptionAttribute(staticPosition, customSettings);
                processSeparatedAttribute(separated, customSettings);
            }
        }
    }

    private static void processGroupsAttribute(@NotNull final String[] groups, @NotNull final JavaCodeStyleSettings settings) {
        PackageEntryTable importTable = new PackageEntryTable();

        for (String group : groups) {
            if (group.equals("*")) {
                importTable.addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY);
            } else {
                importTable.addEntry(new PackageEntry(false, group, true));
            }
        }
        settings.IMPORT_LAYOUT_TABLE.copyFrom(importTable);
    }

    private static void processSeparatedAttribute(final boolean separated, @NotNull final JavaCodeStyleSettings settings) {
        PackageEntryTable importTable = new PackageEntryTable();

        if (settings.IMPORT_LAYOUT_TABLE.getEntryCount() < 1) {
            // nothing to separate
            return;
        }

        for (PackageEntry entry : settings.IMPORT_LAYOUT_TABLE.getEntries()) {
            if (entry != PackageEntry.BLANK_LINE_ENTRY) {
                importTable.addEntry(entry);
                if (separated) {
                    importTable.addEntry(PackageEntry.BLANK_LINE_ENTRY);
                }
            }
        }

        // remove blank line at the very end if present
        if (importTable.getEntryAt(importTable.getEntryCount() - 1) == PackageEntry.BLANK_LINE_ENTRY) {
            importTable.removeEntryAt(importTable.getEntryCount() - 1);
        }

        settings.IMPORT_LAYOUT_TABLE.copyFrom(importTable);
    }

    private static void processOptionAttribute(@NotNull final StaticImportPosition staticImportPosition,
                                               @NotNull final JavaCodeStyleSettings settings) {
        switch (staticImportPosition) {
            case TOP:
                // remove other instances of PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY from the table
                // iterate from the end so we can remove elements without having to modify the index afterwards
                for (int x = settings.IMPORT_LAYOUT_TABLE.getEntryCount() - 1; x >= 0; x--) {
                    if (settings.IMPORT_LAYOUT_TABLE.getEntryAt(x) == PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY) {
                        settings.IMPORT_LAYOUT_TABLE.removeEntryAt(x);
                    }
                }
                settings.IMPORT_LAYOUT_TABLE.insertEntryAt(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY, 0);
                break;
            case BOTTOM:
                // remove other instances of PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY from the table
                // iterate from the end so we can remove elements without having to modify the index afterwards
                for (int x = settings.IMPORT_LAYOUT_TABLE.getEntryCount() - 1; x >= 0; x--) {
                    if (settings.IMPORT_LAYOUT_TABLE.getEntryAt(x) == PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY) {
                        settings.IMPORT_LAYOUT_TABLE.removeEntryAt(x);
                    }
                }
                settings.IMPORT_LAYOUT_TABLE.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
                break;
            case INFLOW:
                settings.LAYOUT_STATIC_IMPORTS_SEPARATELY = false;
                // remove all static imports, in-line with IntelliJ's behavior
                // iterate from the end so we can remove elements without having to modify the index afterwards
                for (int x = settings.IMPORT_LAYOUT_TABLE.getEntryCount() - 1; x >= 0; x--) {
                    if (settings.IMPORT_LAYOUT_TABLE.getEntryAt(x).isStatic()) {
                        settings.IMPORT_LAYOUT_TABLE.removeEntryAt(x);
                    }
                }
                break;
            case ABOVE:
            case UNDER:
                // If the table contains both a static and non-static entry for a given package the position of the
                // non-static import will be used for the import group.
                // To be able to decide whether we have to insert a non-static import when encountering a static import
                // we build an index here for all non-static imports.
                HashSet<String> nonStaticImports = new HashSet<>();
                for (PackageEntry entry : settings.IMPORT_LAYOUT_TABLE.getEntries()) {
                    if (!entry.isStatic()) {
                        nonStaticImports.add(entry.getPackageName());
                    }
                }

                PackageEntryTable importTable = new PackageEntryTable();

                for (PackageEntry entry : settings.IMPORT_LAYOUT_TABLE.getEntries()) {

                    if (entry == PackageEntry.BLANK_LINE_ENTRY) {
                        importTable.addEntry(entry);
                        continue;
                    }

                    // The following blocks basically all work the same way:
                    //
                    // If we encounter a non-static import we add it to the table along with a static import
                    // above or under it.
                    //
                    // If we encounter a static import we check whether a non-static import for the same package exists
                    // using the index we built earlier.
                    // If a non-static import exists we skip the current static import. It will be added later when we
                    // arrive at the non-static import.
                    // If no non-static import exists we add the current static import to the table along with a
                    // non-static import above or under it.
                    if (entry == PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY && !nonStaticImports.contains(PackageEntry.ALL_OTHER_IMPORTS_ENTRY.getPackageName())) {
                        if (staticImportPosition == StaticImportPosition.ABOVE) {
                            importTable.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
                            importTable.addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY);
                        } else {
                            importTable.addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY);
                            importTable.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
                        }
                    } else if (entry == PackageEntry.ALL_OTHER_IMPORTS_ENTRY) {
                        if (staticImportPosition == StaticImportPosition.ABOVE) {
                            importTable.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
                            importTable.addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY);
                        } else {
                            importTable.addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY);
                            importTable.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
                        }
                    } else if (entry.isStatic() && !nonStaticImports.contains(entry.getPackageName())) {
                        if (staticImportPosition == StaticImportPosition.ABOVE) {
                            importTable.addEntry(entry);
                            importTable.addEntry(new PackageEntry(false, entry.getPackageName(), true));
                        } else {
                            importTable.addEntry(new PackageEntry(false, entry.getPackageName(), true));
                            importTable.addEntry(entry);
                        }
                    } else { // non-static import
                        if (staticImportPosition == StaticImportPosition.ABOVE) {
                            importTable.addEntry(new PackageEntry(true, entry.getPackageName(), true));
                            importTable.addEntry(entry);
                        } else {
                            importTable.addEntry(entry);
                            importTable.addEntry(new PackageEntry(true, entry.getPackageName(), true));
                        }
                    }
                }
                settings.IMPORT_LAYOUT_TABLE.copyFrom(importTable);
                break;
        }
    }

    private enum StaticImportPosition {
        TOP,
        ABOVE,
        UNDER,
        INFLOW,
        BOTTOM
    }
}
