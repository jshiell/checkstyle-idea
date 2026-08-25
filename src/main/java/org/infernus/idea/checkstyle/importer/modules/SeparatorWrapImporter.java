package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;

import static com.intellij.psi.codeStyle.CommonCodeStyleSettings.WRAP_ALWAYS;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.csapi.KnownTokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class SeparatorWrapImporter extends ModuleImporter {

    private static final String OPTION_PROP = "option";

    // Checkstyle defaults option to "eol"
    private boolean placeOnNextLine = false;

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if (OPTION_PROP.equals(attrName)) {
            placeOnNextLine = "nl".equals(attrValue);
        }
    }

    @Override
    public void setFrom(@NotNull final ConfigurationModule moduleConfig) {
        super.setFrom(moduleConfig);
        warnIfUnsupported(moduleConfig, KnownTokenTypes.ELLIPSIS);
        warnIfUnsupported(moduleConfig, KnownTokenTypes.METHOD_REF);
        warnIfUnsupported(moduleConfig, KnownTokenTypes.SEMI);
        warnIfUnsupported(moduleConfig, KnownTokenTypes.ARRAY_DECLARATOR);
    }

    private void warnIfUnsupported(@NotNull final ConfigurationModule moduleConfig, @NotNull final KnownTokenTypes token) {
        if (moduleConfig.getKnownTokenTypes().contains(token)) {
            warn(CheckStyleBundle.message("import.separator-wrap.token-unsupported", token.name()));
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getCommonSettings(settings);
        if (appliesTo(KnownTokenTypes.DOT)) {
            // Approximate: Checkstyle's DOT token covers all dot separators, but IDEA only has a
            // setting for the first method in a call chain. DOT nl implies all calls wrap (dot on
            // new line), so METHOD_CALL_CHAIN_WRAP is set to WRAP_ALWAYS. DOT eol only specifies
            // dot placement when wrapping occurs; wrap mode is left unchanged.
            // Note: if LineLength appears after SeparatorWrap in the XML, LineLengthImporter will
            // overwrite METHOD_CALL_CHAIN_WRAP with WRAP_AS_NEEDED.
            javaSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN = placeOnNextLine;
            if (placeOnNextLine) {
                javaSettings.METHOD_CALL_CHAIN_WRAP = WRAP_ALWAYS;
            }
        }
        // COMMA is a no-op: IDEA has no generic "comma on next line" setting.
    }
}
