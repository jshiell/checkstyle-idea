package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.packageSet.InvalidPackageSet;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import org.jetbrains.annotations.NotNull;

import static org.infernus.idea.checkstyle.model.NamedScopeHelper.DEFAULT_SCOPE_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestHelper {
    public static final NamedScope NAMED_SCOPE = new NamedScope("test", new InvalidPackageSet("asdf"));

    @NotNull
    public static Project mockProject() {
        DependencyValidationManager dependencyValidationManager = mock(DependencyValidationManager.class);
        when(dependencyValidationManager.getScope(DEFAULT_SCOPE_ID)).thenReturn(NAMED_SCOPE);

        final Project project = mock(Project.class);
        when(project.getService(NamedScopeManager.class)).thenReturn(new NamedScopeManager(project));
        when(project.getService(DependencyValidationManager.class)).thenReturn(dependencyValidationManager);
        return project;
    }
}
