package org.infernus.idea.checkstyle;

import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;

public class CheckstyleModuleConfigurationEditorProvider implements ModuleConfigurationEditorProvider {
    @Override
    public ModuleConfigurationEditor[] createEditors(final ModuleConfigurationState state) {
        return new ModuleConfigurationEditor[] {new CheckStyleModuleConfigurationEditor(state.getCurrentRootModel().getModule())};
    }
}
