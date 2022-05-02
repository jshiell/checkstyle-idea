package org.infernus.idea.checkstyle.importer;

import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

class ModuleImporterFactory {

    private ModuleImporterFactory() {
    }

    @Nullable
    static ModuleImporter getModuleImporter(@NotNull final ConfigurationModule configuration)
            throws InstantiationException, IllegalAccessException {
        String name = configuration.getName();
        ModuleImporter moduleImporter = createImporter(name);
        if (moduleImporter != null) {
            moduleImporter.setFrom(configuration);
        }
        return moduleImporter;
    }

    @Nullable
    private static ModuleImporter createImporter(@NotNull final String name)
            throws IllegalAccessException, InstantiationException {
        String fqn = getFullyQualifiedClassName(name);
        try {
            Class c = Class.forName(fqn);
            Object o = c.getDeclaredConstructor().newInstance();
            return o instanceof ModuleImporter ? (ModuleImporter) o : null;
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
            return null;
        }
    }

    private static String getFullyQualifiedClassName(@NotNull final String moduleName) {
        return ModuleImporterFactory.class.getPackage().getName() + ".modules." + moduleName + "Importer";
    }
}
