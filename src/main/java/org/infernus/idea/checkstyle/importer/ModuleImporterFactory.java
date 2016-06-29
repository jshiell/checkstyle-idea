package org.infernus.idea.checkstyle.importer;

import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ModuleImporterFactory {

    private ModuleImporterFactory() {}

    @Nullable
    static ModuleImporter getModuleImporter(@NotNull Configuration configuration)
            throws InstantiationException, IllegalAccessException {
        String name = configuration.getName();
        ModuleImporter moduleImporter = createImporter(name);
        if (moduleImporter != null) {
            moduleImporter.setFrom(configuration);
        }
        return moduleImporter;
    }

    @Nullable
    private static ModuleImporter createImporter(@NotNull String name)
            throws IllegalAccessException, InstantiationException {
        String fqn = getFullyQualifiedClassName(name);
        try {
            Class c = Class.forName(fqn);
            Object o = c.newInstance();
            return o instanceof ModuleImporter ? (ModuleImporter) o : null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static String getFullyQualifiedClassName(@NotNull String moduleName) {
        return ModuleImporterFactory.class.getPackage().getName() + ".modules." + moduleName + "Importer";
    }
}
