package org.infernus.idea.checkstyle;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;


public class CheckStyleConfigurableTest {
    private static class CheckStyleConfigurable4Test extends CheckStyleConfigurable {

        private final Project mockProject;

        CheckStyleConfigurable4Test(@NotNull final Project project, @NotNull final CheckStyleConfigPanel configPanel) {
            super(project, configPanel);
            mockProject = project;
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        CheckStyleConfiguration getConfiguration() {
            final ConfigurationLocationFactory mockLocFactory = new ConfigurationLocationFactory();
            final SortedSet<ConfigurationLocation> mockLocations = buildMockLocations(mockProject, mockLocFactory);

            final PluginConfigDto mockConfigDto = new PluginConfigDto("7.1.2", ScanScope.AllSources, false,
                    mockLocations, Arrays.asList("cp1", "cp2"), mockLocations.first(), false);
            CheckStyleConfiguration mockConfig = Mockito.mock(CheckStyleConfiguration.class);
            when(mockConfig.configurationLocationFactory()).thenReturn(mockLocFactory);
            when(mockConfig.getCurrentPluginConfig()).thenReturn(mockConfigDto);
            return mockConfig;
        }
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
        CheckStyleConfigPanel mockPanel = Mockito.mock(CheckStyleConfigPanel.class);
        final SortedSet<ConfigurationLocation> mockLocations = buildMockLocations(mockProject,
                new ConfigurationLocationFactory());

        final PluginConfigDto mockConfigDto = new PluginConfigDto("7.1.2", ScanScope.AllSources, false, mockLocations,
                Arrays.asList("cp1", "cp2"), mockLocations.first(), false);
        when(mockPanel.getPluginConfiguration()).thenReturn(mockConfigDto);
        return mockPanel;
    }


    @Test
    public void testIsModified() {
        Project mockProject = Mockito.mock(Project.class);
        CheckStyleConfigPanel mockPanel = buildMockPanel(mockProject);

        CheckStyleConfigurable classUnderTest = new CheckStyleConfigurable4Test(mockProject, mockPanel);

        boolean actualModified = classUnderTest.isModified();

        assertFalse(actualModified);
    }
}
