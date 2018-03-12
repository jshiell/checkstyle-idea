package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.PackageEntry;
import com.intellij.psi.codeStyle.PackageEntryTable;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CustomImportOrderImporter extends ModuleImporter {
    private static final String CUSTOM_IMPORT_ORDER_RULES_PROP = "customImportOrderRules";
    private static final String STANDARD_PACKAGE_REG_EXP_PROP = "standardPackageRegExp";
    private static final String THIRD_PARTY_PACKAGE_REG_EXP_PROP = "thirdPartyPackageRegExp";
    private static final String SPECIAL_IMPORTS_REG_EXP_PROP = "specialImportsRegExp";
    private static final String SEPARATE_LINE_BETWEEN_GROUPS_PROP = "separateLineBetweenGroups";
    private static final String SORT_IMPORTS_IN_GROUP_ALPHABETICALLY_PROP = "sortImportsInGroupAlphabetically";
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
        thirdPartyPackageRegExp.add("");

        specialImportsRegExp = new ArrayList<>();
        specialImportsRegExp.add("");
    }

    @Override
    protected void handleAttribute(@NotNull String attrName, @NotNull String attrValue) {
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
            default:
                String message = "No CustomImportOrder property with name " + attrName + ".";
                throw new UnknownPropertyException(message);
        }
    }

    @Override
    public void importTo(@NotNull CodeStyleSettings settings) {
        PackageEntryTable customImportTable = createCustomImportTable();
        settings.IMPORT_LAYOUT_TABLE.copyFrom(customImportTable);
    }

    private PackageEntryTable createCustomImportTable() {
        PackageEntryTable table = new PackageEntryTable();
        Consumer<String> createPackageEntry = p -> table.addEntry(new PackageEntry(false, p, true));

        if (customImportOrder != null) {
            for (ImportGroup group : customImportOrder) {
                switch (group) {
                    case STATIC:
                        table.addEntry(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY);
                        break;
                    case THIRD_PARTY_PACKAGE:
                        thirdPartyPackageRegExp.forEach(createPackageEntry);
                        break;
                    case SPECIAL_IMPORTS:
                        specialImportsRegExp.forEach(createPackageEntry);
                        break;
                    case STANDARD_JAVA_PACKAGE:
                        standardPackageRegExp.forEach(createPackageEntry);
                        break;
                    default:
                        // IntelliJ does not support this option or group does not exist
                        break;
                }

                addBlankLineBetweenGroups(table);
            }
        }

        table.addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY);
        return table;
    }

    private void addBlankLineBetweenGroups(PackageEntryTable table) {
        if(!separateLineBetweenGroups) {
          return;
        }

        table.addEntry(PackageEntry.BLANK_LINE_ENTRY);
    }

    private List<String> parseCustomPackagesRegExp(@NotNull String value) {
        String[] tokensToReplace = {"^", "$", ".*", "(", ")"};
        for(String token : tokensToReplace) {
            value = value.replace(token, "");
        }

        // The ending \. used in the regex value is not required in IntelliJ
        value = value.endsWith("\\.") ? value.substring(0, value.length() - 2) : value;

        String[] customPackages = value.split("\\|");

        return Arrays.asList(customPackages);
    }

    private void parseImportOrderRules(@NotNull String value) {
        String[] groups = value.split(IMPORT_GROUP_SEPARATOR);
        customImportOrder = Arrays.stream(groups).map(ImportGroup::valueOf).collect(Collectors.toList());
    }

    public enum ImportGroup {
        STATIC,
        SAME_PACKAGE,
        THIRD_PARTY_PACKAGE,
        STANDARD_JAVA_PACKAGE,
        SPECIAL_IMPORTS
    }

    public class UnknownPropertyException extends RuntimeException {
        UnknownPropertyException(String message) {
            super(message);
        }
    }
}
