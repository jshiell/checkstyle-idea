package org.infernus.idea.checkstyle.csapi;

import org.jetbrains.annotations.NotNull;


/**
 * Visitor called by the PeruseConfiguration command.
 */
public interface ConfigVisitor {

    void visit(@NotNull ConfigurationModule module);

}
