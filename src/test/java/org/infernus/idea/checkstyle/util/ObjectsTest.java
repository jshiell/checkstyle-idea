package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

class ObjectsTest {

    @Test
    void bothNullReturnsZero() {
        assertThat(Objects.compare(null, null), is(0));
    }

    @Test
    void firstNullReturnsNegative() {
        assertThat(Objects.compare(null, "a"), is(lessThan(0)));
    }

    @Test
    void secondNullReturnsPositive() {
        assertThat(Objects.compare("a", null), is(greaterThan(0)));
    }

    @Test
    void equalValuesReturnZero() {
        assertThat(Objects.compare("hello", "hello"), is(0));
    }

    @Test
    void smallerFirstReturnsNegative() {
        assertThat(Objects.compare("a", "b"), is(lessThan(0)));
    }

    @Test
    void largerFirstReturnsPositive() {
        assertThat(Objects.compare("b", "a"), is(greaterThan(0)));
    }

    @Test
    void worksWithIntegers() {
        assertThat(Objects.compare(1, 2), is(lessThan(0)));
        assertThat(Objects.compare(2, 1), is(greaterThan(0)));
        assertThat(Objects.compare(5, 5), is(equalTo(0)));
    }
}
