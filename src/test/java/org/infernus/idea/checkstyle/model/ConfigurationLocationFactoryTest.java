package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


public class ConfigurationLocationFactoryTest {

    private final Project project = TestHelper.mockProject();
    private final NamedScope allScope = NamedScopeHelper.getScopeByIdWithDefaultFallback(project, "All");

    private final ConfigurationLocationFactory underTest = new ConfigurationLocationFactory();

    @Before
    public void setUp() {
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(underTest);
        when(project.getService(ProjectFilePaths.class)).thenReturn(new ProjectFilePaths(project));
    }

    @Test
    public void aFileConfigurationLocationIsCorrectlyParsed() {
        assertThat(underTest.create(project, "anId", ConfigurationType.LOCAL_FILE, "/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml", "Some checkstyle rules", allScope),
                allOf(
                        hasProperty("location", is(oneOf(
                                "/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml",
                                "\\Users\\aUser\\Projects\\aProject\\checkstyle\\cs-rules.xml"))),
                        hasProperty("description", is("Some checkstyle rules"))));
    }

    /**
     * Check that old config entries are converted correctly.
     */
    @Test
    public void testBundledConfigMigration() {
        ConfigurationLocation cl = underTest.create(project, "anId", ConfigurationType.LEGACY_CLASSPATH, "/sun_checks.xml", "The default Checkstyle rules", allScope);
        assertNotNull(cl);
        assertEquals(BundledConfigurationLocation.class, cl.getClass());
        assertEquals(BundledConfig.SUN_CHECKS, ((BundledConfigurationLocation) cl).getBundledConfig());
    }

    @Test
    public void testBundledConfigSun() {
        ConfigurationLocation cls = underTest.create(project, "anId", ConfigurationType.BUNDLED, "(bundled)", "Sun Checks", allScope);
        assertNotNull(cls);
        assertEquals(BundledConfigurationLocation.class, cls.getClass());
        final BundledConfigurationLocation bcl = (BundledConfigurationLocation) cls;
        assertEquals(BundledConfig.SUN_CHECKS, bcl.getBundledConfig());
        assertEquals(BundledConfig.SUN_CHECKS.getDescription(), bcl.getDescription());
        assertEquals(BundledConfig.SUN_CHECKS.getLocation(), bcl.getLocation());
        assertEquals(BundledConfig.SUN_CHECKS.getPath(), bcl.getBundledConfig().getPath());
        assertTrue(bcl.getProperties().isEmpty());
    }

    @Test
    public void testBundledConfigGoogle() {
        ConfigurationLocation clg = underTest.create(project, "anId", ConfigurationType.BUNDLED, "(bundled)", "Google Checks", allScope);
        assertNotNull(clg);
        assertEquals(BundledConfigurationLocation.class, clg.getClass());
        final BundledConfigurationLocation bcl = (BundledConfigurationLocation) clg;
        assertEquals(BundledConfig.GOOGLE_CHECKS, bcl.getBundledConfig());
        assertEquals(BundledConfig.GOOGLE_CHECKS.getDescription(), bcl.getDescription());
        assertEquals(BundledConfig.GOOGLE_CHECKS.getLocation(), bcl.getLocation());
        assertEquals(BundledConfig.GOOGLE_CHECKS.getPath(), bcl.getBundledConfig().getPath());
        assertTrue(bcl.getProperties().isEmpty());
    }
}
