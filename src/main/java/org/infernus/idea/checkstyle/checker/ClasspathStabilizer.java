package org.infernus.idea.checkstyle.checker;

import java.io.File;
import java.net.URL;

import org.jetbrains.annotations.NotNull;


/**
 * Replaces URLs pointing to files inside the project directory with URLs pointing to copies of these files stored in a
 * temporary directory. This prevents them from getting locked by our classloaders.
 */
public class ClasspathStabilizer
{
    private final File tempDir;

    public ClasspathStabilizer(@NotNull final File pTempDir) {
        tempDir = pTempDir;
    }

    // TODO

    public URL[] asArray() {
        // TODO
        return null;
    }
}
