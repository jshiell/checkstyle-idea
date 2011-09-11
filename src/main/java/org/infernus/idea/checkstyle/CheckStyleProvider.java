package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Provider for a CheckStyle integration inspection for IntelliJ IDEA.
 */
public class CheckStyleProvider implements InspectionToolProvider, ApplicationComponent {

    public Class[] getInspectionClasses() {
        return new Class[] {CheckStyleInspection.class};
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return "CheckStyleProvider";
    }

    public void initComponent() {
        // no action required
    }

    public void disposeComponent() {
        // no action required
    }

}
