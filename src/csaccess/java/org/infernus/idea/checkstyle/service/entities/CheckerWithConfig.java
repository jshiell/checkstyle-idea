package org.infernus.idea.checkstyle.service.entities;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CheckerWithConfig implements HasChecker, HasCsConfig {

    private final Checker checker;
    private final Lock lock = new ReentrantLock();
    private final Configuration configuration;

    public CheckerWithConfig(final Checker checker, final Configuration configuration) {
        this.checker = checker;
        this.configuration = configuration;
    }

    @Override
    public Checker getChecker() {
        return checker;
    }

    @Override
    public Lock getCheckerLock() {
        return lock;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
