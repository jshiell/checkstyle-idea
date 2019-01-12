package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CheckStyleConfigurableTest {

    private PluginConfigurationManager mockPluginConfigurationManager(@NotNull final Project project) {
        final ConfigurationLocationFactory mockLocFactory = new ConfigurationLocationFactory();
        final SortedSet<ConfigurationLocation> mockLocations = buildMockLocations(project, mockLocFactory);

        final PluginConfiguration mockConfigDto = PluginConfigurationBuilder.testInstance("7.1.2")
                .withLocations(mockLocations)
                .withThirdPartyClassPath(Arrays.asList("cp1", "cp2"))
                .withActiveLocation(mockLocations.first())
                .build();
        PluginConfigurationManager mockConfig = mock(PluginConfigurationManager.class);
        when(mockConfig.getCurrent()).thenReturn(mockConfigDto);
        return mockConfig;
    }

    private static SortedSet<ConfigurationLocation> buildMockLocations(final Project mockProject,
                                                                       final ConfigurationLocationFactory mockLocFactory) {
        ConfigurationLocation mockLocation1 = mockLocFactory.create(mockProject, ConfigurationType.PROJECT_RELATIVE,
                "src/test/resources/emptyFile1.xml", "description1");
        ConfigurationLocation mockLocation2 = mockLocFactory.create(mockProject, ConfigurationType.PROJECT_RELATIVE,
                "src/test/resources/emptyFile2.xml", "description2");
        SortedSet<ConfigurationLocation> result = new TreeSet<>();
        result.add(mockLocation1);
        result.add(mockLocation2);
        return result;
    }

    private CheckStyleConfigPanel buildMockPanel(final Project mockProject) {
        CheckStyleConfigPanel mockPanel = mock(CheckStyleConfigPanel.class);
        final SortedSet<ConfigurationLocation> mockLocations = buildMockLocations(mockProject,
                new ConfigurationLocationFactory());

        final PluginConfiguration mockConfigDto = PluginConfigurationBuilder.testInstance("7.1.2")
                .withLocations(mockLocations)
                .withThirdPartyClassPath(Arrays.asList("cp1", "cp2"))
                .withActiveLocation(mockLocations.first())
                .build();
        when(mockPanel.getPluginConfiguration()).thenReturn(mockConfigDto);
        return mockPanel;
    }


    @Test
    public void testIsModified() {
        Project mockProject = mock(Project.class);
        CheckStyleConfigPanel mockPanel = buildMockPanel(mockProject);

        CheckStyleConfigurable classUnderTest = new CheckStyleConfigurable(
                mockProject, mockPanel, mock(CheckstyleProjectService.class),
                mockPluginConfigurationManager(mockProject),
                mock(CheckerFactoryCache.class));

        assertFalse(classUnderTest.isModified());
    }
}
