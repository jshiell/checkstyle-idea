package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.infernus.idea.checkstyle.util.Strings.isBlank;

/**
 * A configuration file on a mounted file system.
 */
public class FileConfigurationLocation extends ConfigurationLocation {

    private static final Log LOG = LogFactory.getLog(FileConfigurationLocation.class);
    private static final int BUFFER_SIZE = 4096;

    private final Project project;

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
        if (isBlank(location)) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        super.setLocation(tokenisePath(location));
    }

    @NotNull
    protected InputStream resolveFile() throws IOException {
        final String detokenisedLocation = getLocation();
        if (isInJarFile(detokenisedLocation)) {
            return readLocationFromJar(detokenisedLocation);
        }

        final File locationFile = new File(detokenisedLocation);
        if (!locationFile.exists()) {
            throw new FileNotFoundException("File does not exist: " + absolutePathOf(locationFile));
        }

        return new FileInputStream(locationFile);
    }

    private InputStream readLocationFromJar(final String detokenisedLocation) throws IOException {
        final String[] fileParts = detokenisedLocation.split("!/");
        final InputStream fileStream = readFileFromJar(fileParts[0], fileParts[1]);
        if (fileStream == null) {
            throw new FileNotFoundException("File does not exist: " + fileParts[0] + " containing " + fileParts[1]);
        }
        return fileStream;
    }

    @Nullable
    @Override
    public String resolveAssociatedFile(final String filename, final Module module) throws IOException {
        final String associatedFile = super.resolveAssociatedFile(filename, module);
        if (associatedFile != null) {
            return associatedFile;
        }

        final String detokenisedLocation = getLocation();
        if (isInJarFile(detokenisedLocation)) {
            return writeStreamToTemporaryFile(
                    readFileFromJar(detokenisedLocation.split("!/")[0], filename),
                    extensionOf(filename));
        }

        return null;
    }

    private String extensionOf(final String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return ".tmp";
    }

    private String writeStreamToTemporaryFile(final InputStream fileStream,
                                              final String fileSuffix) throws IOException {
        if (fileStream == null) {
            return null;
        }

        final File tempFile = File.createTempFile("csidea-", fileSuffix);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(tempFile));
            writeTo(fileStream, out);
            tempFile.deleteOnExit();
            return tempFile.getAbsolutePath();

        } finally {
            closeQuietly(out, fileStream);
        }
    }

    private void writeTo(final InputStream fileStream, final BufferedOutputStream out) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = fileStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private boolean isInJarFile(final String detokenisedLocation) {
        return detokenisedLocation != null && detokenisedLocation.toLowerCase().contains(".jar!/");
    }

    public InputStream readFileFromJar(final String jarPath, final String filePath) throws IOException {
        ZipFile jarFile = null;
        try {
            jarFile = new ZipFile(jarPath);
            for (final Enumeration<? extends ZipEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
                final ZipEntry entry = e.nextElement();

                if (!entry.isDirectory() && entry.getName().equals(filePath)) {
                    BufferedInputStream bis = null;

                    try {
                        bis = new BufferedInputStream(jarFile.getInputStream(entry));
                        return new ByteArrayInputStream(readFrom(bis));

                    } finally {
                        closeQuietly(bis);
                    }
                }
            }

        } finally {
            closeQuietly(jarFile);
        }

        return null;
    }

    private byte[] readFrom(final BufferedInputStream bis) throws IOException {
        final ByteArrayOutputStream rulesFile = new ByteArrayOutputStream();
        final byte[] readBuffer = new byte[BUFFER_SIZE];
        int count;
        while ((count = bis.read(readBuffer, 0, BUFFER_SIZE)) != -1) {
            rulesFile.write(readBuffer, 0, count);
        }
        return rulesFile.toByteArray();
    }

    private static void closeQuietly(final ZipFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(final Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Get the base path of the project.
     *
     * @return the base path of the project.
     */
    @Nullable
    File getProjectPath() {
        if (project == null) {
            return null;
        }

        try {
            final VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) {
                return null;
            }

            return new File(baseDir.getPath());

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

        for (String prefix : new String[]{CheckStyleConfiguration.PROJECT_DIR, CheckStyleConfiguration.LEGACY_PROJECT_DIR}) {
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
            return CheckStyleConfiguration.PROJECT_DIR
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
