package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;

import java.util.List;
import java.util.Map;

public interface ScannerListener {

    void scanStarting(final List<PsiFile> filesToScan);

    void filesScanned(int count);

    void scanComplete(final ConfigurationLocationResult configurationLocationResult,
                      final Map<PsiFile, List<Problem>> scanResults);

    void errorCaught(final CheckStylePluginException error);

}
