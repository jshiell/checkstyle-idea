package org.infernus.idea.checkstyle.util;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.validator.routines.UrlValidator;

public class PropertyValueValidator {
    private static String TOKEN_TYPES[] = {
            "ABSTRACT", "ANNOTATION", "ANNOTATION_ARRAY_INIT", "ANNOTATION_DEF",
            "ANNOTATION_FIELD_DEF", "ANNOTATION_MEMBER_VALUE_PAIR", "ANNOTATIONS",
            "ARRAY_DECLARATOR", "ARRAY_INIT", "ASSIGN", "AT", "BAND", "BAND_ASSIGN",
            "BLOCK_COMMENT_BEGIN", "BLOCK_COMMENT_END", "BNOT", "BOR", "BOR_ASSIGN",
            "BSR", "BSR_ASSIGN", "BXOR", "BXOR_ASSIGN", "CASE_GROUP", "CHAR_LITERAL",
            "CLASS_DEF", "COLON", "COMMA", "COMMENT_CONTENT", "CTOR_CALL", "CTOR_DEF",
            "DEC", "DIV", "DIV_ASSIGN", "DO_WHILE", "DOT", "DOUBLE_COLON", "ELIST",
            "ELLIPSIS", "EMPTY_STAT", "ENUM", "ENUM_CONSTANT_DEF", "ENUM_DEF", "EOF",
            "EQUAL", "EXPR", "EXTENDS_CLAUSE", "FINAL", "FOR_CONDITION",
            "FOR_EACH_CLAUSE", "FOR_INIT", "FOR_ITERATOR", "GE", "GENERIC_END",
            "GENERIC_START", "GT", "IDENT", "IMPLEMENTS_CLAUSE", "IMPORT", "INC",
            "INDEX_OP", "INSTANCE_INIT", "INTERFACE_DEF", "LABELED_STAT", "LAMBDA",
            "LAND", "LCURLY", "LE", "LITERAL_ASSERT", "LITERAL_BOOLEAN", "LITERAL_BREAK",
            "LITERAL_BYTE", "LITERAL_CASE", "LITERAL_CATCH", "LITERAL_CHAR", "LITERAL_CLASS",
            "LITERAL_CONTINUE", "LITERAL_DEFAULT", "LITERAL_DO", "LITERAL_DOUBLE", "LITERAL_ELSE",
            "LITERAL_FALSE", "LITERAL_FINALLY", "LITERAL_FLOAT", "LITERAL_FOR", "LITERAL_IF",
            "LITERAL_INSTANCEOF", "LITERAL_INT", "LITERAL_INTERFACE", "LITERAL_LONG", "LITERAL_NATIVE",
            "LITERAL_NEW", "LITERAL_NULL", "LITERAL_PRIVATE", "LITERAL_PROTECTED", "LITERAL_PUBLIC",
            "LITERAL_RETURN", "LITERAL_SHORT", "LITERAL_STATIC", "LITERAL_SUPER", "LITERAL_SWITCH",
            "LITERAL_SYNCHRONIZED", "LITERAL_THIS", "LITERAL_THROW", "LITERAL_THROWS",
            "LITERAL_TRANSIENT", "LITERAL_TRUE", "LITERAL_TRY", "LITERAL_VOID", "LITERAL_VOLATILE",
            "LITERAL_WHILE", "LNOT", "LOR", "LPAREN", "LT", "METHOD_CALL", "METHOD_DEF", "METHOD_REF",
            "MINUS", "MINUS_ASSIGN", "MOD", "MOD_ASSIGN", "MODIFIERS", "NOT_EQUAL", "NUM_DOUBLE",
            "NUM_FLOAT", "NUM_INT", "NUM_LONG", "OBJBLOCK", "PACKAGE_DEF", "PARAMETER_DEF", "PARAMETERS",
            "PLUS", "PLUS_ASSIGN", "POST_DEC", "POST_INC", "QUESTION", "RBRACK", "RCURLY", "RESOURCE",
            "RESOURCE_SPECIFICATION", "RESOURCES", "RPAREN", "SEMI", "SINGLE_LINE_COMMENT", "SL",
            "SL_ASSIGN", "SLIST", "SR", "SR_ASSIGN", "STAR", "STAR_ASSIGN", "STATIC_IMPORT",
            "STATIC_INIT", "STRICTFP", "STRING_LITERAL", "SUPER_CTOR_CALL", "TYPE", "TYPE_ARGUMENT",
            "TYPE_ARGUMENTS", "TYPE_EXTENSION_AND", "TYPE_LOWER_BOUNDS", "TYPE_PARAMETER", "TYPE_PARAMETERS",
            "TYPE_UPPER_BOUNDS", "TYPECAST", "UNARY_MINUS", "UNARY_PLUS", "VARIABLE_DEF", "WILDCARD_TYPE"
    };

    /**
     * Validates the value according to the type
     * @param type - The type of the value
     * @param value - The value to validate
     * @return null when the value pass the validation or has no validation available. Error message
     *         about the reason why not passing the validation.
     */
    public static String validate(String type, String value) {
        switch (type) {
            case "Number Set" :
            case "Integer Set" :
                if (value.contains(",")) {
                    String ints[] = value.split(",");

                    for (String integer : ints) {
                        if (!Pattern.matches("^[0-9]+$", integer)) {
                            return "Has to be a list of number separated with comma without space";
                        }
                    }

                    return null;
                }

                return Pattern.matches("^[0-9]+$", value) || value.equals("{}") ? null : "Has to be a list of number separated with comma without space";
            case "Regular Expression" :
                try {
                    Pattern.compile(value);
                } catch (PatternSyntaxException e) {
                    return "Illegal regular expression";
                }

                return null;
            case "URI" :
                String[] schemes = {"http","https"};
                UrlValidator urlValidator = new UrlValidator(schemes);

                return urlValidator.isValid(value) || Pattern.matches("^([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*$", value)
                        || (new File(value).exists()) ? null : "Not a valid URI";
            case "Access Modifier Set" :
                String split[] = value.split(", ");

                for (String subValue : split) {
                    if (!(subValue.equals("public") || subValue.equals("protected")
                            || subValue.equals("package") || subValue.equals("private"))) {
                        return subValue + " is a invalid Access Modifier";
                    }
                }
                return null;
        }

        if (Pattern.matches("^subset of tokens.+$", type) && value.length() > 0) {
            // types with "subset of tokens..." since those have no pattern we have to hard code things
            String tokenString = type.replaceAll("subset of tokens", "").replace(".", "").trim();
            String tokens[] = tokenString.split(", ");

            if (tokens.length == 1 && tokens[0].equals("TokenTypes")) {
                // another special case in this case
                Set<String> valid_tokens = new HashSet<>(Arrays.asList(TOKEN_TYPES));
                String input_tokens[] = value.split(", ");

                for (String input_token : input_tokens) {
                    if (!valid_tokens.contains(input_token.trim())) {
                        return "Invalid token " + input_token;
                    }
                }
            } else {
                Set<String> valid_tokens = new HashSet<>(Arrays.asList(tokens));
                String input_tokens[] = value.split(", ");

                for (String input_token : input_tokens) {
                    if (!valid_tokens.contains(input_token.trim())) {
                        return "Invalid token " + input_token;
                    }
                }
            }
        }

        return null;
    }
}
