package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class OperatorWrapImporter extends ModuleImporter {

    private static final String OPTION_PROP = "option";

    // Checkstyle defaults option to "nl"
    private boolean binaryOperationSignOnNextLine = true;

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if (OPTION_PROP.equals(attrName)) {
            binaryOperationSignOnNextLine = "nl".equals(attrValue);
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getCommonSettings(settings);
        javaSettings.BINARY_OPERATION_SIGN_ON_NEXT_LINE = binaryOperationSignOnNextLine;
    }
}
