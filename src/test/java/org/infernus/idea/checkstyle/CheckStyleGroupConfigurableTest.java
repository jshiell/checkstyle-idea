package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class CheckStyleGroupConfigurableTest {

    private final CheckStyleGroupConfigurable configurable = new CheckStyleGroupConfigurable();

    @Test
    void displayNameIsCheckstyle() {
        assertEquals("Checkstyle", configurable.getDisplayName());
    }

    @Test
    void hasNoContentOfItsOwn() {
        assertNull(configurable.createComponent());
    }

    @Test
    void isNeverModified() {
        assertFalse(configurable.isModified());
    }
}
