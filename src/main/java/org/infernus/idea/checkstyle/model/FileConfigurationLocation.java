package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.*;

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
     * @param project the project.
     */
    FileConfigurationLocation(final Project project) {
        super(ConfigurationType.FILE);

        if (project == null) {
            throw new IllegalArgumentException("A project is required");
        }

        this.project = project;
    }

    @Override
    public File getBaseDir() {
        final String location = getLocation();
        if (location != null) {
            final File locationFile = new File(location);
            if (locationFile.exists()) {
                return locationFile.getParentFile();
            }
        }

        return null;
    }

    @Override
    public String getLocation() {
        return untokenisePath(super.getLocation());
    }

    @Override
    public void setLocation(final String location) {
        if (location == null || location.trim().length() == 0) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        super.setLocation(tokenisePath(location));
    }

    protected InputStream resolveFile() throws IOException {
        final File locationFile = new File(getLocation());
        if (!locationFile.exists()) {
            throw new FileNotFoundException("File does not exist: " + locationFile.getAbsolutePath());
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
     * Process a stored file path for any tokens, and resolve the platform independent URI syntax
     * to the local filesystem path encoding.
     *
     * @param path the path to process, in (tokenised) URI syntax.
     * @return the processed path, in local file path syntax.
     */
    private String untokenisePath(String path) {
        if (path == null) {
            return null;
        }

        LOG.debug("Processing file: " + path);

        // replace URI style slashes with the platform slash
        path = path.replace('/', File.separatorChar);

        String untokenisedPath = null;

        if (path.startsWith(CheckStyleConstants.PROJECT_DIR)) {
            // path is relative to project dir
            final File projectPath = getProjectPath();
            if (projectPath != null) {
                final String filename = path.substring(CheckStyleConstants.PROJECT_DIR.length());
                untokenisedPath = new File(projectPath, filename).getAbsolutePath();
            } else {
                LOG.warn("Could not untokenise path as project dir is unset: "
                        + path);
            }

        } else {
            // absolute path: turn the URI into a File path
            untokenisedPath = path;
        }

        return untokenisedPath;
    }

    /**
     * Process a path, add tokens as necessary and encode it in platform independent URI syntax.
     *
     * @param path the path to process, in local file path syntax.
     * @return the tokenised path in URI syntax.
     */
    private String tokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        String tokenisedPath = toURI(path);

        final File projectPath = getProjectPath();
        if (projectPath != null) {
            final String projectPathAbs = projectPath.getAbsolutePath();
            if (path.startsWith(projectPathAbs)) {
                tokenisedPath = CheckStyleConstants.PROJECT_DIR
                        + toURI(path.substring(projectPathAbs.length()));
            }
        }

        return tokenisedPath;
    }

    private String toURI(final String s) {
        return s.replace(File.separatorChar, '/');
    }
}
