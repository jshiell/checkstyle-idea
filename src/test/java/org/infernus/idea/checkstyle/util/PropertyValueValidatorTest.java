package org.infernus.idea.checkstyle.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class PropertyValueValidatorTest {
    @Test
    public void PropertyValueValidatorIntegerTest() {
        assertTrue(PropertyValueValidator.validate("Integer", "20"));

        assertFalse(PropertyValueValidator.validate("Integer", "as213"));
        assertFalse(PropertyValueValidator.validate("Integer", "221as13"));
        assertFalse(PropertyValueValidator.validate("Integer", "221as"));
    }

    @Test
    public void PropertyValueValidatorStringTest() {
        assertTrue(PropertyValueValidator.validate("String", "this is a string"));
    }

    @Test
    public void PropertyValueValidatorStringSetTest() {
        assertTrue(PropertyValueValidator.validate("String", "IV_ASSIGN,PLUS_ASSIGN"));
    }

    @Test
    public void PropertyValueValidatorBooleanTest() {
        assertTrue(PropertyValueValidator.validate("Boolean", "true"));
        assertTrue(PropertyValueValidator.validate("Boolean", "random things"));
    }

    @Test
    public void PropertyValueValidatorIntSetTest() {
        assertTrue(PropertyValueValidator.validate("Integer Set", "42,666"));
        assertTrue(PropertyValueValidator.validate("Integer Set", "42"));
        assertTrue(PropertyValueValidator.validate("Integer Set", "666"));
        assertTrue(PropertyValueValidator.validate("Integer Set", "{}"));

        assertFalse(PropertyValueValidator.validate("Integer Set", "42, 666"));
    }

    @Test
    public void PropertyValueValidatorRegExpTest() {
        assertTrue(PropertyValueValidator.validate("Regular Expression", "^[0-9]+$"));
        assertTrue(PropertyValueValidator.validate("Regular Expression", "abcde"));

        assertFalse(PropertyValueValidator.validate("Regular Expression", "["));
    }

    @Test
    public void PropertyValueValidatorURLTest() {
        assertTrue(PropertyValueValidator.validate("URI", "http://checkstyle.sourceforge.net/property_types.html#regexp"));
        assertTrue(PropertyValueValidator.validate("URI", "org.infernus.idea.checkstyle.util"));
        assertTrue(PropertyValueValidator.validate("URI", "or_g.infernus.id_ea.ch12eckstyle.util"));
        assertTrue(PropertyValueValidator.validate("URI", "src/test/java/org/infernus/idea/checkstyle/util/PropertyValueValidatorTest.java"));

        assertFalse(PropertyValueValidator.validate("URI", "htt://checkstyle.sourceforge.net/property_types.html#regexp"));
        assertFalse(PropertyValueValidator.validate("URI", "org.infernus.idea.checkstyle."));
        assertFalse(PropertyValueValidator.validate("URI", "0org.infer156nus.idea.checkstyle"));
        assertFalse(PropertyValueValidator.validate("URI", "not exist file"));
    }

    @Test
    public void PropertyValueValidatorLineSeparatorTest() {
        assertTrue(PropertyValueValidator.validate("lineSeparator", "crlf"));
        assertTrue(PropertyValueValidator.validate("lineSeparator", "cr"));
        assertTrue(PropertyValueValidator.validate("lineSeparator", "lf"));
        assertTrue(PropertyValueValidator.validate("lineSeparator", "lf_cr_crlf"));
        assertTrue(PropertyValueValidator.validate("lineSeparator", "system"));

        assertFalse(PropertyValueValidator.validate("lineSeparator", "somthing else"));
    }

    @Test
    public void PropertyValueValidatorPadPolicyTest() {
        assertTrue(PropertyValueValidator.validate("Pad Policy", "nospace"));
        assertTrue(PropertyValueValidator.validate("Pad Policy", "space"));

        assertFalse(PropertyValueValidator.validate("Pad Policy", "space?"));
    }

    @Test
    public void PropertyValueValidatorWrapOperatorPolicyTest() {
        assertTrue(PropertyValueValidator.validate("Wrap Operator Policy", "nl"));
        assertTrue(PropertyValueValidator.validate("Wrap Operator Policy", "eol"));

        assertFalse(PropertyValueValidator.validate("Wrap Operator Policy", "nleol"));
    }

    @Test
    public void PropertyValueValidatorBlockPolicyTest() {
        assertTrue(PropertyValueValidator.validate("Block Policy", "text"));
        assertTrue(PropertyValueValidator.validate("Block Policy", "statement"));

        assertFalse(PropertyValueValidator.validate("Block Policy", "nleol"));
    }

    @Test
    public void PropertyValueValidatorScopeTest() {
        assertTrue(PropertyValueValidator.validate("Scope", "nothing"));
        assertTrue(PropertyValueValidator.validate("Scope", "public"));
        assertTrue(PropertyValueValidator.validate("Scope", "protected"));
        assertTrue(PropertyValueValidator.validate("Scope", "package"));
        assertTrue(PropertyValueValidator.validate("Scope", "private"));
        assertTrue(PropertyValueValidator.validate("Scope", "anoninner"));

        assertFalse(PropertyValueValidator.validate("Scope", "nleol"));
    }

    @Test
    public void PropertyValueValidatorAccessModifierSetTest() {
        assertTrue(PropertyValueValidator.validate("Access Modifier Set", "public"));
        assertTrue(PropertyValueValidator.validate("Access Modifier Set", "protected"));
        assertTrue(PropertyValueValidator.validate("Access Modifier Set", "package"));
        assertTrue(PropertyValueValidator.validate("Access Modifier Set", "private"));
        assertTrue(PropertyValueValidator.validate("Access Modifier Set", "private, protected"));

        assertFalse(PropertyValueValidator.validate("Access Modifier Set", "nleol"));
        assertFalse(PropertyValueValidator.validate("Access Modifier Set", "private, protected, nleol"));
    }

    @Test
    public void PropertyValueValidatorSeverityTest() {
        assertTrue(PropertyValueValidator.validate("Severity", "ignore"));
        assertTrue(PropertyValueValidator.validate("Severity", "info"));
        assertTrue(PropertyValueValidator.validate("Severity", "warning"));
        assertTrue(PropertyValueValidator.validate("Severity", "error"));

        assertFalse(PropertyValueValidator.validate("Severity", "nleol"));
    }

    @Test
    public void PropertyValueValidatorImportOrderPolicyTest() {
        assertTrue(PropertyValueValidator.validate("Import Order Policy", "top"));
        assertTrue(PropertyValueValidator.validate("Import Order Policy", "above"));
        assertTrue(PropertyValueValidator.validate("Import Order Policy", "inflow"));
        assertTrue(PropertyValueValidator.validate("Import Order Policy", "under"));
        assertTrue(PropertyValueValidator.validate("Import Order Policy", "bottom"));

        assertFalse(PropertyValueValidator.validate("Import Order Policy", "nleol"));
    }

    @Test
    public void PropertyValueValidatorElementStyleTest() {
        assertTrue(PropertyValueValidator.validate("Element Style", "expanded"));
        assertTrue(PropertyValueValidator.validate("Element Style", "compact"));
        assertTrue(PropertyValueValidator.validate("Element Style", "compact_no_array"));
        assertTrue(PropertyValueValidator.validate("Element Style", "ignore"));

        assertFalse(PropertyValueValidator.validate("Element Style", "nleol"));
    }

    @Test
    public void PropertyValueValidatorClosingParensTest() {
        assertTrue(PropertyValueValidator.validate("Closing Parens", "always"));
        assertTrue(PropertyValueValidator.validate("Closing Parens", "never"));
        assertTrue(PropertyValueValidator.validate("Closing Parens", "ignore"));

        assertFalse(PropertyValueValidator.validate("Closing Parens", "nleol"));
    }

    @Test
    public void PropertyValueValidatorTrailingCommaTest() {
        assertTrue(PropertyValueValidator.validate("Trailing Comma", "always"));
        assertTrue(PropertyValueValidator.validate("Trailing Comma", "never"));
        assertTrue(PropertyValueValidator.validate("Trailing Comma", "ignore"));

        assertFalse(PropertyValueValidator.validate("Trailing Comma", "nleol"));
    }
}
