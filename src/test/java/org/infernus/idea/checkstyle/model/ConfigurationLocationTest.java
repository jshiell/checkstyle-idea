package org.infernus.idea.checkstyle.model;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ConfigurationLocationTest {

    private static final String TEST_FILE = "<module name=\"Checker\">\n" +
            "<module name=\"TestFilter\">\n" +
            "  <property name=\"file\" value=\"${property-one}/a-file.xml\"/>\n" +
            "  <property name=\"url\" value=\"http://${property-two}/somewhere.xml\"/>\n" +
            "  <property name=\"something\" value=\"${property-three}\"/>\n" +
            "</module>\n" +
            "</module>";

    private TestConfigurationLocation unit;

    @Before
    public void setUp() {
        unit = new TestConfigurationLocation();
    }

    @Test
    public void whenReadPropertiesAreExtracted() throws IOException {
        unit.resolve();

        assertThat(unit.getProperties(), hasEntry("property-one", null));
        assertThat(unit.getProperties(), hasEntry("property-two", null));
        assertThat(unit.getProperties(), hasEntry("property-three", null));
    }


    private class TestConfigurationLocation extends ConfigurationLocation {
        public TestConfigurationLocation() {
            super(ConfigurationType.FILE);
        }

        @Override
        protected InputStream resolveFile() throws IOException {
            return new ByteArrayInputStream(TEST_FILE.getBytes());
        }
    }

}
