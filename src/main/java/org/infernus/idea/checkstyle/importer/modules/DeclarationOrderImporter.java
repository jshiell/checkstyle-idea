package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class DeclarationOrderImporter extends ModuleImporter {

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
    }
}
