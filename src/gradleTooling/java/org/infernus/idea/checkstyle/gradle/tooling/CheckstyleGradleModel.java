package org.infernus.idea.checkstyle.gradle.tooling;

import java.io.Serializable;
import java.util.Map;

/**
 * What a single Gradle module's {@code checkstyle {}} configuration looked like at sync time, as seen
 * by {@link CheckstyleGradleModelBuilder}. {@code null} fields mean "nothing usable was found", not
 * "an error occurred" — see {@link CheckstyleGradleModelBuilder} for exactly when each field is null.
 */
public interface CheckstyleGradleModel extends Serializable {

    String getConfigFile();

    Map<String, String> getConfigProperties();

    String getToolVersion();
}
