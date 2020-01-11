package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


/**
 * Replaces URLs pointing to files inside the project directory with URLs pointing to copies of these files stored in a
 * temporary directory. This prevents them from getting locked by our classloaders.
 */
public class ClasspathStabilizer {

    private static final Logger LOG = Logger.getInstance(ClasspathStabilizer.class);

    static final String HASHFOLDER = "hashed";

    /**
     * number of characters in the relative path of the library up to which we would still use the path as-is; above
     * that, we use hashes of the path instead in order to avoid path length problems on Windows
     */
    private static final int CLEAR_PATH_THRESHOLD_CHARS = 50;

    private final Project project;

    private final Path copyDir;


    /**
     * Constructor.
     *
     * @param pProject the current IDEA project
     * @param pTempDir project-specific directory to keep copied libs in, retained after IDEA is closed
     */
    public ClasspathStabilizer(@NotNull final Project pProject, @NotNull final Path pTempDir) {
        project = pProject;
        copyDir = pTempDir;
    }


    @NotNull
    public URL[] stabilize(@NotNull final List<URL> pUrls) {
        URL[] result = null;
        try {
            final Optional<Path> projectDir = getProjectDir();
            if (projectDir.isPresent()) {
                final List<URL> stabilizedList = new ArrayList<>(pUrls.size() + 1);
                for (final URL url : pUrls) {
                    URL stabilizedUrl = url;
                    if (residesInProject(projectDir.get(), url)) {
                        stabilizedUrl = locateOrCreateCopy(projectDir.get(), url);
                    }
                    stabilizedList.add(stabilizedUrl);
                }
                result = stabilizedList.toArray(new URL[0]);
            }
        } catch (IOException | URISyntaxException | RuntimeException e) {
            LOG.warn("Failed to stabilize the classpath. Using original classpath. Some files may become locked.", e);
        }
        if (result == null) {
            result = pUrls.toArray(new URL[0]);
        }
        return result;
    }


    @NotNull
    private URL locateOrCreateCopy(@NotNull final Path pProjectDir, final URL pUrl) throws IOException,
            URISyntaxException {
        final Path urlPath = Paths.get(pUrl.toURI()).toRealPath();
        final Path relativePathSrc = pProjectDir.relativize(urlPath);
        final Path relativePathTarget = buildTargetRelativePath(relativePathSrc);
        final Path targetPath = copyDir.resolve(relativePathTarget);
        if (needsUpdate(targetPath, urlPath)) {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.copy(urlPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
        return targetPath.toUri().toURL();
    }


    private boolean needsUpdate(@NotNull final Path pTargetPath, @NotNull final Path pSourcePath) throws IOException {
        boolean result = true;
        if (Files.exists(pTargetPath)) {
            final long sizeSource = Files.size(pSourcePath);
            final long sizeTarget = Files.size(pTargetPath);
            final long modTimeSecsSource = Files.getLastModifiedTime(pSourcePath).to(TimeUnit.SECONDS);
            final long modTimeSecsTarget = Files.getLastModifiedTime(pTargetPath).to(TimeUnit.SECONDS);
            result = sizeSource != sizeTarget || modTimeSecsSource != modTimeSecsTarget;
        }
        return result;
    }


    @NotNull
    private Path buildTargetRelativePath(@NotNull final Path pRelativePathSrc) {
        Path result = pRelativePathSrc;
        if (pRelativePathSrc.toString().length() > CLEAR_PATH_THRESHOLD_CHARS
                || pRelativePathSrc.startsWith(HASHFOLDER)
                || pRelativePathSrc.equals(Paths.get(TempDirProvider.README_FILE))) {
            final Path parent = pRelativePathSrc.getParent();
            final String parentHash = hash(parent != null ? parent.toString() : "");
            result = Paths.get(HASHFOLDER, parentHash, pRelativePathSrc.getFileName().toString());
        }
        return result;
    }

    @NotNull
    private String hash(@NotNull final String pString) {
        final String s = pString.replaceAll(Pattern.quote("\\"), "/");
        final String h = new Base32().encodeAsString(DigestUtils.sha1(s)).toLowerCase(Locale.ENGLISH);
        final StringBuilder sb = new StringBuilder(h);
        for (int i = sb.length() - 1; i >= 0 && sb.charAt(i) == '='; i--) {
            sb.deleteCharAt(i);
        }
        return sb.toString();
    }


    private boolean residesInProject(@NotNull final Path pProjectDir, @NotNull final URL pUrl) throws IOException,
            URISyntaxException {
        boolean result = false;
        if ("file".equals(pUrl.getProtocol())) {
            Path urlPath = Paths.get(pUrl.toURI());
            if (Files.isRegularFile(urlPath)) {
                urlPath = urlPath.toRealPath();
                result = urlPath.startsWith(pProjectDir);
            }
        }
        return result;
    }


    private Optional<Path> getProjectDir() throws IOException {
        Optional<Path> result = Optional.empty();
        String basePath = project.getBasePath();
        if (basePath != null) {
            if (basePath.length() > 2
                    && (basePath.startsWith("/") || basePath.startsWith("\\"))
                    && basePath.charAt(2) == ':') {
                // e.g. "/D:/projects/foo", then we must cut off the leading slash
                basePath = basePath.substring(1);
            }
            result = Optional.of(Paths.get(basePath).toRealPath());
        }
        return result;
    }
}
