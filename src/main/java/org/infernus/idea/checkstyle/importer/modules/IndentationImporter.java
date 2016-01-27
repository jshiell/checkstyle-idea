package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class IndentationImporter extends ModuleImporter {
    private final static String BASIC_OFFSET_PROP     = "basicOffset";
    private final static String CASE_INDENT_PROP      = "caseIndent";
    private final static String LINE_WRAP_INDENT_PROP = "lineWrappingIndentation";
    
    private final static int     DEFAULT_BASIC_OFFSET       = 4;
    private final static int     DEFAULT_LINE_WRAP_INDENT   = 4;
    private final static boolean DEFAULT_INDENT_CASE        = true;
    
    private int basicIndent = DEFAULT_BASIC_OFFSET;
    private int continuationIndent = DEFAULT_LINE_WRAP_INDENT;
    private boolean indentCase = DEFAULT_INDENT_CASE;
    
    @Override
    protected boolean handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        switch (attrName) {
            case BASIC_OFFSET_PROP:
                basicIndent = getIntOrDefault(attrValue, DEFAULT_BASIC_OFFSET);
                return true;
            case CASE_INDENT_PROP:
                int caseIndent = getIntOrDefault(attrValue, 0);
                indentCase = caseIndent > 0;
                return true;
            case LINE_WRAP_INDENT_PROP:
                continuationIndent = getIntOrDefault(attrValue, DEFAULT_LINE_WRAP_INDENT);
                return true;
        }
        return false;
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        CommonCodeStyleSettings.IndentOptions indentOptions = javaSettings.getIndentOptions();
        if (indentOptions != null) {
            indentOptions.INDENT_SIZE = basicIndent;
            indentOptions.CONTINUATION_INDENT_SIZE = continuationIndent;
        }
        javaSettings.INDENT_CASE_FROM_SWITCH = indentCase;
    }
}
