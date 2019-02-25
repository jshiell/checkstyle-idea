package org.infernus.idea.checkstyle.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigRuleTest {
    @Test
    public void ConfigRuleGetNameTest() {
        ConfigRule rule = new ConfigRule("./ConfigurationLocationFactoryTest.java");
        assertEquals("ConfigurationLocationFactoryTest", rule.getRuleName());
    }

    @Test
    public void ConfigRuleGetDescriptionTest() {

    }

    @Test
    public void ConfigRuleGetParametersTest() {

    }
}
