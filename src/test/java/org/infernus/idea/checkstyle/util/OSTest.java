package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class OSTest {

    @Test
    void isWindowsMatchesOsName() {
        String osName = System.getProperty("os.name").toLowerCase(java.util.Locale.ENGLISH);
        assertThat(OS.isWindows(), is(osName.contains("win")));
    }
}
