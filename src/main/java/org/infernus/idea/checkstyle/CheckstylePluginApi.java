package org.infernus.idea.checkstyle;

import org.jetbrains.annotations.Nullable;

/**
 * This class contains methods known to be used by external dependencies.
 * <p/>
 * We guarantee nothing, but try to keep compatibility :-)
 */
public class CheckstylePluginApi {
    private CheckstyleProjectService checkstyleProjectService;

    public CheckstylePluginApi(final CheckstyleProjectService checkstyleProjectService) {
        this.checkstyleProjectService = checkstyleProjectService;
    }

    @Nullable
    public ClassLoader currentCheckstyleClassLoader() {
        return checkstyleProjectService.underlyingClassLoader();
    }
}
