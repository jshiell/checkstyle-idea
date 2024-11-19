package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationResult;
import org.infernus.idea.checkstyle.checker.Problem;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ScanResult(ConfigurationLocationResult configurationLocationResult,
                         Module module,
                         Map<PsiFile, List<Problem>> problems) {

    public static final ScanResult EMPTY = new ScanResult(null, null, Collections.emptyMap());
}
