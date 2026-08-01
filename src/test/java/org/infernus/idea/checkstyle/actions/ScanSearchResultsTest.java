package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.usages.Usage;
import com.intellij.usages.rules.UsageInFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanSearchResultsTest {

    @Test
    public void filesFromReturnsTheFilesBehindUsagesInFiles() {
        final VirtualFile fileOne = validFile();
        final VirtualFile fileTwo = validFile();

        assertThat(ScanSearchResults.filesFrom(List.of(usageIn(fileOne), usageIn(fileTwo))),
                contains(fileOne, fileTwo));
    }

    @Test
    public void filesFromReturnsNothingForNoUsages() {
        assertThat(ScanSearchResults.filesFrom(List.of()), is(empty()));
    }

    @Test
    public void filesFromSkipsUsagesThatAreNotInFiles() {
        final Usage usageWithoutAFile = mock(Usage.class);
        when(usageWithoutAFile.isValid()).thenReturn(true);
        final VirtualFile file = validFile();

        assertThat(ScanSearchResults.filesFrom(List.of(usageWithoutAFile, usageIn(file))),
                contains(file));
    }

    @Test
    public void filesFromSkipsUsagesWithNoFile() {
        final UsageInFile usageWithNullFile = mock(UsageInFile.class);
        when(usageWithNullFile.isValid()).thenReturn(true);
        when(usageWithNullFile.getFile()).thenReturn(null);
        final VirtualFile file = validFile();

        assertThat(ScanSearchResults.filesFrom(List.of(usageWithNullFile, usageIn(file))),
                contains(file));
    }

    @Test
    public void filesFromSkipsInvalidUsages() {
        final VirtualFile fileOfInvalidUsage = validFile();
        final UsageInFile invalidUsage = mock(UsageInFile.class);
        when(invalidUsage.isValid()).thenReturn(false);
        when(invalidUsage.getFile()).thenReturn(fileOfInvalidUsage);
        final VirtualFile file = validFile();

        assertThat(ScanSearchResults.filesFrom(List.of(invalidUsage, usageIn(file))),
                contains(file));
    }

    @Test
    public void filesFromSkipsInvalidFiles() {
        final VirtualFile invalidFile = mock(VirtualFile.class);
        when(invalidFile.isValid()).thenReturn(false);
        final VirtualFile file = validFile();

        assertThat(ScanSearchResults.filesFrom(List.of(usageIn(invalidFile), usageIn(file))),
                contains(file));
    }

    private Usage usageIn(final VirtualFile file) {
        final UsageInFile usage = mock(UsageInFile.class);
        when(usage.isValid()).thenReturn(true);
        when(usage.getFile()).thenReturn(file);
        return usage;
    }

    private VirtualFile validFile() {
        final VirtualFile file = mock(VirtualFile.class);
        when(file.isValid()).thenReturn(true);
        return file;
    }
}
