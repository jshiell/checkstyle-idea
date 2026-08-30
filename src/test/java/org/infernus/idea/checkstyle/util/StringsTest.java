package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class StringsTest {

    @Test
    void nullIsBlank() {
        assertThat(Strings.isBlank(null), is(true));
    }

    @Test
    void emptyStringIsBlank() {
        assertThat(Strings.isBlank(""), is(true));
    }

    @Test
    void whitespaceOnlyIsBlank() {
        assertThat(Strings.isBlank("   "), is(true));
    }

    @Test
    void nonEmptyStringIsNotBlank() {
        assertThat(Strings.isBlank("hello"), is(false));
    }

    @Test
    void stringWithLeadingAndTrailingWhitespaceIsNotBlank() {
        assertThat(Strings.isBlank("  hello  "), is(false));
    }

    @Test
    void nullIsNotHttpUrl() {
        assertThat(Strings.isHttpUrl(null), is(false));
    }

    @Test
    void blankIsNotHttpUrl() {
        assertThat(Strings.isHttpUrl("   "), is(false));
    }

    @Test
    void bareFilenameIsNotHttpUrl() {
        assertThat(Strings.isHttpUrl("foo.jar"), is(false));
    }

    @Test
    void absolutePathIsNotHttpUrl() {
        assertThat(Strings.isHttpUrl("/abs/path.jar"), is(false));
    }

    @Test
    void httpUrlIsHttpUrl() {
        assertThat(Strings.isHttpUrl("http://x/y.jar"), is(true));
    }

    @Test
    void httpsUrlIsCaseInsensitivelyHttpUrl() {
        assertThat(Strings.isHttpUrl("HTTPS://X/Y.JAR"), is(true));
    }

    @Test
    void httpUrlWithSurroundingWhitespaceIsHttpUrl() {
        assertThat(Strings.isHttpUrl("  https://x/y.jar  "), is(true));
    }
}
