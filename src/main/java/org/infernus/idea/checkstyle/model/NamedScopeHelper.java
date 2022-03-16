package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class NamedScopeHelper {

    public static final String DEFAULT_SCOPE_ID = "All";

    public static Optional<NamedScope> getAnyScope(Project project) {
        final NamedScopeManager myLocalScopesManager = NamedScopeManager.getInstance(project);
        final DependencyValidationManager mySharedScopesManager = DependencyValidationManager.getInstance(project);

        return Stream.concat(
                        Arrays.stream(myLocalScopesManager.getScopes()),
                        Arrays.stream(mySharedScopesManager.getScopes()))
                .findAny();
    }

    /**
     * Returns the scope with the given id.
     * If no scope with this ID exists, the Scope with the {@link #DEFAULT_SCOPE_ID} is being returned.
     */
    @Nullable
    public static NamedScope getScopeByIdWithDefaultFallback(Project project, String id) {
        final NamedScope localScopeOrNull = NamedScopeManager.getInstance(project).getScope(id);
        if (localScopeOrNull != null) {
            return localScopeOrNull;
        }
        final NamedScope sharedScopeOrNull = DependencyValidationManager.getInstance(project).getScope(id);
        if (sharedScopeOrNull != null) {
            return sharedScopeOrNull;
        }

        return getDefaultScope(project);
    }

    public static Stream<NamedScope> getAllScopes(Project project) {
        return Stream.concat(
                Arrays.stream(NamedScopeManager.getInstance(project).getScopes()),
                Arrays.stream(DependencyValidationManager.getInstance(project).getScopes()));
    }

    public static NamedScope getDefaultScope(Project project) {
        return DependencyValidationManager.getInstance(project).getScope(DEFAULT_SCOPE_ID);
    }
}
