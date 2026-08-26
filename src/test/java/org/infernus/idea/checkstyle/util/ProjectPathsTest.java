package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ProjectPathsTest {

    @Test
    void modulePathPrefersTheExternalProjectPathOverGuessModuleDir() {
        final Module module = mock(Module.class);
        final VirtualFile externalProjectDir = mock(VirtualFile.class);
        when(externalProjectDir.isDirectory()).thenReturn(true);
        final VirtualFile guessedModuleDir = mock(VirtualFile.class);

        final LocalFileSystem localFileSystem = mock(LocalFileSystem.class);
        when(localFileSystem.findFileByPath("/an/external/project/path")).thenReturn(externalProjectDir);

        try (MockedStatic<ExternalSystemApiUtil> externalSystemApiUtil = mockStatic(ExternalSystemApiUtil.class);
             MockedStatic<LocalFileSystem> localFileSystemStatic = mockStatic(LocalFileSystem.class);
             MockedStatic<ProjectUtil> projectUtil = mockStatic(ProjectUtil.class)) {

            externalSystemApiUtil.when(() -> ExternalSystemApiUtil.getExternalProjectPath(module))
                    .thenReturn("/an/external/project/path");
            localFileSystemStatic.when(LocalFileSystem::getInstance).thenReturn(localFileSystem);
            projectUtil.when(() -> ProjectUtil.guessModuleDir(module)).thenReturn(guessedModuleDir);

            final VirtualFile result = new ProjectPaths().modulePath(module);

            assertThat(result, is(sameInstance(externalProjectDir)));
        }
    }
}
