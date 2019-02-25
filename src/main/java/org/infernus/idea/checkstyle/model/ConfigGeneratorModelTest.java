package org.infernus.idea.checkstyle.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigGeneratorModelTest {
    @Test
    public void ConfigGeneratorRootIsCheckerTest() {
        ConfigGeneratorModel model = new ConfigGeneratorModel();
        assertEquals("Checker", model.config.getName());
    }

    @Test
    public void ConfigGeneratorSetNameTest() {
        ConfigGeneratorModel model = new ConfigGeneratorModel();
        String name = "Config";
        model.setConfigName(name);
        assertEquals(name, model.configName);
    }
}
