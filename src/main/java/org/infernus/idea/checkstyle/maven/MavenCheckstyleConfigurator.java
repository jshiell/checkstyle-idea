package org.infernus.idea.checkstyle.maven;

import com.intellij.openapi.project.Project;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.stream.StreamSupport;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.importing.MavenAfterImportConfigurator;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;

@SuppressWarnings("UnstableApiUsage")
public class MavenCheckstyleConfigurator implements MavenAfterImportConfigurator {

  private static final MavenId CHECKSTYLE_MAVEN_ID =
      new MavenId("com.puppycrawl.tools", "checkstyle", null);
  private static final MavenId MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID =
      new MavenId("org.apache.maven.plugins", "maven-checkstyle-plugin", null);
  private static final String MAVEN_CONFIG_LOCATION_ID = "maven-config-location";

  @Override
  public void afterImport(@NotNull final MavenAfterImportConfigurator.Context context) {
    final var project = context.getProject();

    // TODO: Current CheckStyle IntelliJ plugin supports specifying the active rule for a given
    //  Module. The code here can probably support that as well, if two modules use different
    //  configurations.

    // TODO: The first MavenProject found with a Maven Checkstyle Plugin will modify the settings
    //  for the Project. Might be useful to have a more deterministic way for this to work.
    final var mavenProjectWithModules =
        StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                    context.getMavenProjectsWithModules().iterator(), Spliterator.ORDERED),
                false)
            .filter(
                mavenProjectWithModulesToFilter -> {
                  final var mavenProject = mavenProjectWithModulesToFilter.getMavenProject();
                  final var checkstyleMavenPlugin =
                      mavenProject.findPlugin(
                          MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID.getGroupId(),
                          MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID.getArtifactId());

                  return checkstyleMavenPlugin != null;
                })
            .findFirst()
            .orElse(null);

    if (mavenProjectWithModules == null) {
      return;
    }

    final var mavenProject = mavenProjectWithModules.getMavenProject();
    final var checkstyleMavenPlugin =
        mavenProject.findPlugin(
            MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID.getGroupId(),
            MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID.getArtifactId());

    if (checkstyleMavenPlugin == null) {
      return;
    }

    final var checkstyleDependencyMavenId =
        checkstyleMavenPlugin.getDependencies().stream()
            .filter(
                dependency ->
                    CHECKSTYLE_MAVEN_ID.equals(dependency.getGroupId(), dependency.getArtifactId()))
            .findFirst()
            .orElse(null);

    final var pluginConfigurationManager = project.getService(PluginConfigurationManager.class);
    final var currentPluginConfiguration = pluginConfigurationManager.getCurrent();
    final var pluginConfigurationBuilder =
        PluginConfigurationBuilder.from(currentPluginConfiguration);
    // TODO: This will be null if checkstyle isn't declared explicitly.
    //  Meaning the transitive dependency version won't be found.
    //  Would be great to resolve that transitive version somehow.
    if (checkstyleDependencyMavenId != null && checkstyleDependencyMavenId.getVersion() != null) {
      // Checkstyle Version
      pluginConfigurationBuilder.withCheckstyleVersion(checkstyleDependencyMavenId.getVersion());
    }

    // Checkstyle Third Party Rules
    // TODO: This doesn't differentiate between an additional dependency that may or may
    //  not be providing rules. Not sure if that matters or how that would be worked around.
    final var thirdPartyClassPaths =
        checkstyleMavenPlugin.getDependencies().stream()
            .filter(
                dependency -> {
                  if (!CHECKSTYLE_MAVEN_ID.equals(
                      dependency.getGroupId(), dependency.getArtifactId())) {
                    return false;
                  }

                  if (dependency.getArtifactId() == null
                      || dependency.getGroupId() == null
                      || dependency.getVersion() == null) {
                    return false;
                  }

                  return true;
                })
            .map(
                dependency -> {
                  final var dependencyRelativePath =
                      Path.of(
                          dependency.getGroupId().replace(".", File.separator),
                          dependency.getArtifactId(),
                          dependency.getVersion(),
                          // TODO: This doesn't support classifiers.
                          dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar");
                  final var dependencyPath =
                      mavenProject.getLocalRepository().toPath().resolve(dependencyRelativePath);

                  return dependencyPath.toAbsolutePath().toString();
                })
            .toList();
    pluginConfigurationBuilder.withThirdPartyClassPath(thirdPartyClassPaths);

    final var checkstyleMavenPluginConfiguration = checkstyleMavenPlugin.getConfigurationElement();
    if (checkstyleMavenPluginConfiguration != null) {
      // Checkstyle Config File
      final var configLocationElement =
          checkstyleMavenPluginConfiguration.getChild("configLocation");
      if (configLocationElement != null && configLocationElement.getText() != null) {
        final String mavenPluginConfigLocation = configLocationElement.getText();
        // This must come after the PluginConfigurationBuilder is modified with the new
        // CheckStyle version and the new third party classpaths.
        final var tempConfiguration = pluginConfigurationBuilder.build();
        final var checkstyleProjectService =
            CheckstyleProjectService.forVersion(
                project,
                tempConfiguration.getCheckstyleVersion(),
                tempConfiguration.getThirdPartyClasspath());
        final var configurationLocation =
            createConfigurationLocation(
                project, mavenProject, checkstyleProjectService, mavenPluginConfigLocation);

        final var configLocations = new TreeSet<>(currentPluginConfiguration.getLocations());
        configLocations.removeIf(location -> MAVEN_CONFIG_LOCATION_ID.equals(location.getId()));
        configLocations.add(configurationLocation);
        pluginConfigurationBuilder.withLocations(configLocations);

        final var activeConfigLocationIds =
            new TreeSet<>(currentPluginConfiguration.getActiveLocationIds());
        activeConfigLocationIds.removeIf(MAVEN_CONFIG_LOCATION_ID::equals);
        activeConfigLocationIds.add(configurationLocation.getId());
        pluginConfigurationBuilder.withActiveLocationIds(activeConfigLocationIds);
      }
    }

    // TODO:
    //  Checkstyle Scan Scope

    final var newPluginConfiguration = pluginConfigurationBuilder.build();
    if (!currentPluginConfiguration.equals(newPluginConfiguration)) {
      pluginConfigurationManager.setCurrent(pluginConfigurationBuilder.build(), true);
    }
  }

  private ConfigurationLocation createConfigurationLocation(
      final Project project,
      final MavenProject mavenProject,
      final CheckstyleProjectService checkstyleProjectService,
      final String mavenPluginConfigLocation) {
    final var configurationLocationFactory = project.getService(ConfigurationLocationFactory.class);

    ConfigurationType configurationType = null;
    String configLocation = mavenPluginConfigLocation;

    try {
      final var mavenPluginConfigLocationPath = Path.of(mavenPluginConfigLocation);
      if (mavenPluginConfigLocationPath.isAbsolute()
          && Files.isReadable(mavenPluginConfigLocationPath)) {
        configurationType = ConfigurationType.LOCAL_FILE;
      } else if (Files.isReadable(
          Path.of(mavenProject.getDirectory()).resolve(mavenPluginConfigLocationPath))) {
        configurationType = ConfigurationType.PROJECT_RELATIVE;
        // TODO: Seems a bit odd that this needs to be set to an absolute path, instead of a
        //  relative path.
        configLocation =
            Path.of(mavenProject.getDirectory()).resolve(mavenPluginConfigLocationPath).toString();
      }
    } catch (final InvalidPathException ignored) {
    }

    if (configurationType == null) {
      try {
        // This can also be a file:// URI. This still resolves fine with the HTTP_URL
        // implementation, so just leaving it alone. This is probably a bit confusing
        // and has potential to break in the future.
        new URI(mavenPluginConfigLocation);
        configurationType = ConfigurationType.HTTP_URL;
      } catch (final URISyntaxException ignored) {
      }
    }

    final var classLoader = checkstyleProjectService.underlyingClassLoader();
    final var resource = classLoader.getResource(mavenPluginConfigLocation);
    if (resource != null) {
      configurationType = ConfigurationType.PLUGIN_CLASSPATH;
    }

    if (configurationType == null) {
      throw new RuntimeException(
          "Unable to identify ConfigurationType for configured location: "
              + mavenPluginConfigLocation);
    }

    return configurationLocationFactory.create(
        project,
        MAVEN_CONFIG_LOCATION_ID,
        configurationType,
        configLocation,
        "Maven Config Location",
        // TODO: What should this be?
        NamedScopeHelper.getDefaultScope(project));
  }
}
