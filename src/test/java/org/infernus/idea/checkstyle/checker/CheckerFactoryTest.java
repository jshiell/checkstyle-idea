package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
}
