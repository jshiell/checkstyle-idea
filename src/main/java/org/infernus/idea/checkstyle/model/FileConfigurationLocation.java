package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.Streams;
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

    private static final int BUFFER_SIZE = 4096;
    private static final String JAR_DELIMITER = ".jar!/";
    private static final String JAR_DELIMITER_REGEX = "\\.[jJ][aA][rR]!/";

    /**
     * Create a new file configuration.
     *
     * @param project the project.
     */
    FileConfigurationLocation(@NotNull final Project project,
                              @NotNull final String id) {
        this(project, id, ConfigurationType.LOCAL_FILE);
    }

    FileConfigurationLocation(@NotNull final Project project,
                              @NotNull final String id,
                              @NotNull final ConfigurationType configurationType) {
        super(id, configurationType, project);
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
        return projectFilePaths().detokenise(super.getLocation());
    }

    @Override
    public void setLocation(final String location) {
        if (isBlank(location)) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        super.setLocation(projectFilePaths().tokenise(location));
    }

    @NotNull
    protected InputStream resolveFile(@NotNull ClassLoader checkstyleClassLoader) throws IOException {
        final String detokenisedLocation = getLocation();
        if (isInJarFile(detokenisedLocation)) {
            return readLocationFromJar(detokenisedLocation);
        }

        final File locationFile = new File(detokenisedLocation);
        if (!locationFile.exists()) {
            throw new FileNotFoundException("File does not exist: " + locationFile.getAbsolutePath());
        }

        return Streams.inMemoryCopyOf(new FileInputStream(locationFile));
    }

    private InputStream readLocationFromJar(final String detokenisedLocation) throws IOException {
        final String[] fileParts = detokenisedLocation.split(JAR_DELIMITER_REGEX);
        final InputStream fileStream = readFileFromJar(fileParts[0], fileParts[1]);
        if (fileStream == null) {
            throw new FileNotFoundException("File does not exist: " + fileParts[0] + " containing " + fileParts[1]);
        }
        return fileStream;
    }

    @Nullable
    @Override
    public String resolveAssociatedFile(@Nullable final String filename,
                                        @Nullable final Module module,
                                        @NotNull final ClassLoader checkstyleClassLoader) throws IOException {
        final String associatedFile = super.resolveAssociatedFile(filename, module, checkstyleClassLoader);
        if (associatedFile != null) {
            return associatedFile;
        }

        final String detokenisedLocation = getLocation();
        if (isInJarFile(detokenisedLocation)) {
            return writeStreamToTemporaryFile(
                    readFileFromJar(detokenisedLocation.split(JAR_DELIMITER_REGEX)[0], filename),
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
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
            writeTo(fileStream, out);
            tempFile.deleteOnExit();
            return tempFile.getAbsolutePath();
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
        return detokenisedLocation != null && detokenisedLocation.toLowerCase().contains(JAR_DELIMITER);
    }

    private InputStream readFileFromJar(final String jarPath, final String filePath) throws IOException {
        try (ZipFile jarFile = new ZipFile(jarPath)) {
            for (final Enumeration<? extends ZipEntry> e = jarFile.entries(); e.hasMoreElements();) {
                final ZipEntry entry = e.nextElement();

                if (!entry.isDirectory() && entry.getName().equals(filePath)) {
                    return Streams.inMemoryCopyOf(jarFile.getInputStream(entry));
                }
            }
        }

        return null;
    }

    @NotNull
    protected ProjectFilePaths projectFilePaths() {
        return getProject().getService(ProjectFilePaths.class);
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new FileConfigurationLocation(getProject(), getId()));
    }
}
