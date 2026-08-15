package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.Problem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Merges the results of a re-scan into the results already on display.
 */
public final class ScanResultMerger {

    private ScanResultMerger() {
    }

    /**
     * Merge the results of a re-scan into the results it supersedes.
     * <p>
     * Every file the latest scan is authoritative for is stripped from all of the previous results,
     * so that a file never shows a mixture of old and new findings, and the latest results are then
     * appended. Previous results left with nothing to show are dropped, bounding the growth of the
     * list over repeated merges.
     *
     * @param previous the results currently on display.
     * @param latest   the results of the re-scan.
     * @return the merged results.
     */
    @NotNull
    public static List<ScanResult> merge(@NotNull final List<ScanResult> previous,
                                         @NotNull final List<ScanResult> latest) {
        final Set<VirtualFile> rescanned = filesRescannedBy(latest);

        final List<ScanResult> merged = new ArrayList<>();
        for (final ScanResult previousResult : previous) {
            final Map<PsiFile, List<Problem>> retained = problemsSurviving(previousResult, rescanned);
            if (!retained.isEmpty()) {
                // the retained result is no longer authoritative for anything: it is only ever a source
                // of problems to display, never of files to strip on a subsequent merge.
                merged.add(new ScanResult(previousResult.configurationLocationResult(),
                        previousResult.module(),
                        retained,
                        Set.of()));
            }
        }
        merged.addAll(latest);
        return merged;
    }

    private static Set<VirtualFile> filesRescannedBy(final List<ScanResult> latest) {
        return latest.stream()
                .flatMap(result -> result.scannedFiles().stream())
                .map(PsiFile::getVirtualFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Map<PsiFile, List<Problem>> problemsSurviving(final ScanResult previousResult,
                                                                 final Set<VirtualFile> rescanned) {
        final Map<PsiFile, List<Problem>> retained = new HashMap<>();
        previousResult.problems().forEach((file, problems) -> {
            // an empty entry would still be counted as a file by the result tree, so never keep one
            if (!problems.isEmpty() && !wasRescanned(file, rescanned)) {
                retained.put(file, problems);
            }
        });
        return retained;
    }

    private static boolean wasRescanned(final PsiFile file, final Set<VirtualFile> rescanned) {
        // PSI may hand back a different PsiFile instance for the same file after an edit, so identity
        // of the PsiFile cannot be trusted; a file without a virtual file can never match.
        final VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile != null && rescanned.contains(virtualFile);
    }
}
