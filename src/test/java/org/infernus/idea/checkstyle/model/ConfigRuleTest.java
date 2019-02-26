package org.infernus.idea.checkstyle.model;

import org.junit.Test;
import java.nio.file.Paths;


import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

public class ConfigRuleTest {
    @Test
    public void ConfigRuleGetNameTest() throws IOException {

        String path = Paths.get(".").toAbsolutePath().normalize().toString();
        ConfigRule rule = new ConfigRule(path + "/src/test/java/org/infernus/idea/checkstyle/model/ConfigurationLocationFactoryTest.java");
        assertEquals("ConfigurationLocationFactoryTest", rule.getRuleName());
    }

    @Test
    public void ConfigRuleGetDescriptionTest() {

    }

    @Test
    public void ConfigRuleGetParametersTest() {

    }
}
