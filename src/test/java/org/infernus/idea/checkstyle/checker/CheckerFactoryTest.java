package org.infernus.idea.checkstyle.checker;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.exception.ActionableCheckstyleException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class CheckerFactoryTest {

    private Project project;
    private CheckerFactoryCache cache;
    private ConfigurationLocation location;

    @Mock
    private CheckstyleProjectService checkstyleProjectService;

    private Application previousApplication;

    @BeforeEach
    void setUp() {
        previousApplication = ApplicationManager.getApplication();
        project = TestHelper.mockProject();
        cache = new CheckerFactoryCache();
        location = new StringConfigurationLocation("<module name=\"Checker\"/>", project);
    }

    @AfterEach
    void restoreApplication() {
        if (previousApplication != null) {
            ApplicationManager.setApplication(previousApplication, mock(Disposable.class));
        }
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
    void aFailureTheUserCanActOnIsShownToThemRatherThanLeftInTheEventLog(@TempDir final Path moduleDir) {
        NotificationGroup balloonGroup = interceptNotifications();
        Module module = mockModule("a-module");
        checkstyleInstanceFailingWith(new ActionableCheckstyleException("set the VM option", new IOException()));

        checkerFactoryWith(projectPathsReturning(module, moduleDir)).checker(module, location);

        verify(balloonGroup).createNotification(eq(""), contains("set the VM option"), eq(NotificationType.ERROR));
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

    private NotificationGroup interceptNotifications() {
        final NotificationGroup balloonGroup = mock(NotificationGroup.class,
                withSettings().mockMaker(MockMakers.INLINE));
        when(balloonGroup.createNotification(anyString(), anyString(), any(NotificationType.class)))
                .thenReturn(mock(Notification.class));

        final NotificationGroupManager notificationGroupManager = mock(NotificationGroupManager.class);
        when(notificationGroupManager.getNotificationGroup("CheckStyleIDEABalloonGroup")).thenReturn(balloonGroup);

        final Application application = mock(Application.class);
        when(application.getService(NotificationGroupManager.class)).thenReturn(notificationGroupManager);
        ApplicationManager.setApplication(application, mock(Disposable.class));

        return balloonGroup;
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
