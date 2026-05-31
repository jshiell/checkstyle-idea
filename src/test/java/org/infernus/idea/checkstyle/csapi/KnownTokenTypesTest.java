package org.infernus.idea.checkstyle.csapi;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class KnownTokenTypesTest {

    @Test
    void allExpectedTokensAreDeclared() {
        KnownTokenTypes[] values = KnownTokenTypes.values();
        assertThat(values.length, is(61));
    }

    @Test
    void assignTokenIsPresent() {
        assertThat(KnownTokenTypes.valueOf("ASSIGN"), is(KnownTokenTypes.ASSIGN));
    }

    @Test
    void classDefTokenIsPresent() {
        assertThat(KnownTokenTypes.valueOf("CLASS_DEF"), is(KnownTokenTypes.CLASS_DEF));
    }

    @Test
    void methodDefTokenIsPresent() {
        assertThat(KnownTokenTypes.valueOf("METHOD_DEF"), is(notNullValue()));
    }
}
