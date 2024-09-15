package org.infernus.idea.checkstyle.importer;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Imports code style settings from check style configuration file.
 * Registered as {@code schemeImporter} in <em>plugin.xml</em>.
 */
public class CheckStyleCodeStyleImporter
        implements SchemeImporter<CodeStyleScheme> {

    private static final Logger LOG = Logger.getInstance(CheckStyleCodeStyleImporter.class);

    private CheckstyleProjectService overrideCheckstyleProjectService;

    public CheckStyleCodeStyleImporter() {
    }

    CheckStyleCodeStyleImporter(@NotNull final CheckstyleProjectService checkstyleProjectService) {
        this.overrideCheckstyleProjectService = checkstyleProjectService;
    }

    @NotNull
    @Override
    public String[] getSourceExtensions() {
        return new String[]{"xml"};
    }

    @Override
    public @Nullable CodeStyleScheme importScheme(@NotNull final Project project,
                                                  @NotNull final VirtualFile selectedFile,
                                                  @NotNull final CodeStyleScheme currentScheme,
                                                  @NotNull final SchemeFactory<? extends CodeStyleScheme> schemeFactory) throws SchemeImportException {
        try {
            CodeStyleScheme targetScheme = currentScheme;
            if (currentScheme.isDefault()) {
                targetScheme = schemeFactory.createNewScheme(currentScheme.getName());
            }
            CheckstyleInternalObject configuration = loadConfiguration(project, selectedFile);
            if (configuration != null) {
                importConfiguration(checkstyleProjectService(project), configuration, targetScheme.getCodeStyleSettings());
                return targetScheme;
            }
        } catch (Exception e) {
            LOG.warn("Failed to import style", e);
            throw new SchemeImportException(e);
        }
        return null;
    }

    private CheckstyleProjectService checkstyleProjectService(@NotNull final Project project) {
        if (overrideCheckstyleProjectService != null) {
            return overrideCheckstyleProjectService;
        }
        return project.getService(CheckstyleProjectService.class);
    }

    private ConfigurationLocationFactory configurationLocationFactory(@NotNull final Project project) {
        return project.getService(ConfigurationLocationFactory.class);
    }

    @Nullable
    @Override
    public String getAdditionalImportInfo(@NotNull final CodeStyleScheme scheme) {
        return null;
    }


    @Nullable
    private CheckstyleInternalObject loadConfiguration(@NotNull final Project project,
                                                       @NotNull final VirtualFile selectedFile) {
        return checkstyleProjectService(project)
                .getCheckstyleInstance()
                .loadConfiguration(selectedFile, defaultPropertiesForFile(project, selectedFile));
    }

    @NotNull
    private Map<String, String> defaultPropertiesForFile(@NotNull final Project project,
                                                         @NotNull final VirtualFile selectedFile) {
        try {
            ConfigurationLocation codeImportHolder = configurationLocationFactory(project).create(
                    project,
                    UUID.randomUUID().toString(),
                    ConfigurationType.LOCAL_FILE,
                    selectedFile.getPath(),
                    "Code Import Holder",
                    NamedScopeHelper.getDefaultScope(project));
            codeImportHolder.resolve(checkstyleProjectService(project).underlyingClassLoader());
            final Map<String, String> properties = codeImportHolder.getProperties();
            return properties.keySet().stream().
                    collect(Collectors.toMap(identity(), k -> properties.getOrDefault(k, "")));

        } catch (Exception e) {
            LOG.error("Unable to construct defaulted properties", e);
            return Collections.emptyMap();
        }
    }

    void importConfiguration(@NotNull final CheckstyleProjectService checkstyleProjectService,
                             @NotNull final CheckstyleInternalObject configuration,
                             @NotNull final CodeStyleSettings settings) {

        checkstyleProjectService.getCheckstyleInstance().peruseConfiguration(configuration, module -> {
            ModuleImporter moduleImporter;
            try {
                moduleImporter = ModuleImporterFactory.getModuleImporter(module);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new CheckStylePluginException("error creating module importer", e);
            }
            if (moduleImporter != null) {
                moduleImporter.importTo(settings);
            }
        });
    }
}
