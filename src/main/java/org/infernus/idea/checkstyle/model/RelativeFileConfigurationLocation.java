package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.FilePaths;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * A configuration file on a mounted file system which will always be referred to
 * by a path relative to the project path.
 */
public class RelativeFileConfigurationLocation extends FileConfigurationLocation {

    private static final Log LOG = LogFactory.getLog(RelativeFileConfigurationLocation.class);

    RelativeFileConfigurationLocation(final Project project) {
        super(project, ConfigurationType.PROJECT_RELATIVE);
    }

    @Override
    public void setLocation(final String location) {
        if (Strings.isBlank(location)) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        super.setLocation(tokenisePath(makeProjectRelative(detokenisePath(location))));
    }

    private String makeProjectRelative(@NotNull final String path) {
        final File projectPath = getProjectPath();
        if (projectPath == null) {
            LOG.debug("Couldn't find project path, returning full path: " + path);
            return path;
        }

        try {
            final String basePath = absolutePathOf(projectPath) + separatorChar();
            return basePath + FilePaths.relativePath(path, basePath, "" + separatorChar());

        } catch (FilePaths.PathResolutionException e) {
            LOG.debug("No common path was found between " + path + " and " + projectPath.getAbsolutePath());
            return path;

        } catch (Exception e) {
            throw new RuntimeException("Failed to make relative: " + path, e);
        }
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new RelativeFileConfigurationLocation(getProject()));
    }
}
