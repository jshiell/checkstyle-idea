package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A configuration file on disc.
 */
public class FileConfigurationLocation extends ConfigurationLocation {

    @NonNls
    private static final Log LOG = LogFactory.getLog(FileConfigurationLocation.class);

    private final Project project;

    /**
     * Create a new file configuration.
     *
     * @param project     the project.
     * @param location    the location on disc.
     * @param description the optional description of the file.
     */
    FileConfigurationLocation(final Project project, final String location, final String description) {
        super(ConfigurationType.FILE, location, description);

        if (project == null) {
            throw new IllegalArgumentException("A project is required");
        }

        this.project = project;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getBaseDir() {
        final String location = getLocation();
        if (location != null) {
            final File locationFile = new File(getLocation());
            if (locationFile.exists()) {
                return locationFile.getParentFile();
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocation() {
        return untokenisePath(super.getLocation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLocation(final String location) {
        if (location == null || location.trim().length() == 0) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        super.setLocation(tokenisePath(location));
    }

    /**
     * {@inheritDoc}
     */
    protected InputStream resolveFile() throws IOException {
        final File locationFile = new File(getLocation());
        if (!locationFile.exists()) {
            throw new IOException("File does not exist: " + locationFile.getAbsolutePath());
        }

        return new FileInputStream(locationFile);
    }

    /**
     * Get the base path of the project.
     *
     * @return the base path of the project.
     */
    @Nullable
    private File getProjectPath() {
        if (project == null) {
            return null;
        }

        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }

    /**
     * Process a stored file path for any tokens.
     *
     * @param path the path to process.
     * @return the processed path.
     */
    private String untokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        LOG.debug("Processing file: " + path);

        if (path.startsWith(CheckStyleConstants.PROJECT_DIR)) {
            final File projectPath = getProjectPath();
            if (projectPath != null) {
                final File fullConfigFile = new File(projectPath,
                        path.substring(CheckStyleConstants.PROJECT_DIR.length()));
                return fullConfigFile.getAbsolutePath();
            } else {
                LOG.warn("Could not untokenise path as project dir is unset: "
                        + path);
            }
        }

        return path;
    }

    /**
     * Process a path and add tokens as necessary.
     *
     * @param path the path to processed.
     * @return the tokenised path.
     */
    private String tokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        final File projectPath = getProjectPath();
        if (projectPath != null) {
            final String projectPathAbs = projectPath.getAbsolutePath();
            if (path.startsWith(projectPathAbs)) {
                return CheckStyleConstants.PROJECT_DIR + path.substring(
                        projectPathAbs.length());
            }
        }

        return path;
    }
}
