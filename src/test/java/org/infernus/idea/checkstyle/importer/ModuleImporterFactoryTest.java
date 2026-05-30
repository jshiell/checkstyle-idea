package org.infernus.idea.checkstyle.importer;

import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleImporterFactoryTest {

    @Test
    void knownModuleReturnsModuleImporter() throws Exception {
        ConfigurationModule module = moduleNamed("LineLength");
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer, notNullValue());
    }

    @Test
    void knownModuleImporterIsSetFromConfiguration() throws Exception {
        ConfigurationModule module = moduleNamed("IndentationImporter");
        // "IndentationImporter" won't match (factory appends "Importer"), so use bare name
        ConfigurationModule indentation = moduleNamed("Indentation");
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(indentation);
        assertThat(importer, notNullValue());
        assertThat(importer, instanceOf(ModuleImporter.class));
    }

    @Test
    void unknownModuleReturnsNull() throws Exception {
        ConfigurationModule module = moduleNamed("NonExistentModuleXyz");
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertNull(importer);
    }

    @Test
    void allKnownModulesCanBeInstantiated() throws Exception {
        String[] knownModules = {
            "AvoidStarImport",
            "CustomImportOrder",
            "EmptyLineSeparator",
            "FileTabCharacter",
            "ImportOrder",
            "Indentation",
            "LeftCurly",
            "LineLength",
            "NeedBraces",
            "NoWhitespaceBefore",
            "WhitespaceAfter",
            "WhitespaceAround"
        };
        for (String moduleName : knownModules) {
            ConfigurationModule module = moduleNamed(moduleName);
            ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
            assertThat("Importer for " + moduleName + " should not be null", importer, notNullValue());
        }
    }

    private ConfigurationModule moduleNamed(final String name) {
        ConfigurationModule module = mock(ConfigurationModule.class);
        when(module.getName()).thenReturn(name);
        return module;
    }
}
