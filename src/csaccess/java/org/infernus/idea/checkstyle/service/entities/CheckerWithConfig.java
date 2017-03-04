package org.infernus.idea.checkstyle.service.entities;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.Configuration;


public class CheckerWithConfig implements HasChecker, HasCsConfig {

    private final Checker checker;
    private final Configuration configuration;

    public CheckerWithConfig(final Checker checker, final Configuration configuration) {
        this.checker = checker;
        this.configuration = configuration;
    }

    public Checker getChecker() {
        return checker;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
