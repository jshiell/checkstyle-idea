package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class UnsupportedImportedVersionWarnerTest {

    private final UnsupportedImportedVersionWarner.Notifier notifier =
            mock(UnsupportedImportedVersionWarner.Notifier.class);
    private final UnsupportedImportedVersionWarner warner = new UnsupportedImportedVersionWarner(notifier);
    private final Project project = mock(Project.class);

    @Test
    void warnsOnceForAnUnsupportedUnmappedVersion() {
        warner.warnIfUnsupported(project, "Maven", "99.0.0", "10.0");

        final ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(notifier, times(1)).showWarning(any(Project.class), text.capture());
        assertThat(text.getValue(), containsString("Maven"));
        assertThat(text.getValue(), containsString("99.0.0"));
        assertThat(text.getValue(), containsString("10.0"));
    }

    @Test
    void doesNotWarnAgainForTheSameVersionInTheSameProject() {
        warner.warnIfUnsupported(project, "Maven", "99.0.0", "10.0");
        warner.warnIfUnsupported(project, "Maven", "99.0.0", "10.0");

        verify(notifier, times(1)).showWarning(any(Project.class), anyString());
    }

    @Test
    void warnsAgainForADifferentVersion() {
        warner.warnIfUnsupported(project, "Maven", "99.0.0", "10.0");
        warner.warnIfUnsupported(project, "Maven", "98.0.0", "10.0");

        verify(notifier, times(2)).showWarning(any(Project.class), anyString());
    }

    @Test
    void warnsAgainForTheSameVersionInAnotherProject() {
        warner.warnIfUnsupported(project, "Maven", "99.0.0", "10.0");
        warner.warnIfUnsupported(mock(Project.class), "Maven", "99.0.0", "10.0");

        verify(notifier, times(2)).showWarning(any(Project.class), anyString());
    }

    @Test
    void doesNotWarnForASupportedVersion() {
        final String supported = new VersionListReader().getSupportedVersions().first();

        warner.warnIfUnsupported(project, "Maven", supported, "10.0");

        verify(notifier, never()).showWarning(any(Project.class), anyString());
    }

    @Test
    void doesNotWarnForAMappedVersion() {
        warner.warnIfUnsupported(project, "Maven", "10.21.2", "10.0");

        verify(notifier, never()).showWarning(any(Project.class), anyString());
    }

    @Test
    void doesNotWarnForAnUnresolvedMavenPlaceholder() {
        warner.warnIfUnsupported(project, "Maven", "${checkstyle.version}", "10.0");

        verify(notifier, never()).showWarning(any(Project.class), anyString());
    }
}
