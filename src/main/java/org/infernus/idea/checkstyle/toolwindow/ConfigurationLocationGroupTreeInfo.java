package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.icons.AllIcons;

class ConfigurationLocationGroupTreeInfo extends GroupTreeInfo {

    /**
     * Construct a configuration location node.
     *
     * @param configurationLocationDescription the name of the configuration location.
     * @param problemCount                     the number of problems in the file.
     */
    ConfigurationLocationGroupTreeInfo(final String configurationLocationDescription, final int problemCount) {
        super(configurationLocationDescription, "configuration-location", AllIcons.FileTypes.Properties, problemCount);
    }

}
