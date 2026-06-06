package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileTypesTest {

    @Mock
    private VirtualFile virtualFile;

    @Test
    void javaFileHasJavaExtension() {
        when(virtualFile.getExtension()).thenReturn("java");
        assertTrue(FileTypes.hasJavaExtension(virtualFile));
    }

    @Test
    void javaExtensionIsCaseInsensitive() {
        when(virtualFile.getExtension()).thenReturn("JAVA");
        assertTrue(FileTypes.hasJavaExtension(virtualFile));
    }

    @Test
    void kotlinFileDoesNotHaveJavaExtension() {
        when(virtualFile.getExtension()).thenReturn("kt");
        assertFalse(FileTypes.hasJavaExtension(virtualFile));
    }

    @Test
    void nullExtensionDoesNotHaveJavaExtension() {
        when(virtualFile.getExtension()).thenReturn(null);
        assertFalse(FileTypes.hasJavaExtension(virtualFile));
    }
}
