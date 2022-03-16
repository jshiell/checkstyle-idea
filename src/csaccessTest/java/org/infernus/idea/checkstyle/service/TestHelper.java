package org.infernus.idea.checkstyle.service;

import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import org.jetbrains.annotations.NotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestHelper {
    @NotNull
    public static Project mockProject() {
        final Project project = mock(Project.class);
        when(project.getService(NamedScopeManager.class)).thenReturn(new NamedScopeManager(project));
        when(project.getService(DependencyValidationManager.class)).thenReturn(mock(DependencyValidationManager.class));
        return project;
    }
}
