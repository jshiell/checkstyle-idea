package org.infernus.idea.checkstyle;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class VersionMixExceptionTest extends LightPlatformTestCase {
    private static final String CONFIG_FILE = "config-ok.xml";

    private static final String PROPS_FILE_NAME = "/checkstyle-idea.properties";

    private static final String BASE_VERSION = readBaseVersion();
    private static final String OTHER_VERSION = "8.0";

    private final Project project = mock(Project.class);
    private CheckstyleProjectService csService;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PluginConfigurationManager mockPluginConfig = mock(PluginConfigurationManager.class);
        final PluginConfiguration mockConfigDto = PluginConfigurationBuilder.testInstance(BASE_VERSION).build();
        when(mockPluginConfig.getCurrent()).thenReturn(mockConfigDto);

        when(project.getService(PluginConfigurationManager.class)).thenReturn(mockPluginConfig);

        csService = new CheckstyleProjectService(project);
        csService.activateCheckstyleVersion(BASE_VERSION, null);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
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

        Module module = mock(Module.class);
        when(module.getProject()).thenReturn(project);

        final CheckStyleChecker checker = createChecker(module);

        runChecker(checker);
        try {
            assertNotEquals(OTHER_VERSION, BASE_VERSION);
            csService.activateCheckstyleVersion(OTHER_VERSION, null);    // changes class loader, cause of error
            runChecker(checker);
            fail("expected exception was not thrown");
        } catch (CheckstyleVersionMixException e) {
            // expected
            final String internalClassName = "org.infernus.idea.checkstyle.service.entities.CheckerWithConfig";
            assertTrue(e.getMessage().contains("Expected: " + internalClassName + ", actual: "
                    + internalClassName));
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

        Module module = mock(Module.class);
        when(module.getProject()).thenReturn(project);

        CheckStyleChecker checker = createChecker(module);
        runChecker(checker);

        try {
            assertNotEquals(OTHER_VERSION, BASE_VERSION);
            csService.activateCheckstyleVersion(OTHER_VERSION, null);

            checker = createChecker(module);
            runChecker(checker);
        } finally {
            csService.activateCheckstyleVersion(BASE_VERSION, null);
        }
    }


    private CheckStyleChecker createChecker(@NotNull final Module pModule) throws URISyntaxException, IOException {
        final ConfigurationLocation configLoc = new StringConfigurationLocation(readFile(CONFIG_FILE), TestHelper.mockProject());

        final TabWidthAndBaseDirProvider configurations = mock(TabWidthAndBaseDirProvider.class);
        when(configurations.tabWidth()).thenReturn(2);
        final String baseDir = new File(Objects.requireNonNull(getClass().getResource(CONFIG_FILE)).toURI()).getParent();
        when(configurations.baseDir()).thenReturn(Optional.of(baseDir));

        return csService.getCheckstyleInstance().createChecker(pModule, configLoc,
                Collections.emptyMap(), configurations, getClass().getClassLoader());
    }


    private void runChecker(@NotNull final CheckStyleChecker checker) throws URISyntaxException {

        final File sourceFile = new File(Objects.requireNonNull(getClass().getResource("SourceFile.java")).toURI());

        final ScannableFile file1 = mock(ScannableFile.class);
        when(file1.getFile()).thenReturn(sourceFile);
        final List<ScannableFile> filesToScan = Collections.singletonList(file1);

        final CheckstyleActions csInstance = csService.getCheckstyleInstance();
        csInstance.scan(checker.getCheckerWithConfig4UnitTest(), filesToScan, false, 2, //
                Optional.of(sourceFile.getParent()));
    }


    private String readFile(@NotNull final String filename) throws IOException, URISyntaxException {
        URL url = getClass().getResource(filename);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(filename);
        }
        assertNotNull(url);
        return Files.readString(Paths.get(url.toURI()));
    }

    @NotNull
    private static String readBaseVersion() {
        String result;
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
