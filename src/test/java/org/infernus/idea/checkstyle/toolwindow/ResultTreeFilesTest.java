package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.psi.PsiFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ResultTreeFilesTest {

    @Mock private PsiFile fileA;
    @Mock private PsiFile fileB;

    private ToggleableTreeNode problemNodeFor(final PsiFile file) {
        // mocked to sidestep the icon lookups in the real constructor
        final ProblemResultTreeInfo problemInfo = mock(ProblemResultTreeInfo.class);
        when(problemInfo.getFile()).thenReturn(file);
        return new ToggleableTreeNode(problemInfo);
    }

    private ToggleableTreeNode groupNode(final ToggleableTreeNode... children) {
        return nodeWith(new PackageGroupTreeInfo("a group", children.length), children);
    }

    private ToggleableTreeNode fileNode(final PsiFile file) {
        return nodeWith(new FileGroupTreeInfo("a file", 1), problemNodeFor(file));
    }

    private ToggleableTreeNode nodeWith(final Object userObject, final ToggleableTreeNode... children) {
        final ToggleableTreeNode node = new ToggleableTreeNode(userObject);
        for (ToggleableTreeNode child : children) {
            node.add(child);
        }
        return node;
    }

    @Test
    void aProblemNodeYieldsItsOwnFile() {
        assertThat(ResultTreeFiles.filesUnder(problemNodeFor(fileA)), contains(fileA));
    }

    @Test
    void aGroupNodeYieldsTheFilesOfItsChildren() {
        ToggleableTreeNode group = groupNode(problemNodeFor(fileA), problemNodeFor(fileB));

        assertThat(ResultTreeFiles.filesUnder(group), containsInAnyOrder(fileA, fileB));
    }

    @Test
    void nestedGroupsAreWalked() {
        ToggleableTreeNode root = groupNode(groupNode(groupNode(problemNodeFor(fileA))));

        assertThat(ResultTreeFiles.filesUnder(root), contains(fileA));
    }

    @Test
    void aFileAppearingInSeveralProblemsIsYieldedOnce() {
        ToggleableTreeNode group = groupNode(problemNodeFor(fileA), problemNodeFor(fileA));

        assertThat(ResultTreeFiles.filesUnder(group), contains(fileA));
    }

    @Test
    void filteredOutChildrenAreStillYielded() {
        ToggleableTreeNode hidden = problemNodeFor(fileB);
        hidden.setVisible(false);
        ToggleableTreeNode group = groupNode(problemNodeFor(fileA), hidden);

        assertThat(ResultTreeFiles.filesUnder(group), containsInAnyOrder(fileA, fileB));
    }

    @Test
    void aGroupWithNoChildrenYieldsNoFiles() {
        assertThat(ResultTreeFiles.filesUnder(groupNode()), is(empty()));
    }

    @Test
    void aNodeWithNoUserObjectYieldsNoFiles() {
        assertThat(ResultTreeFiles.filesUnder(new ToggleableTreeNode()), is(empty()));
    }

    @Test
    void nothingIsYieldedForNoNode() {
        assertThat(ResultTreeFiles.filesUnder(null), is(empty()));
    }

    // --- locating the nodes for a set of files ---

    @Test
    void theNodeForARequestedFileIsFound() {
        ToggleableTreeNode fileNode = fileNode(fileA);
        ToggleableTreeNode root = groupNode(fileNode);

        assertThat(ResultTreeFiles.fileNodesShowing(root, Set.of(fileA)), contains(fileNode));
    }

    @Test
    void theNodeForAnotherFileIsNotFound() {
        ToggleableTreeNode root = groupNode(fileNode(fileB));

        assertThat(ResultTreeFiles.fileNodesShowing(root, Set.of(fileA)), is(empty()));
    }

    @Test
    void nodesAreFoundHoweverDeeplyTheTreeIsGrouped() {
        ToggleableTreeNode fileNode = fileNode(fileA);
        ToggleableTreeNode root = groupNode(groupNode(groupNode(fileNode)));

        assertThat(ResultTreeFiles.fileNodesShowing(root, Set.of(fileA)), contains(fileNode));
    }

    @Test
    void aFileAppearingUnderSeveralGroupsIsFoundInEachOfThem() {
        ToggleableTreeNode root = groupNode(groupNode(fileNode(fileA)), groupNode(fileNode(fileA)));

        assertThat(ResultTreeFiles.fileNodesShowing(root, Set.of(fileA)), hasSize(2));
    }

    @Test
    void noNodesAreFoundForNoFiles() {
        ToggleableTreeNode root = groupNode(fileNode(fileA));

        assertThat(ResultTreeFiles.fileNodesShowing(root, Set.of()), is(empty()));
    }
}
