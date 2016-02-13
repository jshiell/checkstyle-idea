package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class NeedBracesImporter extends ModuleImporter {
    private static final String ALLOW_SINGLE_LINE_STATEMENT_PROP = "allowSingleLineStatement";

    private int forceBraces = CommonCodeStyleSettings.FORCE_BRACES_ALWAYS;

    @Override
    protected boolean handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if (ALLOW_SINGLE_LINE_STATEMENT_PROP.equals(attrName)) {
            if (Boolean.parseBoolean(attrValue)) {
                forceBraces = CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE;
            }
            return true;
        }
        return super.handleAttribute(attrName, attrValue);
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (appliesTo(TokenTypes.LITERAL_DO)) {
            javaSettings.DOWHILE_BRACE_FORCE = forceBraces;
        }
        if (appliesTo(TokenTypes.LITERAL_FOR)) {
            javaSettings.FOR_BRACE_FORCE = forceBraces;
        }
        if (appliesTo(TokenTypes.LITERAL_IF)) {
            javaSettings.IF_BRACE_FORCE = forceBraces;
        }
        if (appliesTo(TokenTypes.LITERAL_WHILE)) {
            javaSettings.WHILE_BRACE_FORCE = forceBraces;
        }
    }
}
