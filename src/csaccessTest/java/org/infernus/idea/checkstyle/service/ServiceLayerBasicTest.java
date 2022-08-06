package org.infernus.idea.checkstyle.service;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Objects;
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

    private static final String CONFIG_FILE_CONTENTS_HOLDER = "config-file-contents-holder.xml";

    private static CheckstyleProjectService checkstyleProjectService;

    @BeforeClass
    public static void setUp() {
        PluginConfigurationManager mockPluginConfig = mock(PluginConfigurationManager.class);
        final PluginConfiguration mockConfigDto = PluginConfigurationBuilder.testInstance(currentCsVersion()).build();
        when(mockPluginConfig.getCurrent()).thenReturn(mockConfigDto);
        when(PROJECT.getService(PluginConfigurationManager.class)).thenReturn(mockPluginConfig);

        checkstyleProjectService = new CheckstyleProjectService(PROJECT);
    }

    @AfterClass
    public static void tearDown() {
        checkstyleProjectService = null;
    }

    @Test
    public void theFileContentsHolderCannotBeUsedWithCheckstyle82AndAbove() throws IOException, URISyntaxException {
        assumeThat(currentCsVersion(), isGreaterThanOrEqualTo("8.2"));

        try {
            createChecker(CONFIG_FILE_CONTENTS_HOLDER);
            fail("expected exception was not thrown");
        } catch (CheckstyleToolException e) {
            assertThat(e.getMessage(), containsString("FileContentsHolder"));
        }
    }

    private void createChecker(@NotNull final String configXmlFile)
            throws IOException, URISyntaxException {
        final ConfigurationLocation configLoc = new StringConfigurationLocation(FileUtil.readFile(configXmlFile), TestHelper.mockProject());

        final Module module = mock(Module.class);
        when(module.getProject()).thenReturn(PROJECT);

        final TabWidthAndBaseDirProvider configurations = mock(TabWidthAndBaseDirProvider.class);
        when(configurations.tabWidth()).thenReturn(2);
        when(configurations.baseDir()).thenReturn(
                Optional.of(new File(Objects.requireNonNull(getClass().getResource(configXmlFile)).toURI()).getParent()));

        final CheckstyleActions csInstance = checkstyleProjectService.getCheckstyleInstance();
        csInstance.createChecker(module, configLoc, Collections.emptyMap(), configurations, getClass()
                .getClassLoader());
    }

}
