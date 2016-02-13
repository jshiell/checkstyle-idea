package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class NoWhitespaceBeforeImporter extends ModuleImporter {

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (appliesTo(TokenTypes.COMMA)) {
            javaSettings.SPACE_BEFORE_COMMA = false;
        }
        if (appliesTo(TokenTypes.SEMI)) {
            javaSettings.SPACE_BEFORE_SEMICOLON = false;
        }
    }
}
