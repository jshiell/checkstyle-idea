package org.infernus.idea.checkstyle.config;

public interface ConfigurationListener {

    /**
     * Always invoked on the EDT, regardless of the thread that changed the configuration.
     */
    void configurationChanged();

}
