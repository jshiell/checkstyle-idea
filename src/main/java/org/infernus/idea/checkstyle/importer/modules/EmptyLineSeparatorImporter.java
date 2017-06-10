package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.csapi.KnownTokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class EmptyLineSeparatorImporter extends ModuleImporter {

    private boolean noEmptyLinesBetweenFields = false;
    private static final String NO_EMPTY_LINES_BETWEEN_FIELDS_PROP = "allowNoEmptyLineBetweenFields";

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if (NO_EMPTY_LINES_BETWEEN_FIELDS_PROP.equals(attrName)) {
            noEmptyLinesBetweenFields = Boolean.parseBoolean(attrValue);
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (noEmptyLinesBetweenFields) {
            javaSettings.BLANK_LINES_AROUND_FIELD = 0;
        } else if (appliesTo(KnownTokenTypes.VARIABLE_DEF)) {
            javaSettings.BLANK_LINES_AROUND_FIELD = 1;
        }
        if (appliesTo(KnownTokenTypes.PACKAGE_DEF)) {
            javaSettings.BLANK_LINES_AFTER_PACKAGE = 1;
            javaSettings.BLANK_LINES_BEFORE_PACKAGE = 1;
        }
        if (appliesTo(KnownTokenTypes.IMPORT)) {
            javaSettings.BLANK_LINES_AFTER_IMPORTS = 1;
        }
        if (appliesTo(KnownTokenTypes.METHOD_DEF)) {
            javaSettings.BLANK_LINES_AROUND_METHOD = 1;
        }
    }
}
