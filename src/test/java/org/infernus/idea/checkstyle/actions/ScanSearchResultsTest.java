package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.usages.Usage;
import com.intellij.usages.rules.UsageInFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
