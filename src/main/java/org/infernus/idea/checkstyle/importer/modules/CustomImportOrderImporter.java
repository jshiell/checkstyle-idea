package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.psi.codeStyle.PackageEntry;
import com.intellij.psi.codeStyle.PackageEntryTable;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CustomImportOrderImporter extends ModuleImporter {
    private static final String CUSTOM_IMPORT_ORDER_RULES_PROP = "customImportOrderRules";
    private static final String STANDARD_PACKAGE_REG_EXP_PROP = "standardPackageRegExp";
    private static final String THIRD_PARTY_PACKAGE_REG_EXP_PROP = "thirdPartyPackageRegExp";
    private static final String SPECIAL_IMPORTS_REG_EXP_PROP = "specialImportsRegExp";
    private static final String SEPARATE_LINE_BETWEEN_GROUPS_PROP = "separateLineBetweenGroups";
    private static final String SORT_IMPORTS_IN_GROUP_ALPHABETICALLY_PROP = "sortImportsInGroupAlphabetically";
    private static final String SEVERITY = "severity";
    private static final String IMPORT_GROUP_SEPARATOR = "###";

    private List<ImportGroup> customImportOrder;
    private boolean separateLineBetweenGroups;
    private boolean sortImportsInGroupAlphabetically;
    private List<String> standardPackageRegExp;
    private List<String> thirdPartyPackageRegExp;
    private List<String> specialImportsRegExp;

    public CustomImportOrderImporter() {
        standardPackageRegExp = new ArrayList<>();
        standardPackageRegExp.add("javax");
        standardPackageRegExp.add("java");

        thirdPartyPackageRegExp = new ArrayList<>();

        specialImportsRegExp = new ArrayList<>();
    }

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        switch (attrName) {
            case CUSTOM_IMPORT_ORDER_RULES_PROP:
                parseImportOrderRules(attrValue);
                break;
            case STANDARD_PACKAGE_REG_EXP_PROP:
                standardPackageRegExp = parseCustomPackagesRegExp(attrValue);
                break;
            case THIRD_PARTY_PACKAGE_REG_EXP_PROP:
                thirdPartyPackageRegExp = parseCustomPackagesRegExp(attrValue);
                break;
            case SPECIAL_IMPORTS_REG_EXP_PROP:
                specialImportsRegExp = parseCustomPackagesRegExp(attrValue);
                break;
            case SEPARATE_LINE_BETWEEN_GROUPS_PROP:
                separateLineBetweenGroups = Boolean.parseBoolean(attrValue);
                break;
            case SORT_IMPORTS_IN_GROUP_ALPHABETICALLY_PROP:
                sortImportsInGroupAlphabetically = Boolean.parseBoolean(attrValue);
                break;
            case SEVERITY:
                // ignored, but valid
                break;
            default:
                String message = "No CustomImportOrder property with name " + attrName + ".";
                throw new UnknownPropertyException(message);
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        JavaCodeStyleSettings customSettings = settings.getCustomSettings(JavaCodeStyleSettings.class);
        customSettings.IMPORT_LAYOUT_TABLE.copyFrom(createCustomImportTable());
    }

    private PackageEntryTable createCustomImportTable() {
        PackageEntryTable table = new PackageEntryTable();
        Consumer<String> createPackageEntry = p -> table.addEntry(new PackageEntry(false, p, true));

        if (customImportOrder != null) {
            for (ImportGroup group : customImportOrder) {
                boolean entryAdded = false;
                switch (group) {
                    case STATIC:
                        table.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
                        entryAdded = true;
                        break;
                    case THIRD_PARTY_PACKAGE:
                        thirdPartyPackageRegExp.forEach(createPackageEntry);
                        entryAdded = thirdPartyPackageRegExp.size() > 0;
                        break;
                    case SPECIAL_IMPORTS:
                        specialImportsRegExp.forEach(createPackageEntry);
                        entryAdded = specialImportsRegExp.size() > 0;
                        break;
                    case STANDARD_JAVA_PACKAGE:
                        standardPackageRegExp.forEach(createPackageEntry);
                        entryAdded = standardPackageRegExp.size() > 0;
                        break;
                    default:
                        // IntelliJ does not support this option or group does not exist
                        break;
                }

                if (entryAdded && separateLineBetweenGroups) {
                    table.addEntry(PackageEntry.BLANK_LINE_ENTRY);
                }
            }
        }

        table.addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY);
        return table;
    }

    private List<String> parseCustomPackagesRegExp(@NotNull final String value) {
        String processedValue = value;

        String[] tokensToReplace = {"^", "$", ".*", "(", ")"};
        for (String token : tokensToReplace) {
            processedValue = processedValue.replace(token, "");
        }

        // The ending \. used in the regex value is not required in IntelliJ
        if (processedValue.endsWith("\\.")) {
            processedValue = processedValue.substring(0, processedValue.length() - 2);
        }

        String[] customPackages = processedValue.split("\\|");

        return Arrays.asList(customPackages);
    }

    private void parseImportOrderRules(@NotNull final String value) {
        String[] groups = value.split(IMPORT_GROUP_SEPARATOR);
        customImportOrder = Arrays.stream(groups)
                .map(group -> group.replaceAll("\\(\\d+\\)", ""))
                .map(ImportGroup::valueOf)
                .collect(Collectors.toList());
    }

    public enum ImportGroup {
        STATIC,
        SAME_PACKAGE,
        THIRD_PARTY_PACKAGE,
        STANDARD_JAVA_PACKAGE,
        SPECIAL_IMPORTS
    }

    public static class UnknownPropertyException extends RuntimeException {
        UnknownPropertyException(final String message) {
            super(message);
        }
    }
}
