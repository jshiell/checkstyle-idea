package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.FileUtils;

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
        if (location == null || location.trim().length() == 0) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        super.setLocation(tokenisePath(makeProjectRelative(location)));
    }

    private String makeProjectRelative(final String path) {
        final File projectPath = getProjectPath();
        if (projectPath == null) {
            LOG.debug("Couldn't find project path, returning full path: " + path);
            return path;
        }

        try {
            final String basePath = projectPath.getAbsolutePath() + File.separator;
            return basePath + FileUtils.getRelativePath(path, basePath, File.separator);

        } catch (FileUtils.PathResolutionException e) {
            LOG.debug("No common path was found between " + path + " and " + projectPath.getAbsolutePath());
            return path;
        }
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new RelativeFileConfigurationLocation(getProject()));
    }
}
