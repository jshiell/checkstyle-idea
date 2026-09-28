package org.infernus.idea.checkstyle;

import com.intellij.openapi.module.Module;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class CheckStyleModuleConfigurationEditorTest {

    @Test
    void displayNameIsCheckstyle() {
        final CheckStyleModuleConfigurationEditor editor = new CheckStyleModuleConfigurationEditor(mock(Module.class));

        assertEquals("Checkstyle", editor.getDisplayName());
    }
}
