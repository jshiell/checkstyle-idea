package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

class CheckStyleBundleTest {

    @Test
    void pathDownloadTitleIncludesTheUrl() {
        final String title = CheckStyleBundle.message("config.path.download.title", "https://x/y.jar");

        assertThat(title, containsString("https://x/y.jar"));
    }
}
