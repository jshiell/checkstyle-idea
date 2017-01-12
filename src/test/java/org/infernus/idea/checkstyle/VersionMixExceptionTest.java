package org.infernus.idea.checkstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.mockito.Mockito;


public class VersionMixExceptionTest
        extends LightPlatformTestCase
{
    private static final Project PROJECT = Mockito.mock(Project.class);

    private static final String CONFIG_FILE = "config-ok.xml";

    private static final String PROPS_FILE_NAME = "/checkstyle-idea.properties";

    private static final String BASE_VERSION = readBaseVersion();
    private static final String OTHER_VERSION = "6.19";

    private CheckstyleProjectService csService;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        csService = new CheckstyleProjectService(PROJECT);
        csService.activateCheckstyleVersion(BASE_VERSION, null);
        CheckstyleProjectService.activateMock4UnitTesting(csService);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            CheckstyleProjectService.activateMock4UnitTesting(null);
            csService = null;
        } finally {
            super.tearDown();
        }
    }


    /**
     * Test that a {@link CheckstyleVersionMixException} is thrown when a
     * {@link org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject CheckstyleInternalObject} outlives its class
     * loader.
     */
    public void testVersionMixException() throws IOException, URISyntaxException {

        Module module = Mockito.mock(Module.class);
        Mockito.when(module.getProject()).thenReturn(PROJECT);

        final CheckStyleChecker checker = createChecker(module);

        runChecker(checker);
        try {
            Assert.assertNotEquals(OTHER_VERSION, BASE_VERSION);
            csService.activateCheckstyleVersion(OTHER_VERSION, null);    // changes class loader, cause of error
            runChecker(checker);
            Assert.fail("expected exception was not thrown");
        } catch (CheckstyleVersionMixException e) {
            // expected
            final String internalClassName = "org.infernus.idea.checkstyle.service.entities.CheckerWithConfig";
            Assert.assertTrue(e.getMessage().contains("Expected: " + internalClassName + ", actual: " +
                    internalClassName));
            // Yes! Error, even though both class names are identical (but class loaders differ).
        } finally {
            csService.activateCheckstyleVersion(BASE_VERSION, null);
        }
    }


    /**
     * Test that everything works fine even when class loaders change, provided that
     * {@link org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject CheckstyleInternalObject}s associated with
     * the expired class loaders are recreated.
     */
    public void testSunnyDay() throws IOException, URISyntaxException {

        Module module = Mockito.mock(Module.class);
        Mockito.when(module.getProject()).thenReturn(PROJECT);

        CheckStyleChecker checker = createChecker(module);
        runChecker(checker);

        try {
            Assert.assertNotEquals(OTHER_VERSION, BASE_VERSION);
            csService.activateCheckstyleVersion(OTHER_VERSION, null);

            checker = createChecker(module);
            runChecker(checker);
        } finally {
            csService.activateCheckstyleVersion(BASE_VERSION, null);
        }
    }


    private CheckStyleChecker createChecker(@NotNull final Module pModule) throws URISyntaxException, IOException {
        final ConfigurationLocation configLoc = new StringConfigurationLocation(readFile(CONFIG_FILE));

        final TabWidthAndBaseDirProvider configurations = Mockito.mock(TabWidthAndBaseDirProvider.class);
        Mockito.when(configurations.tabWidth()).thenReturn(2);
        Mockito.when(configurations.baseDir()).thenReturn(  //
                Optional.of(new File(getClass().getResource(CONFIG_FILE).toURI()).getParent()));

        final CheckStyleChecker checker = csService.getCheckstyleInstance().createChecker(pModule, configLoc,
                Collections.emptyMap(), configurations, getClass().getClassLoader());
        return checker;
    }


    private void runChecker(@NotNull final CheckStyleChecker pChecker) throws URISyntaxException {

        final File sourceFile = new File(getClass().getResource("SourceFile.java").toURI());

        final ScannableFile file1 = Mockito.mock(ScannableFile.class);
        Mockito.when(file1.getFile()).thenReturn(sourceFile);
        final List<ScannableFile> filesToScan = Collections.singletonList(file1);

        final CheckstyleActions csInstance = csService.getCheckstyleInstance();
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


    @NotNull
    private static String readBaseVersion() {
        String result = null;
        try (InputStream is = VersionMixExceptionTest.class.getResourceAsStream(PROPS_FILE_NAME)) {
            Properties props = new Properties();
            props.load(is);
            result = props.getProperty("baseVersion");
        } catch (IOException e) {
            throw new CheckStylePluginException("internal error - Failed to read property file: " + PROPS_FILE_NAME, e);
        }
        Assert.assertNotNull(result);
        return result;
    }
}
