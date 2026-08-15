package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationResult;
import org.infernus.idea.checkstyle.checker.Problem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The outcome of scanning a set of files against a single rules file.
 *
 * @param configurationLocationResult the rules file the scan was run against, and its status.
 * @param module                      the module the scanned files belong to.
 * @param problems                    the problems found, keyed by the file they were found in. A file
 *                                    that yielded no problems has no entry.
 * @param scannedFiles                the files this result is authoritative for, whether or not they
 *                                    yielded problems. Files the scan failed to reach are excluded.
 */
public record ScanResult(ConfigurationLocationResult configurationLocationResult,
                         Module module,
                         Map<PsiFile, List<Problem>> problems,
                         Set<PsiFile> scannedFiles) {

    public static final ScanResult EMPTY = new ScanResult(null, null, Collections.emptyMap(), Set.of());
}
