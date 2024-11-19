package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ScanResult;

import java.util.List;

public interface ScannerListener {

    void scanStarting(List<PsiFile> filesToScan);

    void filesScanned(int count);

    void scanCompletedSuccessfully(ScanResult scanResult);

    void scanFailedWithError(CheckStylePluginException error);

}
