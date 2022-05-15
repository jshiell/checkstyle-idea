package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Stream;

public final class NamedScopeHelper {

    private NamedScopeHelper() {
    }

    public static final String DEFAULT_SCOPE_ID = "All";

    /**
     * Returns the scope with the given id.
     * If no scope with this ID exists, the Scope with the {@link #DEFAULT_SCOPE_ID} is being returned.
     */
    @NotNull
    public static NamedScope getScopeByIdWithDefaultFallback(@NotNull final Project project,
                                                             @NotNull final String id) {
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

    public static Stream<NamedScope> getAllScopes(@NotNull final Project project) {
        return Stream.concat(
                Arrays.stream(NamedScopeManager.getInstance(project).getScopes()),
                Arrays.stream(DependencyValidationManager.getInstance(project).getScopes()));
    }

    public static NamedScope getDefaultScope(@NotNull final Project project) {
        return DependencyValidationManager.getInstance(project).getScope(DEFAULT_SCOPE_ID);
    }

    public static boolean isFileInScope(final PsiFile psiFile, @NotNull final NamedScope namedScope) {
        final PackageSet packageSet = namedScope.getValue();
        if (packageSet == null) {
            return true;
        }

        return packageSet.contains(
                psiFile,
                DependencyValidationManager.getInstance(psiFile.getProject()));
    }
}
