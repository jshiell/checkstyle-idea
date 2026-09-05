package org.infernus.idea.checkstyle;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.checker.ResultHandling;
import org.infernus.idea.checkstyle.checker.ScannerListener;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ScanResult;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StaticScannerTest extends LightPlatformTestCase {

    private StaticScanner underTest;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        underTest = getProject().getService(StaticScanner.class);
    }

    /**
     * Regression test: a non-null empty file list used to return before a scan was ever dispatched,
     * so no listener was ever registered or fired, and nothing cleared a caller's progress text.
     * {@link org.infernus.idea.checkstyle.checker.ScanFiles} already handles an empty input
     * correctly on its own (an empty result, reported via its listener), so the fix is simply to
     * let an empty list flow through rather than special-casing it here.
     * <p>
     * Dispatch is observed via a latch fired from {@code scanStarting}, registered before the scan
     * is submitted, rather than by polling {@link StaticScanner#isScanInProgress()}: an empty-list
     * scan can complete on its pooled thread before this thread's next statement runs, so polling
     * the flag immediately after dispatch is inherently racy and cannot reliably distinguish "never
     * dispatched" from "dispatched and already finished".
     */
    public void testAsyncScanFilesWithAnEmptyListStillDispatchesAndCompletesAScan() throws InterruptedException {
        final CountDownLatch dispatched = new CountDownLatch(1);
        final CountDownLatch completed = new CountDownLatch(1);

        underTest.asyncScanFiles(List.of(), null, ResultHandling.REPLACE, new ScannerListener() {
            @Override
            public void scanStarting(final List<PsiFile> filesToScan) {
                dispatched.countDown();
            }

            @Override
            public void filesScanned(final int count) {
            }

            @Override
            public void scanCompletedSuccessfully(final List<ScanResult> scanResults) {
                completed.countDown();
            }

            @Override
            public void scanFailedWithError(final CheckStylePluginException error) {
                completed.countDown();
            }
        });

        assertTrue("an empty file list should still dispatch a scan", dispatched.await(5, TimeUnit.SECONDS));
        assertTrue("the dispatched scan should complete rather than hang", completed.await(5, TimeUnit.SECONDS));
    }
}
