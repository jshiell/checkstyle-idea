package org.infernus.idea.checkstyle.csapi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.Problem;
import org.junit.jupiter.api.Test;

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

    @Test
    public void unknownFileMappingDoesNotAbortProcessingOfSubsequentEvents() {
        PsiFile expectedFile = aPsiFile();
        Map<String, PsiFile> fileNamesToPsiFiles = new HashMap<>();
        fileNamesToPsiFiles.put("knownFile", expectedFile);

        List<Issue> events = Arrays.asList(
                anIssueFor("unknownFile"),
                anIssueFor("knownFile"));

        ProcessResultsThread underTest = underTest(fileNamesToPsiFiles, events);
        underTest.run();

        assertThat(underTest.getProblems(),
                hasEntry(expectedFile, singletonList(aProblemFor(expectedFile))));
    }

    @Test
    public void multiLineFileUsesCorrectOffsetForUncachedLines() {
        // "foo\nbar" — line 2, column 2 → index 5 (the 'a' in 'bar')
        PsiFile multiLineFile = mock(PsiFile.class);
        when(multiLineFile.textToCharArray()).thenReturn("foo\nbar".toCharArray());
        PsiElement expectedElement = mock(PsiElement.class);
        when(multiLineFile.findElementAt(5)).thenReturn(expectedElement);

        Map<String, PsiFile> fileNamesToPsiFiles = new HashMap<>();
        fileNamesToPsiFiles.put("multiLine", multiLineFile);

        Issue issue = new Issue("multiLine", 2, 2, "aMessage", SeverityLevel.Error, "com.checkstyle.rules.aCheck");
        ProcessResultsThread underTest = underTest(fileNamesToPsiFiles, singletonList(issue));
        underTest.run();

        Problem expectedProblem = new Problem(expectedElement, "aMessage", SeverityLevel.Error, 2, 2,
                "com.checkstyle.rules.aCheck", false, false);
        assertThat(underTest.getProblems(), hasEntry(multiLineFile, singletonList(expectedProblem)));
    }

    @Test
    public void lineZeroIssuePointsToFileStart() {
        PsiFile psiFile = mock(PsiFile.class);
        when(psiFile.textToCharArray()).thenReturn("import boo.*;".toCharArray());
        PsiElement fileElement = mock(PsiElement.class);
        when(psiFile.findElementAt(0)).thenReturn(fileElement);

        Map<String, PsiFile> fileNamesToPsiFiles = new HashMap<>();
        fileNamesToPsiFiles.put("aFileName", psiFile);

        Issue issue = new Issue("aFileName", 0, 3, "aMessage", SeverityLevel.Error, "com.checkstyle.rules.aCheck");
        ProcessResultsThread underTest = underTest(fileNamesToPsiFiles, singletonList(issue));
        underTest.run();

        Problem expectedProblem = new Problem(fileElement, "aMessage", SeverityLevel.Error, 0, 3,
                "com.checkstyle.rules.aCheck", false, false);
        assertThat(underTest.getProblems(), hasEntry(psiFile, singletonList(expectedProblem)));
    }

    private ProcessResultsThread underTest(final Map<String, PsiFile> fileNamesToPsiFiles, final List<Issue> events) {
        return new ProcessResultsThread(
                false, Collections.emptyList(), 4, Optional.empty(), events, fileNamesToPsiFiles);
    }

    private PsiElement psiElement = mock(PsiElement.class);

    private PsiFile aPsiFile() {
        PsiFile expectedFile = mock(PsiFile.class);
        when(expectedFile.textToCharArray()).thenReturn("import boo.*;".toCharArray());
        when(expectedFile.findElementAt(6)).thenReturn(psiElement);
        return expectedFile;
    }

    private Issue anIssueFor(final String aFileName) {
        return new Issue(aFileName, 1, 7, "aMessage", SeverityLevel.Error, "com.checkstyle.rules.aCheck");
    }

    private Problem aProblemFor(final PsiFile expectedFile) {
        return new Problem(psiElement, "aMessage", SeverityLevel.Error, 1, 7, "com.checkstyle.rules.aCheck", false, false);
    }

}
