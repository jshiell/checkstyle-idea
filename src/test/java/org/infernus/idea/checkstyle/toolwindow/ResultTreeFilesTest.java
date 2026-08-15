package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.psi.PsiFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        final ToggleableTreeNode group = new ToggleableTreeNode(new FileGroupTreeInfo("a group", children.length));
        for (ToggleableTreeNode child : children) {
            group.add(child);
        }
        return group;
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
}
