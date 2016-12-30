package org.infernus.idea.checkstyle.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleActions;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.entities.InfernusConfigurationsObject;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;


public class ServiceLayerBasicTest
{
    private static final Project PROJECT = Mockito.mock(Project.class);

    private static CheckstyleProjectService sCheckstyleProjectService = null;

    private static final String CONFIG_FILE_1 = "config1-6.16.1-but-not-6.17.xml";
    private static final String CONFIG_FILE_2 = "config2-6.19-but-not-6.17.xml";
    private static final String CONFIG_FILE_3 = "config3-6.19-but-not-6.6.xml";


    @BeforeClass
    public static void setUp() {
        sCheckstyleProjectService = new CheckstyleProjectService(PROJECT);
    }

    @AfterClass
    public static void tearDown() {
        sCheckstyleProjectService = null;
    }


    /**
     * Test that a check property removed with 6.16.1 cannot be used with later runtimes.
     */
    @Test
    public void testConfig1() throws IOException, URISyntaxException {
        try {
            createChecker(CONFIG_FILE_1);

            if (CsVersionInfo.isGreaterThan("6.16.1")) {
                Assert.fail("expected exception was not thrown");
            }
        } catch (CheckstyleToolException e) {
            if (CsVersionInfo.isGreaterThan("6.16.1")) {
                // expected
                Assert.assertTrue(e.getMessage().contains("basenameSeparator"));
            } else {
                throw e;  // test failed
            }
        }
    }


    /**
     * Test that a check introduced with 6.19 cannot be loaded with earlier runtimes.
     */
    @Test
    public void testConfig2() throws IOException, URISyntaxException {
        try {
            createChecker(CONFIG_FILE_2);

            if (CsVersionInfo.isLessThan("6.19")) {
                Assert.fail("expected exception was not thrown");
            }
        } catch (CheckstyleToolException e) {
            if (CsVersionInfo.isLessThan("6.19")) {
                // expected
                Assert.assertTrue(e.getMessage().contains("SingleSpaceSeparator"));
            } else {
                throw e;  // test failed
            }
        }
    }


    /**
     * Test that a custom check which uses part of the API that was broken in 6.6 and 6.7 does not work with these
     * versions of Checkstyle.
     */
    @Test
    public void testConfig3() throws IOException, URISyntaxException {
        //noinspection ThrowableNotThrown
        CustomCheck3.popErrorOccurred4UnitTest();

        final CheckStyleChecker checker = createChecker(CONFIG_FILE_3);
        runChecker(checker);

        final Throwable errorOccurred = CustomCheck3.popErrorOccurred4UnitTest();
        if (CsVersionInfo.isOneOf("6.6", "6.7")) {
            Assert.assertNotNull(errorOccurred);  // redundant with next line, but clearer message
            Assert.assertTrue(errorOccurred instanceof NoSuchMethodError);
            Assert.assertTrue(errorOccurred.getMessage().contains("getFilename"));
        } else {
            Assert.assertNull(errorOccurred);
        }
    }


    private CheckStyleChecker createChecker(@NotNull final String pConfigXmlFile) throws IOException,
            URISyntaxException {

        final ConfigurationLocation configLoc = new StringConfigurationLocation(readFile(pConfigXmlFile));

        final Module module = Mockito.mock(Module.class);
        Mockito.when(module.getProject()).thenReturn(PROJECT);

        final Configurations configurations = Mockito.mock(Configurations.class);
        Mockito.when(configurations.tabWidth(Mockito.any())).thenReturn(2);
        Mockito.when(configurations.baseDir(Mockito.any())).thenReturn(  //
                Optional.of(new File(getClass().getResource(pConfigXmlFile).toURI()).getParent()));

        final CheckstyleActions csInstance = sCheckstyleProjectService.getCheckstyleInstance();
        return csInstance.createChecker(module, configLoc, Collections.emptyMap(), new InfernusConfigurationsObject
                (configurations));
    }


    private void runChecker(@NotNull final CheckStyleChecker pChecker) throws URISyntaxException {

        final File sourceFile = new File(getClass().getResource("SourceFile.java").toURI());

        final ScannableFile file1 = Mockito.mock(ScannableFile.class);
        Mockito.when(file1.getFile()).thenReturn(sourceFile);
        final List<ScannableFile> filesToScan = Collections.singletonList(file1);

        final CheckstyleActions csInstance = sCheckstyleProjectService.getCheckstyleInstance();
        csInstance.scan(pChecker.getCheckerWithConfig4UnitTest(), filesToScan, false, 2, //
                Optional.of(sourceFile.getParent()));
    }


    private String readFile(@NotNull final String pFilename) throws IOException, URISyntaxException {
        URL url = getClass().getResource(pFilename);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(pFilename);
        }
        Assert.assertNotNull(url);
        return new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
    }
}
