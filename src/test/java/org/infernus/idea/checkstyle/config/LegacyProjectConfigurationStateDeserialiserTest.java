package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LegacyProjectConfigurationStateDeserialiserTest {

    private final Project project = TestHelper.mockProject();
    private final ProjectPaths projectPaths = mock(ProjectPaths.class);
    private final ConfigurationLocationFactory configurationLocationFactory = new ConfigurationLocationFactory();
    private final ProjectFilePaths projectFilePaths = new ProjectFilePaths(project, File.separatorChar, File::getAbsolutePath, projectPaths);

    @Before
    public void configureMocks() {
        final VirtualFile projectPath = mock(VirtualFile.class);
        when(projectPath.getPath()).thenReturn("/a/project/path");
        when(projectPaths.projectPath(project)).thenReturn(projectPath);

        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(configurationLocationFactory);
        when(project.getService(ProjectFilePaths.class)).thenReturn(projectFilePaths);
    }

    @Test
    public void basicPropertiesCanBeDeserialised() {
        final ProjectConfigurationState.ProjectSettings configuration = testConfiguration();

        PluginConfiguration pluginConfiguration = new LegacyProjectConfigurationStateDeserialiser(project)
                .deserialise(PluginConfigurationBuilder.testInstance("10.1"), configuration)
                .build();

        assertThat(pluginConfiguration.isCopyLibs(), equalTo(false));
        assertThat(pluginConfiguration.getCheckstyleVersion(), equalTo("10.2"));
        assertThat(pluginConfiguration.isScanBeforeCheckin(), equalTo(false));
        assertThat(pluginConfiguration.getScanScope(), equalTo(ScanScope.JavaOnlyWithTests));
        assertThat(pluginConfiguration.isSuppressErrors(), equalTo(false));
        assertThat(pluginConfiguration.getThirdPartyClasspath(), equalTo(List.of(
                "/a/project/path/test-configs/spring-javaformat-checkstyle-0.0.31.jar",
                "/a/project/path/test-configs/spring-javaformat-config-0.0.31.jar"
        )));
    }

    @Test
    public void configurationLocationsCanBeDeserialised() {
        final ProjectConfigurationState.ProjectSettings configuration = testConfiguration();

        PluginConfiguration pluginConfiguration = new LegacyProjectConfigurationStateDeserialiser(project)
                .deserialise(PluginConfigurationBuilder.testInstance("10.1"), configuration)
                .build();

        ArrayList<ConfigurationLocation> configList = new ArrayList<>(pluginConfiguration.getLocations());
        assertThat(configList, equalTo(List.of(
                Descriptor.parse("BUNDLED:(bundled):Sun Checks;All", project).toConfigurationLocation(project),
                Descriptor.parse("BUNDLED:(bundled):Google Checks;All", project).toConfigurationLocation(project),
                Descriptor.parse("LOCAL_FILE:$PROJECT_DIR$/test-configs/issue-545.xml:545;All", project).toConfigurationLocation(project),
                Descriptor.parse("LOCAL_FILE:$PROJECT_DIR$/test-configs/working-checkstyle-rules-8.24.xml:Working;All", project).toConfigurationLocation(project),
                Descriptor.parse("HTTP_URL:http://demo:demo@localhost:8000/working-checkstyle-rules-8.24.xml:Working HTTP;All", project).toConfigurationLocation(project))
        ));
        assertThat(pluginConfiguration.getActiveLocations(), hasSize(1));
        assertThat(pluginConfiguration.getActiveLocations().first(), equalTo(configList.get(3)));
        assertThat(configList.get(1).getProperties(), equalTo(Map.of(
                "org.checkstyle.google.suppressionfilter.config", "notxpath",
                "org.checkstyle.google.suppressionxpathfilter.config", "xpath"
        )));
        assertThat(configList.get(2).getProperties(), equalTo(Map.of(
                "checkstyle.classdataabstractioncoupling.excludeclassesregexps", ""
        )));
    }

    @NotNull
    private ProjectConfigurationState.ProjectSettings testConfiguration() {
        final Map<String, String> configuration = new HashMap<>();
        configuration.put("active-configuration-0", "LOCAL_FILE:$PROJECT_DIR$/test-configs/working-checkstyle-rules-8.24.xml:Working;All");
        configuration.put("checkstyle-version", "10.2");
        configuration.put("copy-libs", "false");
        configuration.put("location-0", "BUNDLED:(bundled):Sun Checks;All");
        configuration.put("location-1", "BUNDLED:(bundled):Google Checks;All");
        configuration.put("location-2", "LOCAL_FILE:$PROJECT_DIR$/test-configs/issue-545.xml:545;All");
        configuration.put("location-3", "LOCAL_FILE:$PROJECT_DIR$/test-configs/working-checkstyle-rules-8.24.xml:Working;All");
        configuration.put("location-7", "HTTP_URL:http://demo:demo@localhost:8000/working-checkstyle-rules-8.24.xml:Working HTTP;All");
        configuration.put("property-1.org.checkstyle.google.suppressionfilter.config", "notxpath");
        configuration.put("property-1.org.checkstyle.google.suppressionxpathfilter.config", "xpath");
        configuration.put("property-2.checkstyle.classdataabstractioncoupling.excludeclassesregexps", "");
        configuration.put("scan-before-checkin", "false");
        configuration.put("scanscope", "JavaOnlyWithTests");
        configuration.put("suppress-errors", "false");
        configuration.put("thirdparty-classpath", "$PROJECT_DIR$/test-configs/spring-javaformat-checkstyle-0.0.31.jar;$PROJECT_DIR$/test-configs/spring-javaformat-config-0.0.31.jar");

        return new ProjectConfigurationState.ProjectSettings(configuration);
    }

}
