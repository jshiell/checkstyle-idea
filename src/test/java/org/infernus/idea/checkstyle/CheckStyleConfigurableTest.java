package org.infernus.idea.checkstyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class CheckStyleConfigurableTest
{
    private static class CheckStyleConfigurable4Test
        extends CheckStyleConfigurable
    {
        private final Project mockProject;



        CheckStyleConfigurable4Test(@NotNull final Project project, @NotNull final CheckStyleConfigPanel configPanel)
        {
            super(project, configPanel);
            mockProject = project;
        }



        CheckStyleConfiguration getConfiguration()
        {
            final ConfigurationLocationFactory mockLocFactory = new ConfigurationLocationFactory();
            final List<ConfigurationLocation> mockLocations = buildMockLocations(mockProject, mockLocFactory);

            CheckStyleConfiguration mockConfig = Mockito.mock(CheckStyleConfiguration.class);
            Mockito.when(mockConfig.getProject()).thenReturn(mockProject);
            Mockito.when(mockConfig.configurationLocationFactory()).thenReturn(mockLocFactory);
            Mockito.when(mockConfig.getActiveConfiguration()).thenReturn(mockLocations.get(0));
            Mockito.when(mockConfig.configurationLocations()).thenReturn(mockLocations);
            Mockito.when(mockConfig.getThirdPartyClassPath()).thenReturn(Arrays.asList("cp1", "cp2"));
            Mockito.when(mockConfig.getScanScope()).thenReturn(ScanScope.AllSources);
            Mockito.when(mockConfig.isSuppressingErrors()).thenReturn(Boolean.FALSE);
            return mockConfig;
        }
    }



    private static List<ConfigurationLocation> buildMockLocations(final Project mockProject,
        final ConfigurationLocationFactory mockLocFactory)
    {
        ConfigurationLocation mockLocation1 = mockLocFactory.create(mockProject, ConfigurationType.PROJECT_RELATIVE,
            "src/test/resources/emptyFile1.xml", "description1");
        ConfigurationLocation mockLocation2 = mockLocFactory.create(mockProject, ConfigurationType.PROJECT_RELATIVE,
            "src/test/resources/emptyFile2.xml", "description2");
        return Arrays.asList(mockLocation1, mockLocation2);
    }



    private CheckStyleConfigPanel buildMockPanel(final Project mockProject)
    {
        CheckStyleConfigPanel mockPanel = Mockito.mock(CheckStyleConfigPanel.class);
        final List<ConfigurationLocation> mockLocations = buildMockLocations(mockProject,
            new ConfigurationLocationFactory());

        Mockito.when(mockPanel.getActiveLocation()).thenReturn(mockLocations.get(0));
        Mockito.when(mockPanel.getConfigurationLocations()).thenReturn(mockLocations);
        Mockito.when(mockPanel.getThirdPartyClasspath()).thenReturn(Arrays.asList("cp1", "cp2"));
        Mockito.when(mockPanel.getScanScope()).thenReturn(ScanScope.AllSources);
        Mockito.when(mockPanel.isSuppressingErrors()).thenReturn(Boolean.FALSE);

        return mockPanel;
    }



    @Test
    public void testIsModified()
    {
        Project mockProject = Mockito.mock(Project.class);
        CheckStyleConfigPanel mockPanel = buildMockPanel(mockProject);

        CheckStyleConfigurable classUnderTest = new CheckStyleConfigurable4Test(mockProject, mockPanel);

        boolean actualModified = classUnderTest.isModified();

        Assert.assertFalse(actualModified);
    }
}
