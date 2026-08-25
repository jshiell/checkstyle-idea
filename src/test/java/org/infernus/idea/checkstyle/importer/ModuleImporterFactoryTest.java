package org.infernus.idea.checkstyle.importer;

import java.util.Map;

import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
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
            "DeclarationOrder",
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

    @Test
    void customImportOrderWithSamePackageEmitsWarning() throws Exception {
        ConfigurationModule module = moduleNamed("CustomImportOrder");
        when(module.getProperties()).thenReturn(Map.of("customImportOrderRules", "SAME_PACKAGE(2)###STATIC"));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(),
                hasItem(CheckStyleBundle.message("import.custom-order.same-package-unsupported")));
    }

    @Test
    void leftCurlyWithUnrecognizedOptionEmitsWarning() throws Exception {
        ConfigurationModule module = moduleNamed("LeftCurly");
        when(module.getProperties()).thenReturn(Map.of("option", "banana"));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(),
                hasItem("The LeftCurly rule's option value \"banana\" is not recognized and was not imported."));
    }

    @Test
    void separatorWrapWithSemiTokenEmitsWarning() throws Exception {
        ConfigurationModule module = moduleNamed("SeparatorWrap");
        when(module.getKnownTokenTypes()).thenReturn(java.util.Set.of(org.infernus.idea.checkstyle.csapi.KnownTokenTypes.SEMI));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(),
                hasItem("The SeparatorWrap rule's \"SEMI\" token has no IntelliJ equivalent and was not imported."));
    }

    @Test
    void separatorWrapWithNoTokensEmitsNoWarning() throws Exception {
        ConfigurationModule module = moduleNamed("SeparatorWrap");
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(), org.hamcrest.Matchers.empty());
    }

    @Test
    void importOrderWithUnknownPropertyEmitsWarning() throws Exception {
        ConfigurationModule module = moduleNamed("ImportOrder");
        when(module.getProperties()).thenReturn(Map.of("totallyBogusProperty", "x"));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(),
                hasItem(CheckStyleBundle.message("import.import-order.unsupported-property", "totallyBogusProperty")));
    }

    @Test
    void importOrderWithSeverityAndIdEmitsNoWarning() throws Exception {
        ConfigurationModule module = moduleNamed("ImportOrder");
        when(module.getProperties()).thenReturn(Map.of("severity", "warning", "id", "myId"));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(), org.hamcrest.Matchers.empty());
    }

    @Test
    void indentationWithUnknownPropertyEmitsWarning() throws Exception {
        ConfigurationModule module = moduleNamed("Indentation");
        when(module.getProperties()).thenReturn(Map.of("totallyBogusProperty", "x"));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(),
                hasItem(CheckStyleBundle.message("import.indentation.unsupported-property", "totallyBogusProperty")));
    }

    @Test
    void indentationWithSeverityAndIdEmitsNoWarning() throws Exception {
        ConfigurationModule module = moduleNamed("Indentation");
        when(module.getProperties()).thenReturn(Map.of("severity", "warning", "id", "myId"));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(), org.hamcrest.Matchers.empty());
    }

    @Test
    void avoidStarImportWithUnknownPropertyEmitsWarning() throws Exception {
        ConfigurationModule module = moduleNamed("AvoidStarImport");
        when(module.getProperties()).thenReturn(Map.of("totallyBogusProperty", "x"));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(),
                hasItem(CheckStyleBundle.message("import.avoid-star-import.unsupported-property", "totallyBogusProperty")));
    }

    @Test
    void avoidStarImportWithSeverityAndIdEmitsNoWarning() throws Exception {
        ConfigurationModule module = moduleNamed("AvoidStarImport");
        when(module.getProperties()).thenReturn(Map.of("severity", "warning", "id", "myId"));
        ModuleImporter importer = ModuleImporterFactory.getModuleImporter(module);
        assertThat(importer.getWarnings(), org.hamcrest.Matchers.empty());
    }

    private ConfigurationModule moduleNamed(final String name) {
        ConfigurationModule module = mock(ConfigurationModule.class);
        when(module.getName()).thenReturn(name);
        return module;
    }
}
