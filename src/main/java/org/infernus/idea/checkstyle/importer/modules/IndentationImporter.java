package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class IndentationImporter extends ModuleImporter {

    private static final Logger LOG = Logger.getInstance(IndentationImporter.class);

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

            default:
                // uncharted territory - https://checkstyle.org/property_types.html#LeftCurlyOption
                LOG.warn("Unexpected indentation policy: " + attrValue);
                break;
        }
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings commonSettings = getCommonSettings(settings);
        CommonCodeStyleSettings.IndentOptions indentOptions = commonSettings.getIndentOptions();
        if (indentOptions != null) {
            indentOptions.INDENT_SIZE = basicIndent;
            indentOptions.CONTINUATION_INDENT_SIZE = continuationIndent;
        }
        commonSettings.INDENT_CASE_FROM_SWITCH = indentCase;
    }
}
