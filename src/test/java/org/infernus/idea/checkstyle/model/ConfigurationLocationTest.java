package org.infernus.idea.checkstyle.model;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;

public class ConfigurationLocationTest {

    private static final String TEST_FILE = "<module name=\"Checker\">\n" +
            "<module name=\"TestFilter\">\n" +
            "  <property name=\"file\" value=\"${property-one}/a-file.xml\"/>\n" +
            "  <property name=\"url\" value=\"http://${property-two}/somewhere.xml\"/>\n" +
            "  <property name=\"something\" value=\"${property-three}\"/>\n" +
            "</module>\n" +
            "</module>";

    private static final String TEST_FILE_2 = "<module name=\"Checker\">\n" +
            "<module name=\"TestFilter\">\n" +
            "  <property name=\"file\" value=\"${property-one}/a-file.xml\"/>\n" +
            "  <property name=\"url\" value=\"http://${property-two}/somewhere.xml\"/>\n" +
            "  <property name=\"something\" value=\"${property-four}\"/>\n" +
            "</module>\n" +
            "</module>";
    private static final String PROPERTY_ONE = "property-one";
    private static final String PROPERTY_TWO = "property-two";
    private static final String A_VALUE = "aValue";
    private static final String A_NEW_DESCRIPTION = "aNewDescription";
    private static final String A_LOCATION = "aLocation";

    private TestConfigurationLocation underTest;

    @Before
    public void setUp() {
        underTest = new TestConfigurationLocation(TEST_FILE);
        underTest.setDescription("aDescription");
    }

    @Test
    public void whenReadPropertiesAreExtracted() throws IOException {
        underTest.resolve();

        assertThat(underTest.getProperties(), hasEntry(PROPERTY_ONE, ""));
        assertThat(underTest.getProperties(), hasEntry(PROPERTY_TWO, ""));
        assertThat(underTest.getProperties(), hasEntry("property-three", ""));
    }

    @Test
    public void propertiesAreRereadWhenTheLocationIsChanged() throws IOException {
        underTest.resolve();

        underTest.setLocation(TEST_FILE_2);
        underTest.resolve();

        assertThat(underTest.getProperties(), hasEntry(PROPERTY_ONE, ""));
        assertThat(underTest.getProperties(), hasEntry(PROPERTY_TWO, ""));
        assertThat(underTest.getProperties(), hasEntry("property-four", ""));
        assertThat(underTest.getProperties(), not(hasKey("property-three")));
    }

    @Test
    public void propertyValuesAreRetainedWhenThePropertiesAreReread() throws IOException {
        underTest.resolve();

        updatePropertyOn(underTest, PROPERTY_TWO, A_VALUE);

        underTest.setLocation(TEST_FILE_2);
        underTest.resolve();

        assertThat(underTest.getProperties(), hasEntry(PROPERTY_TWO, A_VALUE));
    }

    @Test
    public void theDescriptionIsSetToThePassedStringWhenNotNull() {
        underTest.setDescription(A_NEW_DESCRIPTION);

        assertThat(underTest.getDescription(), is(equalTo(A_NEW_DESCRIPTION)));
    }

    @Test
    public void theDescriptionDefaultsToTheLocationWhenANullValueIsGiven() {
        underTest.setLocation(A_LOCATION);
        underTest.setDescription(null);

        assertThat(underTest.getDescription(), is(equalTo(A_LOCATION)));
    }

    @Test
    public void anUnmodifiedLocationIsNotMarkedAsChanged() throws IOException {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);

        assertThat(location1.hasChangedFrom(location2), is(false));
    }

    @Test
    public void aLocationIsChangedIfTheLocationValueHasChanged() throws IOException {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);

        location1.setLocation("aNewLocation");

        assertThat(location1.hasChangedFrom(location2), is(true));
    }

    @Test
    public void aLocationIsChangedIfTheDescriptionValueHasChanged() throws IOException {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);

        location1.setDescription(A_NEW_DESCRIPTION);

        assertThat(location1.hasChangedFrom(location2), is(true));
    }

    @Test
    public void aLocationIsChangedIfThePropertiesHaveChanged() throws IOException {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);

        updatePropertyOn(location1, PROPERTY_TWO, A_VALUE);

        assertThat(location1.hasChangedFrom(location2), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aLocationCannotBeCreatedWithANullType() {
        new ConfigurationLocation(null) {
            @NotNull
            @Override
            protected InputStream resolveFile() throws IOException {
                return null;
            }

            @Override
            public Object clone() {
                return this;
            }
        };
    }

    @Test
    public void aDescriptorContainsTheLocationDescriptionAndType() {
        final ConfigurationLocation location = new TestConfigurationLocation(A_LOCATION);

        assertThat(location.getDescriptor(), is(equalTo(format("%s:%s:%s",
                location.getType(), location.getLocation(), location.getDescription()))));
    }

    @Test
    public void equalsIgnoresProperties() throws IOException {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        updatePropertyOn(location1, PROPERTY_ONE, A_VALUE);

        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);
        updatePropertyOn(location2, PROPERTY_ONE, "anotherValue");

        assertThat(location1, is(equalTo(location2)));
    }

    @Test
    public void hashCodeIgnoresProperties() throws IOException {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        updatePropertyOn(location1, PROPERTY_ONE, A_VALUE);

        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);
        updatePropertyOn(location2, PROPERTY_ONE, "anotherValue");

        assertThat(location1.hashCode(), is(equalTo(location2.hashCode())));
    }

    @Test
    public void toStringReturnsTheDescription() {
        assertThat(underTest.toString(), is(equalTo("aDescription")));
    }

    private void updatePropertyOn(final TestConfigurationLocation configurationLocation,
                                  final String propertyKey,
                                  final String propertyValue) throws IOException {
        final Map<String,String> properties = new HashMap<>(underTest.getProperties());
        properties.put(propertyKey, propertyValue);
        configurationLocation.setProperties(properties);
    }

    private class TestConfigurationLocation extends ConfigurationLocation {
        public TestConfigurationLocation(final String content) {
            super(ConfigurationType.LOCAL_FILE);

            setLocation(content);
        }

        @NotNull
        @Override
        protected InputStream resolveFile() throws IOException {
            return new ByteArrayInputStream(getLocation().getBytes());
        }

        @Override
        public Object clone() {
            return new TestConfigurationLocation(getLocation());
        }
    }

}
