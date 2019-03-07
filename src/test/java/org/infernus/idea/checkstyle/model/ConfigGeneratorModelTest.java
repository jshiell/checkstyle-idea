package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class ConfigGeneratorModelTest {
    private ConfigGeneratorModel model;

    @Before
    public void setUp() {
        model = new ConfigGeneratorModel(mock(Project.class));
    }

    @Test
    public void ConfigGeneratorRootIsCheckerTest() {

    }

    @Test
    public void GenerateConfigTest() {

    }

    @Test
    public void ImportConfigTest() {

    }

    @Test
    public void GetActiveRulesTest() {

    }

    @Test
    public void GetConfigRuleForXMLTest() {
        XMLConfig xmlConfig = new XMLConfig("AbstractClassName");
        ConfigRule rule = model.getConfigRuleforXML(xmlConfig);
        assertEquals("AbstractClassName", rule.getRuleName());

    }

    @Test
    public void GetAvailableRulesHasAllCategoriesTest() {
        Set<String> defaultCats = new TreeSet<>();
        defaultCats.add("Annotations");
        defaultCats.add("Block Checks");
        defaultCats.add("Checker");
        defaultCats.add("Class Design");
        defaultCats.add("Coding");
        defaultCats.add("Headers");
        defaultCats.add("Imports");
        defaultCats.add("Javadoc Comments");
        defaultCats.add("Metrics");
        defaultCats.add("Miscellaneous");
        defaultCats.add("Modifiers");
        defaultCats.add("Naming Conventions");
        defaultCats.add("Regexp");
        defaultCats.add("Size Violations");
        defaultCats.add("Whitespace");

        TreeMap<String, List<ConfigRule>> rules = model.getAvailableRules();
        assertEquals(defaultCats, rules.keySet());
    }

    @Test
    public void ConfigPreviewNoRulesTest() {
        String preview = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<!DOCTYPE module PUBLIC \"-//Checkstyle//DTD Checkstyle Configuration 1.3//EN\" " +
                "\"https://checkstyle.org/dtds/configuration_1_3.dtd\">\n" +
                "<module name=\"Checker\"/>\n";
        assertEquals(preview, model.getPreview().replace("\r", ""));

    }

    @Test
    public void ConfigPreviewWithAddedRuleTest() {

        String preview = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<!DOCTYPE module PUBLIC \"-//Checkstyle//DTD Checkstyle Configuration 1.3//EN\" " +
                "\"https://checkstyle.org/dtds/configuration_1_3.dtd\">\n" +
                "<module name=\"Checker\">\n" +
                "    <module name=\"FileTabCharacter\"/>\n" +
                "</module>\n";
        XMLConfig xmlConfig = new XMLConfig("FileTabCharacter");
        model.addActiveRule(xmlConfig);
        assertEquals(preview, model.getPreview().replace("\r", ""));
    }

    @Test
    public void GetConfigNamesTest() {
    }

    @Test
    public void AddActiveRuleTest() {
        XMLConfig xml = new XMLConfig("TestConfig");
        model.addActiveRule(xml);
        assertEquals(model.getActiveRules().size(), 1);
        Collection<XMLConfig> active = model.getActiveRules();
        assertTrue(active.contains(xml));
    }

    @Test
    public void RemoveActiveRuleTest() {
        XMLConfig xml = new XMLConfig("TestConfig");
        model.addActiveRule(xml);
        Collection<XMLConfig> active = model.getActiveRules();
        assertTrue(active.contains(xml));
        model.removeActiveRule(xml);
        assertTrue(!model.getActiveRules().contains(xml));
    }
}
