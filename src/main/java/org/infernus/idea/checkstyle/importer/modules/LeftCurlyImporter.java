package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@SuppressWarnings("unused")
public class LeftCurlyImporter extends ModuleImporter {
    private final static String OPTION_PROP = "option";
    
    private final static String LEFT_CURLY_POLICY_EOL   = "eol";
    private final static String LEFT_CURLY_POLICY_NL    = "nl";
    private final static String LEFT_CURLY_POLICY_NLOW  = "nlow";
    
    private int leftCurlyPolicy = CommonCodeStyleSettings.END_OF_LINE;
    
    private final static Set<Integer> CONDITIONAL_TOKENS = setOf(
            TokenTypes.LITERAL_WHILE,
            TokenTypes.LITERAL_TRY,
            TokenTypes.LITERAL_CATCH,
            TokenTypes.LITERAL_FINALLY,
            TokenTypes.LITERAL_SYNCHRONIZED,
            TokenTypes.LITERAL_SWITCH,
            TokenTypes.LITERAL_DO,
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_ELSE,
            TokenTypes.LITERAL_FOR
    );
    
    @Override
    protected boolean handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
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
            }
            return true;
        }
        return super.handleAttribute(attrName, attrValue);
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (appliesTo(TokenTypes.CLASS_DEF) || appliesTo(TokenTypes.INTERFACE_DEF)) {
            javaSettings.CLASS_BRACE_STYLE = leftCurlyPolicy;
        }
        if (appliesTo(TokenTypes.METHOD_DEF) || appliesTo(TokenTypes.CTOR_DEF)) {
            javaSettings.METHOD_BRACE_STYLE = leftCurlyPolicy;
        }
        if (appliesToOneOf(CONDITIONAL_TOKENS)) {
            javaSettings.BRACE_STYLE = leftCurlyPolicy;
        }
    }
}
