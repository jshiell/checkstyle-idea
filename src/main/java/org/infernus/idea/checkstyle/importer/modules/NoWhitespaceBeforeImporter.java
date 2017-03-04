package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.csapi.KnownTokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class NoWhitespaceBeforeImporter extends ModuleImporter {

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        // nothing to do
    }


    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (appliesTo(KnownTokenTypes.COMMA)) {
            javaSettings.SPACE_BEFORE_COMMA = false;
        }
        if (appliesTo(KnownTokenTypes.SEMI)) {
            javaSettings.SPACE_BEFORE_SEMICOLON = false;
        }
    }
}
