package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@SuppressWarnings("unused")
public class WhitespaceAroundImporter extends ModuleImporter {

    private static final Set<Integer> ASSIGNMENTS =
            setOf(
                    TokenTypes.ASSIGN,
                    TokenTypes.BAND_ASSIGN,
                    TokenTypes.BOR_ASSIGN,
                    TokenTypes.BSR_ASSIGN,
                    TokenTypes.BXOR_ASSIGN,
                    TokenTypes.DIV_ASSIGN,
                    TokenTypes.MOD_ASSIGN,
                    TokenTypes.MINUS_ASSIGN,
                    TokenTypes.PLUS_ASSIGN,
                    TokenTypes.SL_ASSIGN,
                    TokenTypes.SR_ASSIGN,
                    TokenTypes.STAR_ASSIGN
            );
    private static final Set<Integer> LOGICAL_OPERATORS =
            setOf(
                    TokenTypes.LOR,
                    TokenTypes.LAND
            );
    private static final Set<Integer> EQUALITY_OPERATORS =
            setOf(
                    TokenTypes.EQUAL,
                    TokenTypes.NOT_EQUAL
            );
    private static final Set<Integer> RELATIONAL_OPERATORS =
            setOf(
                    TokenTypes.LT,
                    TokenTypes.LE,
                    TokenTypes.GT,
                    TokenTypes.GE
            );
    private static final Set<Integer> BITWISE_OPERATORS =
            setOf(
                    TokenTypes.BAND,
                    TokenTypes.BOR,
                    TokenTypes.BXOR
            );
    private static final Set<Integer> ADDITIVE_OPERATORS =
            setOf(
                    TokenTypes.PLUS,
                    TokenTypes.MINUS
            );
    private static final Set<Integer> MULTIPLICATIVE_OPERATORS =
            setOf(
                    TokenTypes.STAR,
                    TokenTypes.DIV,
                    TokenTypes.MOD
            );
    private static final Set<Integer> SHIFT_OPERATORS =
            setOf(
                    TokenTypes.SR,
                    TokenTypes.SL,
                    TokenTypes.BSR
            );


    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        CommonCodeStyleSettings javaSettings = getJavaSettings(settings);
        if (appliesToOneOf(ASSIGNMENTS)) {
            javaSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
        }
        if (appliesToOneOf(LOGICAL_OPERATORS)) {
            javaSettings.SPACE_AROUND_LOGICAL_OPERATORS = true;
        }
        if (appliesToOneOf(EQUALITY_OPERATORS)) {
            javaSettings.SPACE_AROUND_EQUALITY_OPERATORS = true;
        }
        if (appliesToOneOf(RELATIONAL_OPERATORS)) {
            javaSettings.SPACE_AROUND_RELATIONAL_OPERATORS = true;
        }
        if (appliesToOneOf(BITWISE_OPERATORS)) {
            javaSettings.SPACE_AROUND_BITWISE_OPERATORS = true;
        }
        if (appliesToOneOf(ADDITIVE_OPERATORS)) {
            javaSettings.SPACE_AROUND_ADDITIVE_OPERATORS = true;
        }
        if (appliesToOneOf(MULTIPLICATIVE_OPERATORS)) {
            javaSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = true;
        }
        if (appliesToOneOf(SHIFT_OPERATORS)) {
            javaSettings.SPACE_AROUND_SHIFT_OPERATORS = true;
        }
        if (appliesTo(TokenTypes.COLON)) {
            javaSettings.SPACE_BEFORE_COLON = true;
            javaSettings.SPACE_AFTER_COLON = true;
        }
        if (appliesTo(TokenTypes.QUESTION)) {
            javaSettings.SPACE_AFTER_QUEST = true;
            javaSettings.SPACE_BEFORE_QUEST = true;
        }
        if (appliesTo(TokenTypes.LITERAL_CATCH)) {
            javaSettings.SPACE_BEFORE_CATCH_KEYWORD = true;
            javaSettings.SPACE_BEFORE_CATCH_PARENTHESES = true;
        }
        if (appliesTo(TokenTypes.LITERAL_ELSE)) {
            javaSettings.SPACE_BEFORE_ELSE_KEYWORD = true;
            javaSettings.SPACE_BEFORE_ELSE_LBRACE = true;
        }
        if (appliesTo(TokenTypes.LITERAL_FINALLY)) {
            javaSettings.SPACE_BEFORE_FINALLY_KEYWORD = true;
            javaSettings.SPACE_BEFORE_FINALLY_LBRACE = true;
        }
        if (appliesTo(TokenTypes.LITERAL_FOR)) {
            javaSettings.SPACE_BEFORE_FOR_PARENTHESES = true;
        }
        if (appliesTo(TokenTypes.LITERAL_IF)) {
            javaSettings.SPACE_BEFORE_IF_PARENTHESES = true;
        }
        if (appliesTo(TokenTypes.DO_WHILE) || appliesTo(TokenTypes.LITERAL_WHILE)) {
            javaSettings.SPACE_BEFORE_WHILE_KEYWORD = true;
            javaSettings.SPACE_BEFORE_WHILE_PARENTHESES = true;
        }
        if (appliesTo(TokenTypes.LITERAL_DO)) {
            javaSettings.SPACE_BEFORE_DO_LBRACE = true;
        }
        if (appliesTo(TokenTypes.LITERAL_SWITCH)) {
            javaSettings.SPACE_BEFORE_SWITCH_PARENTHESES = true;
        }
        if (appliesTo(TokenTypes.LITERAL_SYNCHRONIZED)) {
            javaSettings.SPACE_BEFORE_SYNCHRONIZED_PARENTHESES = true;
        }
        if (appliesTo(TokenTypes.LITERAL_TRY)) {
            javaSettings.SPACE_BEFORE_TRY_PARENTHESES = true;
            javaSettings.SPACE_BEFORE_TRY_LBRACE = true;
        }
        if (appliesTo(TokenTypes.LCURLY)) {
            javaSettings.SPACE_BEFORE_ANNOTATION_ARRAY_INITIALIZER_LBRACE = true;
            javaSettings.SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE = true;
            javaSettings.SPACE_BEFORE_CATCH_LBRACE = true;
            javaSettings.SPACE_BEFORE_CLASS_LBRACE = true;
            javaSettings.SPACE_BEFORE_DO_LBRACE = true;
            javaSettings.SPACE_BEFORE_ELSE_LBRACE = true;
            javaSettings.SPACE_BEFORE_FINALLY_LBRACE = true;
            javaSettings.SPACE_BEFORE_IF_LBRACE = true;
            javaSettings.SPACE_BEFORE_METHOD_LBRACE = true;
            javaSettings.SPACE_BEFORE_SWITCH_LBRACE = true;
            javaSettings.SPACE_BEFORE_SYNCHRONIZED_LBRACE = true;
            javaSettings.SPACE_BEFORE_TRY_LBRACE = true;
        }
    }
}
