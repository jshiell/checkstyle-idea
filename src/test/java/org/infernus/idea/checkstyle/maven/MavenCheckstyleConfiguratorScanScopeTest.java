package org.infernus.idea.checkstyle.maven;

import org.infernus.idea.checkstyle.model.ScanScope;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenCheckstyleConfiguratorScanScopeTest {

    @Test
    void nullConfigElementReturnsDefaultScope() {
        assertEquals(ScanScope.getDefaultValue(), MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(null));
    }

    @Test
    void configElementMissingIncludeElementsReturnsDefaultScope() {
        assertEquals(ScanScope.getDefaultValue(), MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(new Element("configuration")));
    }

    @Test
    void configElementAllIncludeSettingsTrueReturnsAllSourcesWithTests() {
        var config = configWith("true", "true", "true");
        assertEquals(ScanScope.AllSourcesWithTests, MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    @Test
    void configElementIncludeResourcesOnlyTrueReturnsAllSources() {
        var config = configWith("true", "false", "false");
        assertEquals(ScanScope.AllSources, MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    @Test
    void configElementTestSourceDirectoryOnlyTrueReturnsJavaOnlyWithTests() {
        var config = configWith("false", "false", "true");
        assertEquals(ScanScope.JavaOnlyWithTests, MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    @Test
    void configElementAllIncludeSettingsFalseReturnsJavaOnly() {
        var config = configWith("false", "false", "false");
        assertEquals(ScanScope.JavaOnly, MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    @Test
    void configElementUnrecognisedCombinationReturnsDefaultScope() {
        var config = configWith("false", "true", "false");
        assertEquals(ScanScope.getDefaultValue(), MavenCheckstyleConfigurator.getScanScopeFromMavenConfig(config));
    }

    private static Element configWith(final String includeResources,
                                      final String includeTestResources,
                                      final String includeTestSourceDirectory) {
        var element = new Element("configuration");
        element.addContent(new Element("includeResources").setText(includeResources));
        element.addContent(new Element("includeTestResources").setText(includeTestResources));
        element.addContent(new Element("includeTestSourceDirectory").setText(includeTestSourceDirectory));
        return element;
    }
}
