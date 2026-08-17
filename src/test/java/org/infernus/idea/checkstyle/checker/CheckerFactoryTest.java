package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.exception.ActionableCheckstyleException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckerFactoryTest {

    private Project project;
    private CheckerFactoryCache cache;
    private ConfigurationLocation location;

    @Mock
    private CheckstyleProjectService checkstyleProjectService;

    @BeforeEach
    void setUp() {
        project = TestHelper.mockProject();
        cache = new CheckerFactoryCache();
        location = new StringConfigurationLocation("<module name=\"Checker\"/>", project);
    }

    @Test
    void checkerReturnsCachedCheckerOnCacheHit() {
        CheckStyleChecker mockChecker = mock(CheckStyleChecker.class);
        CachedChecker cachedChecker = new CachedChecker(mockChecker);
        cache.put(location, null, cachedChecker);

        CheckerFactory factory = CheckerFactory.create(project, checkstyleProjectService, cache);
        Optional<CheckStyleChecker> result = factory.checker(null, location);

        assertTrue(result.isPresent());
        assertThat(result.get(), is(mockChecker));
    }

    @Test
    void checkerReturnsDifferentCachedCheckersPerModule() {
        Module module1 = mock(Module.class);
        when(module1.getProject()).thenReturn(project);
        when(module1.getName()).thenReturn("module1");
        Module module2 = mock(Module.class);
        when(module2.getProject()).thenReturn(project);
        when(module2.getName()).thenReturn("module2");

        CheckStyleChecker checker1 = mock(CheckStyleChecker.class);
        CheckStyleChecker checker2 = mock(CheckStyleChecker.class);
        cache.put(location, module1, new CachedChecker(checker1));
        cache.put(location, module2, new CachedChecker(checker2));

        CheckerFactory factory = CheckerFactory.create(project, checkstyleProjectService, cache);

        assertThat(factory.checker(module1, location).orElseThrow(), is(checker1));
        assertThat(factory.checker(module2, location).orElseThrow(), is(checker2));
    }

    @Test
    void cacheIsSharedAcrossFactoryInstances() {
        CheckStyleChecker mockChecker = mock(CheckStyleChecker.class);
        cache.put(location, null, new CachedChecker(mockChecker));

        // Two factories sharing the same cache should both see the cached checker
        CheckerFactory factory1 = CheckerFactory.create(project, checkstyleProjectService, cache);
        CheckerFactory factory2 = CheckerFactory.create(project, checkstyleProjectService, cache);

        assertThat(factory1.checker(null, location).orElseThrow(), is(mockChecker));
        assertThat(factory2.checker(null, location).orElseThrow(), is(mockChecker));
    }

    @Test
    void basedirIsTheModulePathReportedByTheProjectPathsService(@TempDir final Path moduleDir) {
        CheckstyleActions checkstyleActions = stubCheckstyleInstance();
        Module module = mockModule("a-module");

        checkerFactoryWith(projectPathsReturning(module, moduleDir)).checker(module, location);

        assertThat(propertiesPassedTo(checkstyleActions).get("basedir"), is(moduleDir.toFile().getAbsolutePath()));
    }

    @Test
    void aUserPropertyReferencingABuiltInIsExpandedBeforeReachingCheckstyle(@TempDir final Path moduleDir) {
        CheckstyleActions checkstyleActions = stubCheckstyleInstance();
        Module module = mockModule("a-module");
        ConfigurationLocation locationUsingBaseDir = new StringConfigurationLocation(
                "<module name=\"Checker\">"
                        + "<module name=\"SuppressionFilter\">"
                        + "<property name=\"file\" value=\"${baseDir}/suppressions.xml\"/>"
                        + "</module></module>", project);
        locationUsingBaseDir.setProperties(Map.of("baseDir", "${basedir}"));

        checkerFactoryWith(projectPathsReturning(module, moduleDir)).checker(module, locationUsingBaseDir);

        assertThat(propertiesPassedTo(checkstyleActions).get("baseDir"), is(moduleDir.toFile().getAbsolutePath()));
    }

    @Test
    void aFailureTheUserCanActOnIsReportedRatherThanLoggedAsAnException() {
        checkstyleInstanceFailingWith(new ActionableCheckstyleException("set the VM option", new IOException()));

        CheckStylePluginException thrown = assertThrows(CheckStylePluginException.class, () ->
                checkerFactoryWith(mock(ProjectPaths.class)).checker(null, location));

        // the reported message, rather than a stack trace for the event log
        assertThat(thrown.getCause().getMessage(), containsString("set the VM option"));
        assertThat(thrown.getCause().getMessage(), containsString("could not be parsed"));
    }

    private CheckerFactory checkerFactoryWith(final ProjectPaths projectPaths) {
        return CheckerFactory.create(project, checkstyleProjectService, cache, projectPaths);
    }

    private ProjectPaths projectPathsReturning(final Module module, final Path path) {
        VirtualFile moduleDir = mock(VirtualFile.class);
        when(moduleDir.getPath()).thenReturn(path.toString());

        ProjectPaths projectPaths = mock(ProjectPaths.class);
        when(projectPaths.modulePath(module)).thenReturn(moduleDir);
        return projectPaths;
    }

    private Module mockModule(final String name) {
        Module module = mock(Module.class);
        when(module.getProject()).thenReturn(project);
        when(module.getName()).thenReturn(name);
        return module;
    }

    private void checkstyleInstanceFailingWith(final RuntimeException failure) {
        when(checkstyleProjectService.underlyingClassLoader()).thenReturn(getClass().getClassLoader());

        CheckstyleActions checkstyleActions = mock(CheckstyleActions.class);
        when(checkstyleActions.createChecker(any(), any(), any())).thenThrow(failure);
        when(checkstyleProjectService.getCheckstyleInstance()).thenReturn(checkstyleActions);
    }

    private CheckstyleActions stubCheckstyleInstance() {
        when(checkstyleProjectService.underlyingClassLoader()).thenReturn(getClass().getClassLoader());

        CheckstyleActions checkstyleActions = mock(CheckstyleActions.class);
        when(checkstyleActions.createChecker(any(), any(), any())).thenReturn(mock(CheckStyleChecker.class));
        when(checkstyleProjectService.getCheckstyleInstance()).thenReturn(checkstyleActions);
        return checkstyleActions;
    }

    private Map<String, String> propertiesPassedTo(final CheckstyleActions checkstyleActions) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> properties = ArgumentCaptor.forClass(Map.class);
        verify(checkstyleActions).createChecker(any(), any(), properties.capture());
        return properties.getValue();
    }
}
