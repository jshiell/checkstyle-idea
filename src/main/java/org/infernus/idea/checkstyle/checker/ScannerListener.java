package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;

import java.util.List;
import java.util.Map;

public interface ScannerListener {

    void scanStarting(List<PsiFile> filesToScan);

    void filesScanned(int count);

    void scanComplete(ConfigurationLocationResult configurationLocationResult,
                      Map<PsiFile, List<Problem>> scanResults);

    void errorCaught(CheckStylePluginException error);

}
