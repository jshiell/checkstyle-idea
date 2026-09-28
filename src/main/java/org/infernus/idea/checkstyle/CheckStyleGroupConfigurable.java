package org.infernus.idea.checkstyle;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;

import javax.swing.JComponent;

/**
 * The "Checkstyle" parent node in the Settings tree, grouping the project- and application-scoped
 * Checkstyle pages together. Has no content of its own - {@link CheckStyleConfigurable} and
 * {@link CheckStyleApplicationConfigurable} nest under it via {@code parentId} in {@code plugin.xml}.
 */
public class CheckStyleGroupConfigurable implements Configurable {

    @Nls
    @Override
    public String getDisplayName() {
        return CheckStyleBundle.message("plugin.configuration-name");
    }

    @Override
    public JComponent createComponent() {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() {
        // no content of its own
    }
}
