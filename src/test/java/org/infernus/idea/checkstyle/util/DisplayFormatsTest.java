package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DisplayFormatsTest {

    @Test
    void classNameWithPackageReturnsSimpleName() {
        assertThat(DisplayFormats.shortenClassName("com.example.FooCheck"), is("Foo"));
    }

    @Test
    void classNameWithoutPackageReturnedAsIs() {
        assertThat(DisplayFormats.shortenClassName("FooCheck"), is("FooCheck"));
    }

    @Test
    void checkSuffixIsStripped() {
        assertThat(DisplayFormats.shortenClassName("com.puppycrawl.tools.checkstyle.checks.NeedBracesCheck"), is("NeedBraces"));
    }

    @Test
    void classWithNoCheckSuffixIsReturnedUnmodified() {
        assertThat(DisplayFormats.shortenClassName("com.example.MyRule"), is("MyRule"));
    }

    @Test
    void classNameWithOnlyPackageSeparatorAndCheckSuffix() {
        assertThat(DisplayFormats.shortenClassName("pkg.Check"), is(""));
    }
}
