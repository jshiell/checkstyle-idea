package org.infernus.idea.checkstyle.importer.modules;

import java.util.EnumSet;
import java.util.Set;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.csapi.KnownTokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class LeftCurlyImporter extends ModuleImporter {

    private static final Logger LOG = Logger.getInstance(LeftCurlyImporter.class);

    private static final String OPTION_PROP = "option";

    private static final String LEFT_CURLY_POLICY_EOL = "eol";
    private static final String LEFT_CURLY_POLICY_NL = "nl";
    private static final String LEFT_CURLY_POLICY_NLOW = "nlow";

    private int leftCurlyPolicy = CommonCodeStyleSettings.END_OF_LINE;

    private static final Set<KnownTokenTypes> CONDITIONAL_TOKENS = EnumSet.of(
            KnownTokenTypes.LITERAL_WHILE,
            KnownTokenTypes.LITERAL_TRY,
            KnownTokenTypes.LITERAL_CATCH,
            KnownTokenTypes.LITERAL_FINALLY,
            KnownTokenTypes.LITERAL_SYNCHRONIZED,
            KnownTokenTypes.LITERAL_SWITCH,
            KnownTokenTypes.LITERAL_DO,
            KnownTokenTypes.LITERAL_IF,
            KnownTokenTypes.LITERAL_ELSE,
            KnownTokenTypes.LITERAL_FOR);

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if (OPTION_PROP.equals(attrName)) {
            switch (attrValue) {
                case LEFT_CURLY_POLICY_EOL:
                    leftCurlyPolicy = CommonCodeStyleSettings.END_OF_LINE;
                    break;
                case LEFT_CURLY_POLICY_NL:
                    leftCurlyPolicy = CommonCodeStyleSettings.NEXT_LINE;
                    break;
                case LEFT_CURLY_POLICY_NLOW:
                    leftCurlyPolicy = CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED;
                    break;
                default:
                    // uncharted territory - https://checkstyle.org/property_types.html#LeftCurlyOption
                    LOG.warn("Unexpected left curly policy: " + attrValue);
                    break;
            }
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (appliesTo(KnownTokenTypes.CLASS_DEF) || appliesTo(KnownTokenTypes.INTERFACE_DEF)) {
            javaSettings.CLASS_BRACE_STYLE = leftCurlyPolicy;
            if (policyRequiresANewLine()) {
                javaSettings.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false;
            }
        }
        if (appliesTo(KnownTokenTypes.METHOD_DEF) || appliesTo(KnownTokenTypes.CTOR_DEF)) {
            javaSettings.METHOD_BRACE_STYLE = leftCurlyPolicy;
            if (policyRequiresANewLine()) {
                javaSettings.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false;
            }
        }
        if (appliesToOneOf(CONDITIONAL_TOKENS)) {
            javaSettings.BRACE_STYLE = leftCurlyPolicy;
            if (policyRequiresANewLine()) {
                javaSettings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false;
            }
        }
    }

    private boolean policyRequiresANewLine() {
        return leftCurlyPolicy == CommonCodeStyleSettings.NEXT_LINE || leftCurlyPolicy == CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED;
    }
}
