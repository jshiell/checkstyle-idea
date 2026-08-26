package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.config.Descriptor;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ConfigurationLocationTest {

    private static final String TEST_FILE = """
            <module name="Checker">
            <module name="TestFilter">
              <property name="file" value="${property-one}/a-file.xml"/>
              <property name="url" value="http://${property-two}/somewhere.xml"/>
              <property name="something" value="${property-three}"/>
            </module>
            </module>""";

    private static final String TEST_FILE_2 = """
            <module name="Checker">
            <module name="TestFilter">
              <property name="file" value="${property-one}/a-file.xml"/>
              <property name="url" value="http://${property-two}/somewhere.xml"/>
              <property name="something" value="${property-four}"/>
            </module>
            </module>""";

    private static final String TEST_FILE_WITH_UNDECLARED_ENTITY = """
            <module name="Checker">
            <module name="TestFilter">&someUndeclaredEntity;</module>
            </module>""";

    private TestConfigurationLocation underTest;

    @BeforeEach
    public void setUp() {
        underTest = new TestConfigurationLocation(TEST_FILE);
        underTest.setDescription("aDescription");
    }

    @Test
    public void whenReadPropertiesAreExtracted() throws IOException {
        underTest.resolve(getClass().getClassLoader()).close();

        assertThat(underTest.getProperties(), hasEntry("property-one", ""));
        assertThat(underTest.getProperties(), hasEntry("property-two", ""));
        assertThat(underTest.getProperties(), hasEntry("property-three", ""));
    }

    @Test
    public void propertiesAreRereadWhenTheLocationIsChanged() throws IOException {
        underTest.resolve(getClass().getClassLoader()).close();

        underTest.setLocation(TEST_FILE_2);
        underTest.resolve(getClass().getClassLoader()).close();

        assertThat(underTest.getProperties(), hasEntry("property-one", ""));
        assertThat(underTest.getProperties(), hasEntry("property-two", ""));
        assertThat(underTest.getProperties(), hasEntry("property-four", ""));
        assertThat(underTest.getProperties(), not(hasKey("property-three")));
    }

    @Test
    public void propertyValuesAreRetainedWhenThePropertiesAreReread() throws IOException {
        underTest.resolve(getClass().getClassLoader()).close();

        updatePropertyOn(underTest, "property-two", "aValue");

        underTest.setLocation(TEST_FILE_2);
        underTest.resolve(getClass().getClassLoader()).close();

        assertThat(underTest.getProperties(), hasEntry("property-two", "aValue"));
    }

    @Test
    public void propertiesAreRetainedWhenTheFileCannotBeScanned() throws IOException {
        underTest.resolve(getClass().getClassLoader()).close();
        updatePropertyOn(underTest, "property-two", "aValue");

        underTest.setLocation(TEST_FILE_WITH_UNDECLARED_ENTITY);
        underTest.resolve(getClass().getClassLoader()).close();

        assertThat(underTest.getProperties(), hasEntry("property-one", ""));
        assertThat(underTest.getProperties(), hasEntry("property-two", "aValue"));
        assertThat(underTest.getProperties(), hasEntry("property-three", ""));
    }

    @Test
    public void thereIsNoBaseUriByDefault() {
        assertThat(underTest.baseUri(), is(nullValue()));
    }

    @Test
    public void theDescriptionIsSetToThePassedStringWhenNotNull() {
        underTest.setDescription("aNewDescription");

        assertThat(underTest.getDescription(), is(equalTo("aNewDescription")));
    }

    @Test
    public void theDescriptionDefaultsToTheLocationWhenANullValueIsGiven() {
        underTest.setLocation("aLocation");
        underTest.setDescription(null);

        assertThat(underTest.getDescription(), is(equalTo("aLocation")));
    }

    @Test
    public void anUnmodifiedLocationIsNotMarkedAsChanged() {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);

        assertThat(location1.hasChangedFrom(location2), is(false));
    }

    @Test
    public void aLocationIsChangedIfTheLocationValueHasChanged() {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);

        location1.setLocation("aNewLocation");

        assertThat(location1.hasChangedFrom(location2), is(true));
    }

    @Test
    public void aLocationIsChangedIfTheDescriptionValueHasChanged() {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);

        location1.setDescription("aNewDescription");

        assertThat(location1.hasChangedFrom(location2), is(true));
    }

    @Test
    public void aLocationIsChangedIfThePropertiesHaveChanged() {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);

        updatePropertyOn(location1, "property-two", "aValue");

        assertThat(location1.hasChangedFrom(location2), is(true));
    }

    @Test
    public void aLocationsPropertiesAreIgnoredIfInTheDefaultProjectAndItCannotBeResolvedInTheDefaultProject() {
        final DefaultProjectTestConfigurationLocation location1 = new DefaultProjectTestConfigurationLocation();
        final DefaultProjectTestConfigurationLocation location2 = new DefaultProjectTestConfigurationLocation();

        updatePropertyOn(location1, "property-two", "aValue");

        assertThat(location1.hasChangedFrom(location2), is(false));
    }

    @Test
    public void aDescriptorContainsTheLocationDescriptionAndType() {
        final ConfigurationLocation location = new TestConfigurationLocation("aLocation");

        assertThat(Descriptor.of(location, location.getProject()).toString(), is(equalTo(format("%s:%s:%s;%s",
                location.getType(), location.getLocation(), location.getDescription(), location.getNamedScope().orElseThrow().getScopeId()))));
    }

    @Test
    public void equalsIgnoresProperties() {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        updatePropertyOn(location1, "property-one", "aValue");

        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);
        updatePropertyOn(location2, "property-one", "anotherValue");

        assertThat(location1, is(equalTo(location2)));
    }

    @Test
    public void hashCodeIgnoresProperties() {
        final TestConfigurationLocation location1 = new TestConfigurationLocation(TEST_FILE);
        updatePropertyOn(location1, "property-one", "aValue");

        final TestConfigurationLocation location2 = new TestConfigurationLocation(TEST_FILE);
        updatePropertyOn(location2, "property-one", "anotherValue");

        assertThat(location1.hashCode(), is(equalTo(location2.hashCode())));
    }

    @Test
    public void toStringReturnsTheDescription() {
        assertThat(underTest.toString(), is(equalTo("aDescription")));
    }

    @Test
    public void checkModuleFileResolvesEachModuleAgainstItsOwnDirectory(@TempDir final Path moduleOneDir,
                                                                        @TempDir final Path moduleTwoDir) throws IOException {
        final String fileName = "csidea-test-fixture-537.xml";
        Files.writeString(moduleOneDir.resolve(fileName), "module one's copy");
        Files.writeString(moduleTwoDir.resolve(fileName), "module two's copy");

        final Project project = TestHelper.mockProject();
        final ProjectPaths projectPaths = mock(ProjectPaths.class);
        when(project.getService(ProjectPaths.class)).thenReturn(projectPaths);

        final Module moduleOne = mockModuleIn(moduleOneDir, projectPaths);
        final Module moduleTwo = mockModuleIn(moduleTwoDir, projectPaths);

        final TestConfigurationLocation location = new TestConfigurationLocation(TEST_FILE, project);

        final String resolvedForModuleOne =
                location.resolveAssociatedFile(fileName, moduleOne, getClass().getClassLoader());
        final String resolvedForModuleTwo =
                location.resolveAssociatedFile(fileName, moduleTwo, getClass().getClassLoader());

        assertThat(resolvedForModuleOne, is(not(equalTo(resolvedForModuleTwo))));
        assertThat(resolvedForModuleOne, is(equalTo(moduleOneDir.resolve(fileName).toString())));
        assertThat(resolvedForModuleTwo, is(equalTo(moduleTwoDir.resolve(fileName).toString())));
    }

    /**
     * Module.getComponent(ModuleRootManager.class) is what {@code ModuleRootManager.getInstance(module)}
     * resolves to under the hood - confirmed by decompiling the platform's ModuleRootManager class.
     */
    private Module mockModuleIn(final Path moduleDir, final ProjectPaths projectPaths) {
        final Module module = mock(Module.class);

        final ModuleRootManager rootManager = mock(ModuleRootManager.class);
        when(rootManager.getContentEntries()).thenReturn(new ContentEntry[0]);
        when(module.getComponent(ModuleRootManager.class)).thenReturn(rootManager);

        final VirtualFile moduleVirtualDir = mock(VirtualFile.class);
        when(moduleVirtualDir.getPath()).thenReturn(moduleDir.toString());
        when(projectPaths.modulePath(module)).thenReturn(moduleVirtualDir);

        return module;
    }

    private void updatePropertyOn(final ConfigurationLocation configurationLocation,
                                  final String propertyKey,
                                  final String propertyValue) {
        final Map<String, String> properties = new HashMap<>(underTest.getProperties());
        properties.put(propertyKey, propertyValue);
        configurationLocation.setProperties(properties);
    }

    private static class DefaultProjectTestConfigurationLocation extends ConfigurationLocation {
        DefaultProjectTestConfigurationLocation() {
            super("anId", ConfigurationType.LOCAL_FILE, TestHelper.mockProject());

            when(getProject().isDefault()).thenReturn(true);
        }

        @Override
        public boolean canBeResolvedInDefaultProject() {
            return false;
        }

        @NotNull
        @Override
        protected InputStream resolveFile(@NotNull final ClassLoader checkstyleClassLoader) {
            throw new RuntimeException("Can't be called in default project");
        }

        @Override
        public Object clone() {
            return new DefaultProjectTestConfigurationLocation();
        }
    }

    private static class TestConfigurationLocation extends ConfigurationLocation {

        TestConfigurationLocation(final String content) {
            this(content, TestHelper.mockProject());
        }

        TestConfigurationLocation(final String content, final Project project) {
            super("anId", ConfigurationType.LOCAL_FILE, project);

            setLocation(content);
            setNamedScope(TestHelper.NAMED_SCOPE);
        }

        @NotNull
        @Override
        protected InputStream resolveFile(@NotNull final ClassLoader checkstyleClassLoader) {
            return new ByteArrayInputStream(getLocation().getBytes());
        }

        @Override
        public Object clone() {
            return new TestConfigurationLocation(getLocation());
        }
    }


    @Test
    public void testSorting() {
        final Project project = TestHelper.mockProject();
        when(project.getService(ProjectFilePaths.class)).thenReturn(ProjectFilePaths.testInstanceWith(project, new ProjectPaths()));

        List<ConfigurationLocation> list = new ArrayList<>();
        FileConfigurationLocation fcl = new FileConfigurationLocation(project, "id1", ConfigurationType.LOCAL_FILE);
        fcl.setDescription("descB");
        fcl.setLocation("locB");
        list.add(fcl);
        RelativeFileConfigurationLocation rfcl1 = new RelativeFileConfigurationLocation(project, "id2");
        rfcl1.setDescription("descA");
        rfcl1.setLocation("locA");
        list.add(rfcl1);
        list.add(new BundledConfigurationLocation(BundledConfig.SUN_CHECKS, project));
        RelativeFileConfigurationLocation rfcl2 = new RelativeFileConfigurationLocation(project, "id4");
        rfcl2.setDescription("descC");
        rfcl2.setLocation("locC");
        list.add(rfcl2);
        list.add(new BundledConfigurationLocation(BundledConfig.GOOGLE_CHECKS, project));

        Collections.sort(list);

        assertEquals(BundledConfigurationLocation.class, list.get(0).getClass());
        assertTrue(list.get(0).getDescription().contains("Sun Checks"));
        assertTrue(list.contains(new BundledConfigurationLocation(BundledConfig.SUN_CHECKS, project)));
        assertEquals(BundledConfigurationLocation.class, list.get(1).getClass());
        assertTrue(list.get(1).getDescription().contains("Google Checks"));
        assertTrue(list.contains(new BundledConfigurationLocation(BundledConfig.GOOGLE_CHECKS, project)));
        assertEquals(RelativeFileConfigurationLocation.class, list.get(2).getClass());
        assertEquals("descA", list.get(2).getDescription());
        assertEquals(FileConfigurationLocation.class, list.get(3).getClass());
        assertEquals("descB", list.get(3).getDescription());
        assertEquals(RelativeFileConfigurationLocation.class, list.get(4).getClass());
        assertEquals("descC", list.get(4).getDescription());
    }
}
