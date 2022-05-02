package org.infernus.idea.checkstyle.build;

import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;


/**
 * Listener that reports on test suite statistics after completion of the test suite.
 */
class TestSuiteStatsReporter
    implements TestListener {
    private final Logger logger;


    public TestSuiteStatsReporter(final Logger pLogger) {
        logger = pLogger;
    }


    @Override
    public void afterSuite(final TestDescriptor pSuite, final TestResult pTestResult) {
        if (pSuite.getParent() == null) {
            logger.lifecycle("\r\t" + pTestResult.getTestCount() + " tests executed, " + pTestResult.getSuccessfulTestCount() + " successful, " //
                    + (pTestResult.getTestCount() - pTestResult.getSuccessfulTestCount() - pTestResult.getSkippedTestCount()) + " failed, " //
                    + pTestResult.getSkippedTestCount() + " skipped.");
        }
    }


    @Override
    public void beforeSuite(final TestDescriptor pSuite) {
        // do nothing
    }

    @Override
    public void afterTest(final TestDescriptor pTestDescriptor, final TestResult pTestResult) {
        // do nothing
    }

    @Override
    public void beforeTest(final TestDescriptor pTestDescriptor) {
        // do nothing
    }
}
