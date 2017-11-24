package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CustomImportOrderImporter extends ModuleImporter {
    private static final String CUSTOM_IMPORT_ORDER_RULES_PROP = "customImportOrderRules";
    private static final String STANDARD_PACKAGE_REG_EXP_PROP = "standardPackageRegExp";
    private static final String THIRD_PARTY_PACKAGE_REG_EXP_PROP = "thirdPartyPackageRegExp";
    private static final String SPECIAL_IMPORTS_REG_EXP_PROP = "specialImportsRegExp";
    private static final String SEPARATE_LINE_BETWEEN_GROUPS_PROP = "separateLineBetweenGroups";
    private static final String SORT_IMPORTS_IN_GROUP_ALPHABETICALLY_PROP = "sortImportsInGroupAlphabetically";

    private List<ImportGroup> customImportOrder;
    private boolean separateLineBetweenGroups;
    private boolean sortImportsInGroupAlphabetically;
    private Pattern standardPackageRegExp;
    private Pattern thirdPartyPackageRegExp;
    private Pattern specialImportsRegExp;


    @Override
    protected void handleAttribute(@NotNull String attrName, @NotNull String attrValue) {
        switch (attrName) {
            case CUSTOM_IMPORT_ORDER_RULES_PROP:
                parseImportOrderRules(attrValue);
                break;
            case STANDARD_PACKAGE_REG_EXP_PROP:
                standardPackageRegExp = Pattern.compile(attrValue);
                break;
            case THIRD_PARTY_PACKAGE_REG_EXP_PROP:
                thirdPartyPackageRegExp = Pattern.compile(attrValue);
                break;
            case SPECIAL_IMPORTS_REG_EXP_PROP:
                specialImportsRegExp = Pattern.compile(attrValue);
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
        return;
    }

    private void parseImportOrderRules(@NotNull String value) {
        String[] groups = value.split(value);
        customImportOrder = Arrays.stream(groups).map(ImportGroup::valueOf).collect(Collectors.toList());
    }

    public enum ImportGroup {
        STATIC,
        SAME_PACKAGE,
        THIRD_PARTY,
        STANDARD_JAVA_PACKAGE,
        SPECIAL_IMPORTS
    }

    public class UnknownPropertyException extends RuntimeException {
        UnknownPropertyException(String message) {
            super(message);
        }
    }
}
