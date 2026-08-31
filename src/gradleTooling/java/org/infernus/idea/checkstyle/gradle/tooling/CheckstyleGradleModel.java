package org.infernus.idea.checkstyle.gradle.tooling;

import java.io.Serializable;

/**
 * Spike (Increment 0): proves the ModelBuilderService round-trip and the typed-access question. Will
 * be replaced by the real model in Increment 3.
 */
public interface CheckstyleGradleModel extends Serializable {

    String getConfigFilePath();
}
