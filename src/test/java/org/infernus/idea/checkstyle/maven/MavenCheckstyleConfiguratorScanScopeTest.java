package org.infernus.idea.checkstyle.maven;

import org.infernus.idea.checkstyle.model.ScanScope;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenCheckstyleConfiguratorScanScopeTest {

    @Test
    void nullConfigElement_returnsDefaultScope() {
        assertEquals(ScanScope.getDefaultValue(), MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(null));
    }

    @Test
    void configElement_missingIncludeElements_returnsDefaultScope() {
        assertEquals(ScanScope.getDefaultValue(), MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(new Element("configuration")));
    }

    @Test
    void configElement_allIncludeSettingsTrue_returnsAllSourcesWithTests() {
        var config = configWith("true", "true", "true");
        assertEquals(ScanScope.AllSourcesWithTests, MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    @Test
    void configElement_includeResourcesOnlyTrue_returnsAllSources() {
        var config = configWith("true", "false", "false");
        assertEquals(ScanScope.AllSources, MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    @Test
    void configElement_testSourceDirectoryOnlyTrue_returnsJavaOnlyWithTests() {
        var config = configWith("false", "false", "true");
        assertEquals(ScanScope.JavaOnlyWithTests, MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    @Test
    void configElement_allIncludeSettingsFalse_returnsJavaOnly() {
        var config = configWith("false", "false", "false");
        assertEquals(ScanScope.JavaOnly, MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    @Test
    void configElement_unrecognisedCombination_returnsDefaultScope() {
        var config = configWith("false", "true", "false");
        assertEquals(ScanScope.getDefaultValue(), MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    private static Element configWith(String includeResources, String includeTestResources, String includeTestSourceDirectory) {
        var element = new Element("configuration");
        element.addContent(new Element("includeResources").setText(includeResources));
        element.addContent(new Element("includeTestResources").setText(includeTestResources));
        element.addContent(new Element("includeTestSourceDirectory").setText(includeTestSourceDirectory));
        return element;
    }
}
