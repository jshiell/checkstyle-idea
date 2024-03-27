package org.infernus.idea.checkstyle.importer.modules;

import java.util.EnumSet;
import java.util.Set;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.csapi.KnownTokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class WhitespaceAroundImporter extends ModuleImporter {

    private static final Set<KnownTokenTypes> ASSIGNMENTS = EnumSet.of(
                    KnownTokenTypes.ASSIGN,
                    KnownTokenTypes.BAND_ASSIGN,
                    KnownTokenTypes.BOR_ASSIGN,
                    KnownTokenTypes.BSR_ASSIGN,
                    KnownTokenTypes.BXOR_ASSIGN,
                    KnownTokenTypes.DIV_ASSIGN,
                    KnownTokenTypes.MOD_ASSIGN,
                    KnownTokenTypes.MINUS_ASSIGN,
                    KnownTokenTypes.PLUS_ASSIGN,
                    KnownTokenTypes.SL_ASSIGN,
                    KnownTokenTypes.SR_ASSIGN,
                    KnownTokenTypes.STAR_ASSIGN
            );
    private static final Set<KnownTokenTypes> LOGICAL_OPERATORS = EnumSet.of(
                    KnownTokenTypes.LOR,
                    KnownTokenTypes.LAND
            );
    private static final Set<KnownTokenTypes> EQUALITY_OPERATORS = EnumSet.of(
                    KnownTokenTypes.EQUAL,
                    KnownTokenTypes.NOT_EQUAL
            );
    private static final Set<KnownTokenTypes> RELATIONAL_OPERATORS = EnumSet.of(
                    KnownTokenTypes.LT,
                    KnownTokenTypes.LE,
                    KnownTokenTypes.GT,
                    KnownTokenTypes.GE
            );
    private static final Set<KnownTokenTypes> BITWISE_OPERATORS = EnumSet.of(
                    KnownTokenTypes.BAND,
                    KnownTokenTypes.BOR,
                    KnownTokenTypes.BXOR
            );
    private static final Set<KnownTokenTypes> ADDITIVE_OPERATORS = EnumSet.of(
                    KnownTokenTypes.PLUS,
                    KnownTokenTypes.MINUS
            );
    private static final Set<KnownTokenTypes> MULTIPLICATIVE_OPERATORS = EnumSet.of(
                    KnownTokenTypes.STAR,
                    KnownTokenTypes.DIV,
                    KnownTokenTypes.MOD
            );
    private static final Set<KnownTokenTypes> SHIFT_OPERATORS = EnumSet.of(
                    KnownTokenTypes.SR,
                    KnownTokenTypes.SL,
                    KnownTokenTypes.BSR
            );


    @Override
    protected void handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        // nothing to do
    }


    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings commonSettings = getCommonSettings(settings);

        if (appliesToOneOf(ASSIGNMENTS)) {
            commonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
        }
        if (appliesToOneOf(LOGICAL_OPERATORS)) {
            commonSettings.SPACE_AROUND_LOGICAL_OPERATORS = true;
        }
        if (appliesToOneOf(EQUALITY_OPERATORS)) {
            commonSettings.SPACE_AROUND_EQUALITY_OPERATORS = true;
        }
        if (appliesToOneOf(RELATIONAL_OPERATORS)) {
            commonSettings.SPACE_AROUND_RELATIONAL_OPERATORS = true;
        }
        if (appliesToOneOf(BITWISE_OPERATORS)) {
            commonSettings.SPACE_AROUND_BITWISE_OPERATORS = true;
        }
        if (appliesToOneOf(ADDITIVE_OPERATORS)) {
            commonSettings.SPACE_AROUND_ADDITIVE_OPERATORS = true;
        }
        if (appliesToOneOf(MULTIPLICATIVE_OPERATORS)) {
            commonSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = true;
        }
        if (appliesToOneOf(SHIFT_OPERATORS)) {
            commonSettings.SPACE_AROUND_SHIFT_OPERATORS = true;
        }
        if (appliesTo(KnownTokenTypes.COLON)) {
            commonSettings.SPACE_BEFORE_COLON = true;
            commonSettings.SPACE_AFTER_COLON = true;
        }
        if (appliesTo(KnownTokenTypes.QUESTION)) {
            commonSettings.SPACE_AFTER_QUEST = true;
            commonSettings.SPACE_BEFORE_QUEST = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_CATCH)) {
            commonSettings.SPACE_BEFORE_CATCH_KEYWORD = true;
            commonSettings.SPACE_BEFORE_CATCH_PARENTHESES = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_ELSE)) {
            commonSettings.SPACE_BEFORE_ELSE_KEYWORD = true;
            commonSettings.SPACE_BEFORE_ELSE_LBRACE = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_FINALLY)) {
            commonSettings.SPACE_BEFORE_FINALLY_KEYWORD = true;
            commonSettings.SPACE_BEFORE_FINALLY_LBRACE = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_FOR)) {
            commonSettings.SPACE_BEFORE_FOR_PARENTHESES = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_IF)) {
            commonSettings.SPACE_BEFORE_IF_PARENTHESES = true;
        }
        if (appliesTo(KnownTokenTypes.DO_WHILE) || appliesTo(KnownTokenTypes.LITERAL_WHILE)) {
            commonSettings.SPACE_BEFORE_WHILE_KEYWORD = true;
            commonSettings.SPACE_BEFORE_WHILE_PARENTHESES = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_DO)) {
            commonSettings.SPACE_BEFORE_DO_LBRACE = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_SWITCH)) {
            commonSettings.SPACE_BEFORE_SWITCH_PARENTHESES = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_SYNCHRONIZED)) {
            commonSettings.SPACE_BEFORE_SYNCHRONIZED_PARENTHESES = true;
        }
        if (appliesTo(KnownTokenTypes.LITERAL_TRY)) {
            commonSettings.SPACE_BEFORE_TRY_PARENTHESES = true;
            commonSettings.SPACE_BEFORE_TRY_LBRACE = true;
        }
        if (appliesTo(KnownTokenTypes.LCURLY)) {
            commonSettings.SPACE_BEFORE_ANNOTATION_ARRAY_INITIALIZER_LBRACE = false;
            commonSettings.SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE = true;
            commonSettings.SPACE_BEFORE_CATCH_LBRACE = true;
            commonSettings.SPACE_BEFORE_CLASS_LBRACE = true;
            commonSettings.SPACE_BEFORE_DO_LBRACE = true;
            commonSettings.SPACE_BEFORE_ELSE_LBRACE = true;
            commonSettings.SPACE_BEFORE_FINALLY_LBRACE = true;
            commonSettings.SPACE_BEFORE_IF_LBRACE = true;
            commonSettings.SPACE_BEFORE_METHOD_LBRACE = true;
            commonSettings.SPACE_BEFORE_SWITCH_LBRACE = true;
            commonSettings.SPACE_BEFORE_SYNCHRONIZED_LBRACE = true;
            commonSettings.SPACE_BEFORE_TRY_LBRACE = true;

            getJavaSettings(settings).SPACE_INSIDE_ONE_LINE_ENUM_BRACES = true;
        }
    }
}
