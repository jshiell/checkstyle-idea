package org.infernus.idea.checkstyle;

import com.intellij.testFramework.LightPlatformTestCase;

import java.util.List;

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
     */
    public void testAsyncScanFilesWithAnEmptyListStillDispatchesAndCompletesAScan() throws InterruptedException {
        underTest.asyncScanFiles(List.of(), null);

        assertTrue("an empty file list should still dispatch a scan", underTest.isScanInProgress());

        final long deadline = System.currentTimeMillis() + 5000;
        while (underTest.isScanInProgress() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertFalse("the dispatched scan should complete rather than hang", underTest.isScanInProgress());
    }
}
