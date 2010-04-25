package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Provider for a CheckStyle integration inspection for IntelliJ IDEA.
 *
 * @author James Shiell
 * @version 1.0
 */
public class CheckStyleProvider implements InspectionToolProvider, ApplicationComponent {

    /**
     * {@inheritDoc}
     */
    public Class[] getInspectionClasses() {
        return new Class[] {CheckStyleInspection.class};
    }

    /**
     * {@inheritDoc}
     */
    @NonNls
    @NotNull
    public String getComponentName() {
        return "CheckStyleProvider";
    }

    /**
     * {@inheritDoc}
     */
    public void initComponent() {
        // no action required
    }

    /**
     * {@inheritDoc}
     */
    public void disposeComponent() {
        // no action required
    }

}
