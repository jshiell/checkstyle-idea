package org.infernus.idea.checkstyle.importer;

import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.csapi.ConfigVisitor;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports code style settings from check style configuration file.
 * Registered as {@code schemeImporter} in <em>plugin.xml</em>.
 */
public class CheckStyleCodeStyleImporter
        implements SchemeImporter<CodeStyleScheme>
{
    private static final Log LOG = LogFactory.getLog(CheckStyleCodeStyleImporter.class);

    private CheckstyleProjectService checkstyleProjectService = null;

    public CheckStyleCodeStyleImporter() {
        super();
    }

    public CheckStyleCodeStyleImporter(@NotNull final CheckstyleProjectService pCheckstyleProjectService) {
        super();
        checkstyleProjectService = pCheckstyleProjectService;
    }


    @NotNull
    @Override
    public String[] getSourceExtensions() {
        return new String[]{"xml"};
    }

    @Nullable
    @Override
    public CodeStyleScheme importScheme(@NotNull final Project project, @NotNull final VirtualFile selectedFile,
                                        @NotNull final CodeStyleScheme currentScheme, @NotNull final
                                            SchemeFactory<CodeStyleScheme> schemeFactory) throws SchemeImportException {
        try {
            CodeStyleScheme targetScheme = currentScheme;
            if (currentScheme.isDefault()) {
                targetScheme = schemeFactory.createNewScheme(currentScheme.getName());
            }
            CheckstyleInternalObject configuration = loadConfiguration(project, selectedFile);
            if (configuration != null) {
                checkstyleProjectService = CheckstyleProjectService.getInstance(project);
                importConfiguration(configuration, targetScheme.getCodeStyleSettings());
                return targetScheme;
            }
        } catch (Exception e) {
            LOG.error("Failed to import style", e);
            throw new SchemeImportException(e);
        }
        return null;
    }


    @Nullable
    @Override
    public String getAdditionalImportInfo(@NotNull final CodeStyleScheme scheme) {
        return null;
    }


    @Nullable
    private CheckstyleInternalObject loadConfiguration(@NotNull final Project project, @NotNull final VirtualFile
            selectedFile) {
        CheckstyleProjectService csService = CheckstyleProjectService.getInstance(project);
        return csService.getCheckstyleInstance().loadConfiguration(selectedFile, true, null);
    }


    void importConfiguration(@NotNull final CheckstyleInternalObject configuration,
                             @NotNull final CodeStyleSettings settings) {

        checkstyleProjectService.getCheckstyleInstance().peruseConfiguration(configuration, new ConfigVisitor()
        {
            @Override
            public void visit(@NotNull final ConfigurationModule pModule) {
                ModuleImporter moduleImporter = null;
                try {
                    moduleImporter = ModuleImporterFactory.getModuleImporter(pModule);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new CheckStylePluginException("error creating module importer", e);
                }
                if (moduleImporter != null) {
                    moduleImporter.importTo(settings);
                }
            }
        });
    }
}
