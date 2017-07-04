package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;


public class ConfigurationLocationFactoryTest
{
    private static final Project PROJECT = Mockito.mock(Project.class);

    private final ConfigurationLocationFactory underTest = new ConfigurationLocationFactory();


    @Test
    public void aFileConfigurationLocationIsCorrectlyParsed()
    {
        assertThat(underTest.create(PROJECT,
                "LOCAL_FILE:/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml:Some checkstyle rules"), allOf
                (hasProperty("location", isOneOf("/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml",
                        "\\Users\\aUser\\Projects\\aProject\\checkstyle\\cs-rules.xml")), hasProperty("description",
                        is("Some checkstyle rules"))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aTruncatedConfigurationLocationThrowsAnIllegalArgumentException()
    {
        underTest.create(PROJECT, "LOCAL_FILE:/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void aConfigurationLocationWithNoFieldSeparatorsThrowsAnIllegalArgumentException()
    {
        underTest.create(PROJECT, "LOCAL_FILE");
    }


    /**
     * Check that old config entries are converted correctly.
     */
    @Test
    public void testBundledConfigMigration()
    {
        ConfigurationLocation cl = underTest.create(PROJECT, "CLASSPATH:/sun_checks.xml:The default Checkstyle rules");
        Assert.assertNotNull(cl);
        Assert.assertEquals(BundledConfigurationLocation.class, cl.getClass());
        Assert.assertEquals(BundledConfig.SUN_CHECKS, ((BundledConfigurationLocation) cl).getBundledConfig());
    }

    @Test
    public void testBundledConfigSun()
    {
        ConfigurationLocation cls = underTest.create(PROJECT, "BUNDLED:SUN_CHECKS:ignored description");
        Assert.assertNotNull(cls);
        Assert.assertEquals(BundledConfigurationLocation.class, cls.getClass());
        final BundledConfigurationLocation bcl = (BundledConfigurationLocation) cls;
        Assert.assertEquals(BundledConfig.SUN_CHECKS, bcl.getBundledConfig());
        Assert.assertEquals(BundledConfig.SUN_CHECKS.getDescription(), bcl.getDescription());
        Assert.assertEquals(BundledConfig.SUN_CHECKS.getPath(), bcl.getLocation());
        Assert.assertTrue(bcl.getProperties().isEmpty());
    }

    @Test
    public void testBundledConfigGoogle()
    {
        ConfigurationLocation clg = underTest.create(PROJECT, "BUNDLED:GOOGLE_CHECKS:ignored description");
        Assert.assertNotNull(clg);
        Assert.assertEquals(BundledConfigurationLocation.class, clg.getClass());
        final BundledConfigurationLocation bcl = (BundledConfigurationLocation) clg;
        Assert.assertEquals(BundledConfig.GOOGLE_CHECKS, bcl.getBundledConfig());
        Assert.assertEquals(BundledConfig.GOOGLE_CHECKS.getDescription(), bcl.getDescription());
        Assert.assertEquals(BundledConfig.GOOGLE_CHECKS.getPath(), bcl.getLocation());
        Assert.assertTrue(bcl.getProperties().isEmpty());
    }
}
