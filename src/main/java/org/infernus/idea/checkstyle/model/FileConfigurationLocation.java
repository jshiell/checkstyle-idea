package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.jetbrains.annotations.Nullable;

import java.io.*;

/**
 * A configuration file on a mounted file system.
 */
public class FileConfigurationLocation extends ConfigurationLocation {

    private static final Log LOG = LogFactory.getLog(FileConfigurationLocation.class);

    private final Project project;

    private File cachedProjectBase;

    /**
     * Create a new file configuration.
     *
     * @param project the project.
     */
    FileConfigurationLocation(final Project project) {
        this(project, ConfigurationType.LOCAL_FILE);
    }

    FileConfigurationLocation(final Project project, final ConfigurationType configurationType) {
        super(configurationType);

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
        return detokenisePath(super.getLocation());
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
            throw new FileNotFoundException("File does not exist: " + absolutePathOf(locationFile));
        }

        return new FileInputStream(locationFile);
    }

    /**
     * Get the base path of the project.
     *
     * @return the base path of the project.
     */
    @Nullable
    File getProjectPath() {
        if (cachedProjectBase != null) {
            return cachedProjectBase;
        }

        if (project == null) {
            return null;
        }

        try {
            final VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) {
                return null;
            }

            cachedProjectBase = new File(baseDir.getPath());
            return cachedProjectBase;

        } catch (Exception e) {
            // IDEA 10.5.2 sometimes throws an AssertionException in project.getBaseDir()
            LOG.debug("Couldn't retrieve base location", e);
            return null;
        }
    }

    /**
     * Process a stored file path for any tokens, and resolve the *nix style path
     * to the local filesystem path encoding.
     *
     * @param path the path to process, in (tokenised) URI syntax.
     * @return the processed path, in local file path syntax.
     */
    String detokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        LOG.debug("Processing file: " + path);

        for (String prefix : new String[]{CheckStyleConstants.PROJECT_DIR, CheckStyleConstants.LEGACY_PROJECT_DIR}) {
            if (path.startsWith(prefix)) {
                // path is relative to project dir
                final File projectPath = getProjectPath();
                if (projectPath != null) {
                    final String projectRelativePath = fromUnixPath(path.substring(prefix.length()));
                    final String completePath = projectPath + File.separator + projectRelativePath;
                    return absolutePathOf(new File(completePath));

                } else {
                    LOG.warn("Could not untokenise path as project dir is unset: " + path);
                }
            }
        }

        return fromUnixPath(path);
    }

    /**
     * Process a path, add tokens as necessary and encode it a *nix-style path.
     *
     * @param path the path to process, in local file path syntax.
     * @return the tokenised path in URI syntax.
     */
    String tokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        final File projectPath = getProjectPath();
        if (projectPath != null && path.startsWith(absolutePathOf(projectPath) + separatorChar())) {
            return CheckStyleConstants.PROJECT_DIR
                    + toUnixPath(path.substring(absolutePathOf(projectPath).length()));
        }
        return toUnixPath(path);
    }

    String absolutePathOf(final File file) {
        return file.getAbsolutePath();
    }

    private String toUnixPath(final String path) {
        if (separatorChar() == '/') {
            return path;
        }
        return path.replace(separatorChar(), '/');
    }

    char separatorChar() {
        return File.separatorChar;
    }

    private String fromUnixPath(final String path) {
        if (separatorChar() == '/') {
            return path;
        }
        return path.replace('/', separatorChar());
    }

    Project getProject() {
        return project;
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new FileConfigurationLocation(project));
    }
}
