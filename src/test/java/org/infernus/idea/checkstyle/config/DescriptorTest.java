package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.BundledConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class DescriptorTest {

    private final Project project = TestHelper.mockProject();

    @Before
    public void setUp() {
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(new ConfigurationLocationFactory());
        when(project.getService(ProjectFilePaths.class)).thenReturn(new ProjectFilePaths(project));
    }

    @Test
    public void aFileConfigurationLocationIsCorrectlyParsed() {
        assertThat(Descriptor.parse("LOCAL_FILE:/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml:Some checkstyle rules", project).toConfigurationLocation(project),
                allOf(
                        hasProperty("location", is(oneOf(
                                "/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml",
                                "\\Users\\aUser\\Projects\\aProject\\checkstyle\\cs-rules.xml"))),
                        hasProperty("description", is("Some checkstyle rules"))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aTruncatedConfigurationLocationThrowsAnIllegalArgumentException() {
        Descriptor.parse("LOCAL_FILE:/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml", project).toConfigurationLocation(project);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aConfigurationLocationWithNoFieldSeparatorsThrowsAnIllegalArgumentException() {
        Descriptor.parse("LOCAL_FILE", project).toConfigurationLocation(project);
    }

    /**
     * Check that old config entries are converted correctly.
     */
    @Test
    public void testBundledConfigMigration() {
        ConfigurationLocation cl = Descriptor.parse("CLASSPATH:/sun_checks.xml:The default Checkstyle rules", project).toConfigurationLocation(project);
        assertNotNull(cl);
        assertEquals(BundledConfigurationLocation.class, cl.getClass());
        assertEquals(BundledConfig.SUN_CHECKS, ((BundledConfigurationLocation) cl).getBundledConfig());
    }

    @Test
    public void testBundledConfigSun() {
        ConfigurationLocation cls = Descriptor.parse("BUNDLED:(bundled):Sun Checks", project).toConfigurationLocation(project);
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
        ConfigurationLocation clg = Descriptor.parse("BUNDLED:(bundled):Google Checks", project).toConfigurationLocation(project);
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
