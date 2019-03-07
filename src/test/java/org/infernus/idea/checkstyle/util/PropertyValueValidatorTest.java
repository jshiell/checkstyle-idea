package org.infernus.idea.checkstyle.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class PropertyValueValidatorTest {
    @Test
    public void PropertyValueValidatorIntSetTest() {
        assertEquals(null, PropertyValueValidator.validate("Integer Set", "42,666"));
        assertEquals(null, PropertyValueValidator.validate("Integer Set", "42"));
        assertEquals(null, PropertyValueValidator.validate("Integer Set", "666"));
        assertEquals(null, PropertyValueValidator.validate("Integer Set", "{}"));

        assertEquals("Has to be a list of number separated with comma without space",
                PropertyValueValidator.validate("Integer Set", "42, 666"));
    }

    @Test
    public void PropertyValueValidatorRegExpTest() {
        assertEquals(null, PropertyValueValidator.validate("Regular Expression", "^[0-9]+$"));
        assertEquals(null, PropertyValueValidator.validate("Regular Expression", "abcde"));

        assertEquals("Illegal regular expression",
                PropertyValueValidator.validate("Regular Expression", "["));
    }

    @Test
    public void PropertyValueValidatorURLTest() {
        assertEquals(null, PropertyValueValidator.validate("URI", "http://checkstyle.sourceforge.net/property_types.html#regexp"));
        assertEquals(null, PropertyValueValidator.validate("URI", "org.infernus.idea.checkstyle.util"));
        assertEquals(null, PropertyValueValidator.validate("URI", "or_g.infernus.id_ea.ch12eckstyle.util"));
        assertEquals(null, PropertyValueValidator.validate("URI", "src/test/java/org/infernus/idea/checkstyle/util/PropertyValueValidatorTest.java"));

        assertEquals("Not a valid URI", PropertyValueValidator.validate("URI", "htt://checkstyle.sourceforge.net/property_types.html#regexp"));
        assertEquals("Not a valid URI", PropertyValueValidator.validate("URI", "org.infernus.idea.checkstyle."));
        assertEquals("Not a valid URI", PropertyValueValidator.validate("URI", "0org.infer156nus.idea.checkstyle"));
        assertEquals("Not a valid URI", PropertyValueValidator.validate("URI", "not exist file"));
    }

    @Test
    public void PropertyValueValidatorSubsetOfTest() {
        assertEquals(null, PropertyValueValidator.validate("subset of tokens ANNOTATION_DEF, ANNOTATION_FIELD_DEF, CLASS_DEF, CTOR_DEF, ENUM_CONSTANT_DEF, ENUM_DEF, INTERFACE_DEF, METHOD_DEF, PACKAGE_DEF, VARIABLE_DEF.",
                "ANNOTATION_DEF, ANNOTATION_FIELD_DEF, CLASS_DEF, CTOR_DEF, ENUM_CONSTANT_DEF, ENUM_DEF, INTERFACE_DEF, METHOD_DEF, PACKAGE_DEF, VARIABLE_DEF"));
        assertEquals(null, PropertyValueValidator.validate("subset of tokens TokenTypes.", "LABELED_STAT"));
        assertEquals(null, PropertyValueValidator.validate("subset of tokens TokenTypes.", ""));

        assertEquals("Invalid token ANNOTATION_DEF", PropertyValueValidator.validate("subset of tokens ENUM_CONSTANT_DEF.",
                "ANNOTATION_DEF, ANNOTATION_FIELD_DEF, CLASS_DEF, CTOR_DEF, ENUM_CONSTANT_DEF, ENUM_DEF, INTERFACE_DEF, METHOD_DEF, PACKAGE_DEF, VARIABLE_DEF"));
    }

    @Test
    public void PropertyValueValidatorAccessModifierSetTest() {
        assertEquals(null, PropertyValueValidator.validate("Access Modifier Set", "public"));
        assertEquals(null, PropertyValueValidator.validate("Access Modifier Set", "protected"));
        assertEquals(null, PropertyValueValidator.validate("Access Modifier Set", "package"));
        assertEquals(null, PropertyValueValidator.validate("Access Modifier Set", "private"));
        assertEquals(null, PropertyValueValidator.validate("Access Modifier Set", "private, protected"));

        assertEquals("nleol is a invalid Access Modifier", PropertyValueValidator.validate("Access Modifier Set", "nleol"));
        assertEquals("nleol is a invalid Access Modifier", PropertyValueValidator.validate("Access Modifier Set", "private, protected, nleol"));
    }
}
