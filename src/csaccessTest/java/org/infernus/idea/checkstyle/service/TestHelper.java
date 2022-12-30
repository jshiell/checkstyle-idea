package org.infernus.idea.checkstyle.service;

import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.jetbrains.annotations.NotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestHelper {

    private TestHelper() {
    }

    @NotNull
    public static Project mockProject() {
        final Project project = mock(Project.class);
        final CheckerFactoryCache checkerFactoryCache = new CheckerFactoryCache();
        when(project.getService(CheckerFactoryCache.class)).thenReturn(checkerFactoryCache);
        when(project.getService(NamedScopeManager.class)).thenReturn(new NamedScopeManager(project));
        when(project.getService(DependencyValidationManager.class)).thenReturn(mock(DependencyValidationManager.class));
        return project;
    }
}
