package org.infernus.idea.checkstyle.model;

import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.*;

public class ConfigRuleTest {
    @Test
    public void ConfigRuleGetNameTest() throws FileNotFoundException {
        ConfigRule rule = new ConfigRule("/Users/elliottdebruin/IdeaProjects/checkstyle-idea/src/test/java/org/infernus/idea/checkstyle/model/ConfigurationLocationFactoryTest.java");
        assertEquals("ConfigurationLocationFactoryTest", rule.getRuleName());
    }

    @Test
    public void ConfigRuleGetDescriptionTest() {

    }

    @Test
    public void ConfigRuleGetParametersTest() {

    }
}
