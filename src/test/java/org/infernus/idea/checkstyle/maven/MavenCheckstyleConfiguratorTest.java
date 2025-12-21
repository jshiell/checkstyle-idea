package org.infernus.idea.checkstyle.maven;

import com.intellij.maven.testFramework.MavenMultiVersionImportingTestCase;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.TreeSet;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.junit.Test;

public class MavenCheckstyleConfiguratorTest extends MavenMultiVersionImportingTestCase {

    private static final String PROJECT_INFO = """
        <groupId>test</groupId>
        <artifactId>test</artifactId>
        <version>1</version>
        <name>test</name>
        """.stripIndent();

    @Test
    public void afterImport_importSettingsFromMavenIsDisabled_doesNothing() throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var beforeConfig = PluginConfigurationBuilder.from(
                pluginConfigurationManager.getCurrent()).withImportSettingsFromMaven(false)
            .withCheckstyleVersion("10.26.0").build();
        pluginConfigurationManager.setCurrent(beforeConfig, true);

        createProjectPom(PROJECT_INFO + """
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
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

        assertEquals(beforeConfig, pluginConfigurationManager.getCurrent());
    }

    @Test
    public void afterImport_importSettingsFromMavenIsEnabled_updatesVersion() throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withCheckstyleVersion("10.26.0");
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        createProjectPom(PROJECT_INFO + """
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
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

        assertEquals("10.26.1", pluginConfigurationManager.getCurrent().getCheckstyleVersion());
    }

    @Test
    public void afterImport_importSettingsFromMavenIsEnabled_updatesThirdPartyClassPath()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withThirdPartyClassPath(List.of("/com/stuff/something.jar"));
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        createProjectPom(PROJECT_INFO + """
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
                        <dependencies>
                            <dependency>
                                <groupId>com.puppycrawl.tools</groupId>
                                <artifactId>checkstyle</artifactId>
                                <version>10.26.1</version>
                            </dependency>
                            <dependency>
                                <groupId>com.checkstyle.third.party.rules</groupId>
                                <artifactId>cool-stuff</artifactId>
                                <version>3.2.1</version>
                            </dependency>
                        </dependencies>
                    </plugin>
                </plugins>
            </build>
            """.stripIndent());

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation));

        assertOrderedEquals(List.of(MavenUtil.resolveDefaultLocalRepository()
                                    + "/com/checkstyle/third/party/rules/cool-stuff/3.2.1/cool-stuff-3.2.1.jar".replace(
                "/", File.separator)),
            pluginConfigurationManager.getCurrent().getThirdPartyClasspath());
    }

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
    public void afterImport_importSettingsFromMavenIsEnabled_updatesScanScope() throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withScanScope(ScanScope.Everything);
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        createProjectPom(PROJECT_INFO + """
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
                        <configuration>
                            <includeResources>true</includeResources>
                            <includeTestResources>true</includeTestResources>
                            <includeTestSourceDirectory>true</includeTestSourceDirectory>
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

        assertEquals(ScanScope.AllSourcesWithTests,
            pluginConfigurationManager.getCurrent().getScanScope());
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
    public void updatePluginConfigLocationsFromMavenPlugin_configLocationIsMissingAndMavenConfigExists_removesMavenConfigLocation()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);
        final var configurationLocationFactory = getProject().getService(
            ConfigurationLocationFactory.class);

        final var mavenConfigLocation = configurationLocationFactory.create(getProject(),
            "maven-config-location", ConfigurationType.PROJECT_RELATIVE, "checkstyle.xml",
            "Maven Config Location", NamedScopeHelper.getDefaultScope(getProject()));

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withLocations(new TreeSet<>(List.of(mavenConfigLocation)))
            .withActiveLocationIds(new TreeSet<>(List.of(mavenConfigLocation.getId())));
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

        assertTrue(pluginConfigurationManager.getCurrent().getLocations().stream()
            .noneMatch(config -> config.getId().equals(mavenConfigLocation.getId())));
    }

    @Test
    public void updatePluginConfigLocationsFromMavenPlugin_configLocationExistsAndMavenConfigAlreadyExists_overwritesWithNewConfig()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);
        final var configurationLocationFactory = getProject().getService(
            ConfigurationLocationFactory.class);

        final var configPath = Files.writeString(
            getProjectRoot().toNioPath().resolve("checkstyle.xml"), "<config></config>");
        final var mavenConfigLocation = configurationLocationFactory.create(getProject(),
            "maven-config-location", ConfigurationType.PROJECT_RELATIVE, "checkstyle-existing.xml",
            "Maven Config Location", NamedScopeHelper.getDefaultScope(getProject()));

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withLocations(new TreeSet<>(List.of(mavenConfigLocation)))
            .withActiveLocationIds(new TreeSet<>(List.of(mavenConfigLocation.getId())));
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

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
                    </plugin>
                </plugins>
            </build>
            """.stripIndent());

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation));

        assertEquals(configPath.toString(),
            pluginConfigurationManager.getCurrent().getLocations().stream()
                .filter(config -> config.getId().equals(mavenConfigLocation.getId()))
                .map(ConfigurationLocation::getLocation).findFirst().get());
    }

    @Test
    public void updatePluginConfigLocationsFromMavenPlugin_mavenConfigDoesNotAlreadyExist_addsNewConfig()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var configPath = Files.writeString(
            getProjectRoot().toNioPath().resolve("checkstyle.xml"), "<config></config>");
        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true).withLocations(new TreeSet<>())
            .withActiveLocationIds(new TreeSet<>());
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

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
                    </plugin>
                </plugins>
            </build>
            """.stripIndent());

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation));

        assertEquals(configPath.toString(),
            pluginConfigurationManager.getCurrent().getLocations().stream()
                .filter(config -> config.getId().equals("maven-config-location"))
                .map(ConfigurationLocation::getLocation).findFirst().get());
    }

    @Test
    public void updatePluginScanScopeFromMavenPlugin_includeSettingsAreMissingAndScanScopeExists_usesAllSourcesWithTestsScanScope()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withScanScope(ScanScope.Everything);
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

        assertEquals(ScanScope.getDefaultValue(),
            pluginConfigurationManager.getCurrent().getScanScope());
    }

    @Test
    public void updatePluginScanScopeFromMavenPlugin_includeSettingsExistsAndScanScopeAlreadyExists_usesNewScanScope()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true)
            .withScanScope(ScanScope.Everything);
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        createProjectPom(PROJECT_INFO + """
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-checkstyle-plugin</artifactId>
                        <version>3.6.0</version>
                        <configuration>
                            <includeResources>false</includeResources>
                            <includeTestResources>false</includeTestResources>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
            """.stripIndent());

        BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation));

        assertEquals(ScanScope.JavaOnly, pluginConfigurationManager.getCurrent().getScanScope());
    }

    @Test
    public void afterImport_mavenCheckstylePluginNotConfiguredAndSyncEnabled_doesNotThrow()
        throws Exception {
        final var pluginConfigurationManager = getProject().getService(
            PluginConfigurationManager.class);

        final var updatedConfigurationBuilder = PluginConfigurationBuilder.from(
            pluginConfigurationManager.getCurrent());
        updatedConfigurationBuilder.withImportSettingsFromMaven(true);
        pluginConfigurationManager.setCurrent(updatedConfigurationBuilder.build(), true);

        createProjectPom(PROJECT_INFO);

        assertDoesNotThrow(() -> BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
            (scope, continuation) -> importProjectAsync(continuation)));
    }

    // TODO: Replace this when migrating to JUnit 5.
    private void assertDoesNotThrow(Executable executable) {
        try {
            executable.execute();
        } catch (Exception e) {
            fail("Unexpected exception thrown: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @FunctionalInterface
    interface Executable {

        void execute() throws Exception;
    }
}
