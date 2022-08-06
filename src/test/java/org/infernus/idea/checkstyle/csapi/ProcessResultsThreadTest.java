package org.infernus.idea.checkstyle.csapi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.Problem;
import org.junit.Test;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessResultsThreadTest {

    @Test
    public void fileWithMatchingPathIsLinked() {
        PsiFile expectedFile = aPsiFile();
        Map<String, PsiFile> fileNamesToPsiFiles = new HashMap<>();
        fileNamesToPsiFiles.put("aFileName", expectedFile);
        fileNamesToPsiFiles.put("anotherFileName", mock(PsiFile.class));

        ProcessResultsThread underTest = underTest(fileNamesToPsiFiles, singletonList(anIssueFor("aFileName")));
        underTest.run();

        assertThat(underTest.getProblems(),
                hasEntry(expectedFile, singletonList(aProblemFor(expectedFile))));
    }

    @Test
    public void fileWithMatchingButDenormalisedPathIsLinked() {
        PsiFile expectedFile = aPsiFile();

        Map<String, PsiFile> fileNamesToPsiFiles = new HashMap<>();
        fileNamesToPsiFiles.put("aFileName", expectedFile);
        fileNamesToPsiFiles.put("anotherFileName", mock(PsiFile.class));

        List<Issue> events = singletonList(anIssueFor("foo/bar/../../aFileName"));

        ProcessResultsThread underTest = underTest(fileNamesToPsiFiles, events);

        underTest.run();

        assertThat(underTest.getProblems(),
                hasEntry(expectedFile, singletonList(aProblemFor(expectedFile))));
    }

    private ProcessResultsThread underTest(final Map<String, PsiFile> fileNamesToPsiFiles, final List<Issue> events) {
        return new ProcessResultsThread(
                false, Collections.emptyList(), 4, Optional.empty(), events, fileNamesToPsiFiles);
    }

    private PsiFile aPsiFile() {
        PsiFile expectedFile = mock(PsiFile.class);
        when(expectedFile.textToCharArray()).thenReturn("import boo.*;".toCharArray());
        when(expectedFile.findElementAt(6)).thenReturn(mock(PsiElement.class));
        return expectedFile;
    }

    private Issue anIssueFor(final String aFileName) {
        return new Issue(aFileName, 1, 7, "aMessage", SeverityLevel.Error, "aSourceName");
    }

    private Problem aProblemFor(final PsiFile expectedFile) {
        return new Problem(expectedFile, "aMessage", SeverityLevel.Error, 1, 7, "com.checkstyle.rules.aCheck", false, false);
    }

}
