package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.csapi.KnownTokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class NeedBracesImporter extends ModuleImporter {

    private static final String ALLOW_SINGLE_LINE_STATEMENT_PROP = "allowSingleLineStatement";

    private int forceBraces = CommonCodeStyleSettings.FORCE_BRACES_ALWAYS;

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if (ALLOW_SINGLE_LINE_STATEMENT_PROP.equals(attrName)) {
            if (Boolean.parseBoolean(attrValue)) {
                forceBraces = CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE;
            }
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (appliesTo(KnownTokenTypes.LITERAL_DO)) {
            javaSettings.DOWHILE_BRACE_FORCE = forceBraces;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_FOR)) {
            javaSettings.FOR_BRACE_FORCE = forceBraces;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_IF)) {
            javaSettings.IF_BRACE_FORCE = forceBraces;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_WHILE)) {
            javaSettings.WHILE_BRACE_FORCE = forceBraces;
        }
    }
}
