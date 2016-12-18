package org.infernus.idea.checkstyle.checker;

import java.util.Objects;

import com.intellij.openapi.module.Module;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


class CheckerFactoryCacheKey
{
    private final String projectName;

    private final String moduleName;

    private final ConfigurationLocation location;


    CheckerFactoryCacheKey(@NotNull final ConfigurationLocation location, @Nullable final Module module, @Nullable
    final String pProjectName) {
        this.projectName = pProjectName != null ? pProjectName : "noProject";
        this.moduleName = module != null ? module.getName() : "noModule";
        this.location = location;
    }


    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (pOther == null || getClass() != pOther.getClass()) {
            return false;
        }
        final CheckerFactoryCacheKey other = (CheckerFactoryCacheKey) pOther;
        return Objects.equals(projectName, other.projectName) && Objects.equals(moduleName, other.moduleName) &&
                Objects.equals(location, other.location);
    }


    @Override
    public int hashCode() {
        return Objects.hash(projectName, moduleName, location);
    }
}
