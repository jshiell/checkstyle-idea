package org.infernus.idea.checkstyle.importer;

import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImporter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;

import java.io.InputStream;

/**
 * Imports code style settings from check style configuration file.
 */
public class CheckStyleCodeStyleImporter implements SchemeImporter<CodeStyleScheme> {
    private static final Log LOG = LogFactory.getLog(CheckStyleCodeStyleImporter.class);

    @NotNull
    @Override
    public String[] getSourceExtensions() {
        return new String[]{"xml"};
    }

    @Nullable
    @Override
    public CodeStyleScheme importScheme(@NotNull final Project project,
                                        @NotNull final VirtualFile selectedFile,
                                        @NotNull final CodeStyleScheme currentScheme,
                                        @NotNull final SchemeFactory<CodeStyleScheme> schemeFactory) throws SchemeImportException {
        try {
            CodeStyleScheme targetScheme = currentScheme;
            if (currentScheme.isDefault()) {
                targetScheme = schemeFactory.createNewScheme(currentScheme.getName());
            }
            Configuration configuration = loadConfiguration(selectedFile);
            if (configuration != null) {
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
    private Configuration loadConfiguration(@NotNull VirtualFile selectedFile) throws Exception {
        InputStream inputStream = null;
        try {
            inputStream = selectedFile.getInputStream();
            InputSource inputSource = new InputSource(inputStream);
            return ConfigurationLoader.loadConfiguration(inputSource, name -> "", false);
        } finally {
            if (inputStream != null) { //noinspection ThrowFromFinallyBlock
                inputStream.close();
            }
        }
    }

    static void importConfiguration(@NotNull Configuration configuration, @NotNull CodeStyleSettings settings)
            throws IllegalAccessException, InstantiationException {
        ModuleImporter moduleImporter =
                ModuleImporterFactory.getModuleImporter(configuration);
        if (moduleImporter != null) {
            moduleImporter.importTo(settings);
        }
        for (Configuration childConfig : configuration.getChildren()) {
            importConfiguration(childConfig, settings);
        }
    }
}
