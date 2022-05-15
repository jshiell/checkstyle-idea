package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;

/**
 * A configuration file on a mounted file system which will always be referred to
 * by a path relative to the project path.
 */
public class RelativeFileConfigurationLocation extends FileConfigurationLocation {

    RelativeFileConfigurationLocation(@NotNull final Project project,
                                      @NotNull final String id) {
        super(project, id, ConfigurationType.PROJECT_RELATIVE);
    }

    @Override
    public boolean canBeResolvedInDefaultProject() {
        return false;
    }

    @Override
    public void setLocation(final String location) {
        if (Strings.isBlank(location)) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        super.setLocation(projectFilePaths().tokenise(
                projectFilePaths().makeProjectRelative(
                        projectFilePaths().detokenise(location))));
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new RelativeFileConfigurationLocation(getProject(), getId()));
    }
}
