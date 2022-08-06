package org.infernus.idea.checkstyle.util;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.regex.Pattern;

public final class FilePaths {

    private FilePaths() {
        // utility class
    }

    /**
     * Get the relative path from one file to another, specifying the directory separator.
     * If one of the provided resources does not exist, it is assumed to be a file unless it ends with '/' or
     * '\'.
     * <p/>
     * This (and the tests) is from <a href="http://stackoverflow.com/a/3054692">...</a> - all credit to the author.
     * Ideally we'd use URI.relativize(), but it only works in the prefixes match
     * (<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6226081">...</a>).
     *
     * @param targetPath    targetPath is calculated to this file
     * @param basePath      basePath is calculated from this file
     * @param pathSeparator directory separator. The platform default is not assumed so that we can test Unix behaviour when running on Windows (for example)
     * @return the relative file.
     * @see "http://stackoverflow.com/a/3054692"
     */
    public static String relativePath(@NotNull final String targetPath,
                                      @NotNull final String basePath,
                                      @NotNull final String pathSeparator) {
        // Normalize the paths
        String normalizedTargetPath = FilenameUtils.normalizeNoEndSeparator(targetPath);
        String normalizedBasePath = FilenameUtils.normalizeNoEndSeparator(basePath);

        // Undo the changes to the separators made by normalization
        if ("/".equals(pathSeparator)) {
            normalizedTargetPath = FilenameUtils.separatorsToUnix(normalizedTargetPath);
            normalizedBasePath = FilenameUtils.separatorsToUnix(normalizedBasePath);

        } else if ("\\".equals(pathSeparator)) {
            normalizedTargetPath = FilenameUtils.separatorsToWindows(normalizedTargetPath);
            normalizedBasePath = FilenameUtils.separatorsToWindows(normalizedBasePath);

        } else {
            throw new IllegalArgumentException("Unrecognised dir separator '" + pathSeparator + "'");
        }

        final String[] base = normalizedBasePath.split(Pattern.quote(pathSeparator));
        final String[] target = normalizedTargetPath.split(Pattern.quote(pathSeparator));

        // First get all the common elements. Store them as a string,
        // and also count how many of them there are.
        final StringBuilder common = new StringBuilder();

        int commonIndex = 0;
        while (commonIndex < target.length && commonIndex < base.length
                && target[commonIndex].equals(base[commonIndex])) {
            common.append(target[commonIndex]).append(pathSeparator);
            commonIndex++;
        }

        if (commonIndex == 0) {
            // No single common path element. This most
            // likely indicates differing drive letters, like C: and D:.
            // These paths cannot be relativized.
            throw new PathResolutionException("No common path element found for '" + normalizedTargetPath
                    + "' and '" + normalizedBasePath + "'");
        }

        // The number of directories we have to backtrack depends on whether the base is a file or a dir
        // For example, the relative path from
        //
        // /foo/bar/baz/gg/ff to /foo/bar/baz
        //
        // ".." if ff is a file
        // "../.." if ff is a directory
        //
        // The following is a heuristic to figure out if the base refers to a file or dir. It's not perfect, because
        // the resource referred to by this path may not actually exist, but it's the best I can do
        boolean baseIsFile = true;

        File baseResource = new File(normalizedBasePath);

        if (baseResource.exists()) {
            baseIsFile = baseResource.isFile();

        } else if (basePath.endsWith(pathSeparator)) {
            baseIsFile = false;
        }

        final StringBuilder relative = new StringBuilder();

        if (base.length != commonIndex) {
            int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

            for (int i = 0; i < numDirsUp; i++) {
                relative.append("..").append(pathSeparator);
            }
        }

        if (common.length() >= normalizedTargetPath.length()) {
            return ".";
        }

        relative.append(normalizedTargetPath.substring(common.length()));
        return relative.toString();
    }


    public static class PathResolutionException extends RuntimeException {
        private static final long serialVersionUID = -8628071255737767040L;

        PathResolutionException(final String msg) {
            super(msg);
        }
    }
}
