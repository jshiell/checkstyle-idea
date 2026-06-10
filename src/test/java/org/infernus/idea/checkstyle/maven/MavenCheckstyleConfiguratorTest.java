package org.infernus.idea.checkstyle.maven;

import com.intellij.maven.testFramework.MavenMultiVersionImportingTestCase;
import java.nio.file.Files;
import java.util.List;
import java.util.TreeSet;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class MavenCheckstyleConfiguratorTest extends MavenMultiVersionImportingTestCase {

    @Parameterized.Parameters(name = "with Maven-{0}")
    public static List<String[]> getMavenVersions() {
        return List.<String[]>of(new String[]{"bundled"});
    }

    private static final String PROJECT_INFO = """
        <groupId>test</groupId>
        <artifactId>test</artifactId>
        <version>1</version>
        <name>test</name>
        """.stripIndent();

    @Test
    public void afterImport_importSettingsFromMavenIsEnabled_updatesConfigLocations()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true).withLocations(new TreeSet<>())
            .withActiveLocationIds(new TreeSet<>());
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        final var configPath = Files.writeString(
            getProjectRoot().toNioPath().resolve("checkstyle.xml"), "<config></config>");

        createProjectPom(PROJECT_INFO + """
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
                        <configuration>
                            <configLocation>checkstyle.xml</configLocation>
                        </configuration>
                        <dependencies>
                            <dependency>
                                <groupId>com.puppycrawl.tools</groupId>
                                <artifactId>checkstyle</artifactId>
                                <version>10.26.1</version>
                            </dependency>
                        </dependencies>
                    </plugin>
                </plugins>
            </build>
            """.stripIndent());

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation));

        final var configurationLocationFactory = getProject().getService(
            ConfigurationLocationFactory.class);
        assertOrderedEquals(
            List.of(configurationLocationFactory.create(BundledConfig.SUN_CHECKS, getProject()),
                configurationLocationFactory.create(BundledConfig.GOOGLE_CHECKS, getProject()),
                configurationLocationFactory.create(getProject(), "maven-config-location",
                    ConfigurationType.PROJECT_RELATIVE, configPath.toString(),
                    "Maven Config Location", NamedScopeHelper.getDefaultScope(getProject()))),
            pluginConfigurationManager.getCurrent().getLocations());
        assertOrderedEquals(List.of("maven-config-location"),
            pluginConfigurationManager.getCurrent().getActiveLocationIds());
    }

    @Test
    public void afterImport_importSettingsFromMavenIsEnabledAndInheritingMavenPluginCheckstyleVersion_updatesVersionWithInheritedValue()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withCheckstyleVersion("10.26.1");
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        createProjectPom(PROJECT_INFO + """
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
                    </plugin>
                </plugins>
            </build>
            """.stripIndent());

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation));

        assertEquals("9.3", pluginConfigurationManager.getCurrent().getCheckstyleVersion());
    }

    @Test
    public void afterImport_configLocationContainsCustomMavenProperty_resolvesProperty() throws Exception {
        final var pluginConfigurationManager = getProject().getService(PluginConfigurationManager.class);

        final var configPath = Files.writeString(
            getProjectRoot().toNioPath().resolve("checkstyle.xml"), "<config></config>");

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withLocations(new TreeSet<>()).withActiveLocationIds(new TreeSet<>());
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        createProjectPom(PROJECT_INFO + """
            <properties>
                <checkstyle.config.file>checkstyle.xml</checkstyle.config.file>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
                        <configuration>
                            <configLocation>${checkstyle.config.file}</configLocation>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """.stripIndent());

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation));

        assertEquals(configPath.toString(),
            pluginConfigurationManager.getCurrent().getLocations().stream()
                .filter(loc -> "maven-config-location".equals(loc.getId()))
                .map(org.infernus.idea.checkstyle.model.ConfigurationLocation::getLocation).findFirst().orElseThrow());
    }

    @Test
    public void afterImport_suppressionsLocationContainsCustomMavenProperty_resolvesProperty() throws Exception {
        final var pluginConfigurationManager = getProject().getService(PluginConfigurationManager.class);

        Files.writeString(getProjectRoot().toNioPath().resolve("checkstyle.xml"), "<config></config>");

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withLocations(new TreeSet<>()).withActiveLocationIds(new TreeSet<>());
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        createProjectPom(PROJECT_INFO + """
            <properties>
                <checkstyle.suppressions.path>suppressions.xml</checkstyle.suppressions.path>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
                        <configuration>
                            <configLocation>checkstyle.xml</configLocation>
                            <suppressionsLocation>${checkstyle.suppressions.path}</suppressionsLocation>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """.stripIndent());

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation));

        final var suppressionsValue = pluginConfigurationManager.getCurrent().getLocations().stream()
            .filter(loc -> "maven-config-location".equals(loc.getId()))
            .findFirst().orElseThrow().getProperties().get("checkstyle.suppressions.file");
        assertEquals("suppressions.xml", suppressionsValue);
    }
}
