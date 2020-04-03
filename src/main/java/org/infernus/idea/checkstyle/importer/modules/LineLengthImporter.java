package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

import static com.intellij.psi.codeStyle.CommonCodeStyleSettings.WRAP_AS_NEEDED;

@SuppressWarnings("unused")
public class LineLengthImporter extends ModuleImporter {

    private static final int DEFAULT_MAX_COLUMNS = 80;
    private static final String MAX_PROP = "max";
    private int maxColumns = DEFAULT_MAX_COLUMNS;

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if (MAX_PROP.equals(attrName)) {
            try {
                maxColumns = Integer.parseInt(attrValue);
            } catch (NumberFormatException nfe) {
                // ignore
            }
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        settings.setRightMargin(JavaLanguage.INSTANCE, maxColumns);
        CommonCodeStyleSettings commonSettings = settings.getCommonSettings(JavaLanguage.INSTANCE);

        commonSettings.CALL_PARAMETERS_WRAP = WRAP_AS_NEEDED;
        commonSettings.METHOD_PARAMETERS_WRAP = WRAP_AS_NEEDED;
        commonSettings.RESOURCE_LIST_WRAP = WRAP_AS_NEEDED;
        commonSettings.EXTENDS_LIST_WRAP = WRAP_AS_NEEDED;
        commonSettings.THROWS_LIST_WRAP = WRAP_AS_NEEDED;
        commonSettings.EXTENDS_KEYWORD_WRAP = WRAP_AS_NEEDED;
        commonSettings.THROWS_KEYWORD_WRAP = WRAP_AS_NEEDED;
        commonSettings.METHOD_CALL_CHAIN_WRAP = WRAP_AS_NEEDED;
        commonSettings.BINARY_OPERATION_WRAP = WRAP_AS_NEEDED;
        commonSettings.TERNARY_OPERATION_WRAP = WRAP_AS_NEEDED;
        commonSettings.FOR_STATEMENT_WRAP = WRAP_AS_NEEDED;
        commonSettings.ARRAY_INITIALIZER_WRAP = WRAP_AS_NEEDED;
        commonSettings.ASSIGNMENT_WRAP = WRAP_AS_NEEDED;
        commonSettings.ASSERT_STATEMENT_WRAP = WRAP_AS_NEEDED;
        commonSettings.PARAMETER_ANNOTATION_WRAP = WRAP_AS_NEEDED;
        commonSettings.VARIABLE_ANNOTATION_WRAP = WRAP_AS_NEEDED;
        commonSettings.ENUM_CONSTANTS_WRAP = WRAP_AS_NEEDED;
    }
}
