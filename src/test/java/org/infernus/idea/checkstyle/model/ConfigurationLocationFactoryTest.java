package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * <p>
 * Checks that overload methods are grouped together. Example:
 * </p>
 * <pre>
 * {@code
 * public void foo(int i) {}
 * public void foo(String s) {}
 * public void notFoo() {} // Have to be after foo(int i, String s)
 * public void foo(int i, String s) {}
 * }
 * </pre>
 * <p>
 * An example of how to configure the check is:
 * </p>
 *
 * <pre>
 * &lt;module name="OverloadMethodsDeclarationOrder"/&gt;
 * </pre>
 */
public class ConfigurationLocationFactoryTest {

    private static final Project PROJECT = Mockito.mock(Project.class);

    private final ConfigurationLocationFactory underTest = new ConfigurationLocationFactory();

    @Test
    public void aFileConfigurationLocationIsCorrectlyParsed() {
        assertThat(underTest.create(PROJECT, "LOCAL_FILE:/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml:Some checkstyle rules"),
                allOf(
                        hasProperty("location", is(oneOf(
                                "/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml",
                                "\\Users\\aUser\\Projects\\aProject\\checkstyle\\cs-rules.xml"))),
                        hasProperty("description", is("Some checkstyle rules"))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aTruncatedConfigurationLocationThrowsAnIllegalArgumentException() {
        underTest.create(PROJECT, "LOCAL_FILE:/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void aConfigurationLocationWithNoFieldSeparatorsThrowsAnIllegalArgumentException() {
        underTest.create(PROJECT, "LOCAL_FILE");
    }


    /**
     * Check that old config entries are converted correctly.
     */
    @Test
    public void testBundledConfigMigration() {
        ConfigurationLocation cl = underTest.create(PROJECT, "CLASSPATH:/sun_checks.xml:The default Checkstyle rules");
        assertNotNull(cl);
        assertEquals(BundledConfigurationLocation.class, cl.getClass());
        assertEquals(BundledConfig.SUN_CHECKS, ((BundledConfigurationLocation) cl).getBundledConfig());
    }

    @Test
    public void testBundledConfigSun() {
        ConfigurationLocation cls = underTest.create(PROJECT, "BUNDLED:(bundled):Sun Checks");
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
        ConfigurationLocation clg = underTest.create(PROJECT, "BUNDLED:(bundled):Google Checks");
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
