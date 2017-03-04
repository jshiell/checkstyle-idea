package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class IndentationImporter extends ModuleImporter {

    private static final String BASIC_OFFSET_PROP = "basicOffset";
    private static final String CASE_INDENT_PROP = "caseIndent";
    private static final String LINE_WRAP_INDENT_PROP = "lineWrappingIndentation";

    private static final int DEFAULT_BASIC_OFFSET = 4;
    private static final int DEFAULT_LINE_WRAP_INDENT = 4;
    private static final boolean DEFAULT_INDENT_CASE = true;

    private int basicIndent = DEFAULT_BASIC_OFFSET;
    private int continuationIndent = DEFAULT_LINE_WRAP_INDENT;
    private boolean indentCase = DEFAULT_INDENT_CASE;

    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        switch (attrName) {
            case BASIC_OFFSET_PROP:
                basicIndent = getIntOrDefault(attrValue, DEFAULT_BASIC_OFFSET);
                break;
            case CASE_INDENT_PROP:
                int caseIndent = getIntOrDefault(attrValue, 0);
                indentCase = caseIndent > 0;
                break;
            case LINE_WRAP_INDENT_PROP:
                continuationIndent = getIntOrDefault(attrValue, DEFAULT_LINE_WRAP_INDENT);
                break;
        }
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
