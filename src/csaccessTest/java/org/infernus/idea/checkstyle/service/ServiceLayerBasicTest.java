package org.infernus.idea.checkstyle.service;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.picocontainer.PicoContainer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.infernus.idea.checkstyle.service.CsVersionInfo.*;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ServiceLayerBasicTest {
    private static final Project PROJECT = mock(Project.class);

    private static final String CONFIG_FILE_BREAKS_AFTER_6_16_1 = "config1-6.16.1-but-not-6.17.xml";
    private static final String CONFIG_FILE_BREAKS_BEFORE_6_19 = "config2-6.19-but-not-6.17.xml";

    private static CheckstyleProjectService checkstyleProjectService;

    @BeforeClass
    public static void setUp() {
        PicoContainer picoContainer = mock(PicoContainer.class);
        when(PROJECT.getPicoContainer()).thenReturn(picoContainer);

        PluginConfigurationManager mockPluginConfig = mock(PluginConfigurationManager.class);
        final PluginConfiguration mockConfigDto = PluginConfigurationBuilder.testInstance(currentCsVersion()).build();
        when(mockPluginConfig.getCurrent()).thenReturn(mockConfigDto);
        when(picoContainer.getComponentInstance(PluginConfigurationManager.class.getName())).thenReturn(mockPluginConfig);

        checkstyleProjectService = new CheckstyleProjectService(PROJECT);
    }

    @AfterClass
    public static void tearDown() {
        checkstyleProjectService = null;
    }

    @Test
    public void aTestPropertyRemovedIn6_16_1CannotBeUsedWithLaterRuntimes() throws IOException, URISyntaxException {
        try {
            createChecker(CONFIG_FILE_BREAKS_AFTER_6_16_1);

            if (csVersionIsGreaterThan("6.16.1")) {
                fail("expected exception was not thrown");
            }
        } catch (CheckstyleToolException e) {
            if (csVersionIsGreaterThan("6.16.1")) {
                assertThat(e.getMessage(), containsString("basenameSeparator"));
            } else {
                throw e;  // test failed
            }
        }
    }

    @Test
    public void aCheckIntroducedIn6_19CannotBeLoadedWithEarlierRuntimes() throws IOException, URISyntaxException {
        assumeThat(currentCsVersion(), isLessThan("8.2"));

        try {
            createChecker(CONFIG_FILE_BREAKS_BEFORE_6_19);

            if (csVersionIsLessThan("6.19")) {
                fail("expected exception was not thrown");
            }
        } catch (CheckstyleToolException e) {
            if (csVersionIsLessThan("6.19")) {
                assertThat(e.getMessage(), containsString("SingleSpaceSeparator"));
            } else {
                throw e;  // test failed
            }
        }
    }

    @Test
    public void theFileContentsHolderCannotBeUsedWithCheckstyle8_2AndAbove() throws IOException, URISyntaxException {
        assumeThat(currentCsVersion(), isGreaterThanOrEqualTo("8.2"));

        try {
            createChecker(CONFIG_FILE_BREAKS_BEFORE_6_19);
            fail("expected exception was not thrown");
        } catch (CheckstyleToolException e) {
            assertThat(e.getMessage(), containsString("FileContentsHolder"));
        }
    }

    private CheckStyleChecker createChecker(@NotNull final String configXmlFile)
            throws IOException, URISyntaxException {
        final ConfigurationLocation configLoc = new StringConfigurationLocation(FileUtil.readFile(configXmlFile), mock(Project.class));

        final Module module = mock(Module.class);
        when(module.getProject()).thenReturn(PROJECT);

        final TabWidthAndBaseDirProvider configurations = mock(TabWidthAndBaseDirProvider.class);
        when(configurations.tabWidth()).thenReturn(2);
        when(configurations.baseDir()).thenReturn(
                Optional.of(new File(getClass().getResource(configXmlFile).toURI()).getParent()));

        final CheckstyleActions csInstance = checkstyleProjectService.getCheckstyleInstance();
        return csInstance.createChecker(module, configLoc, Collections.emptyMap(), configurations, getClass()
                .getClassLoader());
    }

}
