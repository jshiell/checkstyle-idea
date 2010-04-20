package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checker.CheckStyleAuditListener;
import org.infernus.idea.checkstyle.checker.CheckerFactory;
import org.infernus.idea.checkstyle.checks.Check;
import org.infernus.idea.checkstyle.checks.CheckFactory;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.ui.CheckStyleInspectionPanel;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Inspection for CheckStyle integration for IntelliJ IDEA.
 *
 * @author James Shiell
 * @version 1.1
 */
public class CheckStyleInspection extends LocalInspectionTool {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            CheckStyleInspection.class);

    /**
     * The configuration panel.
     */
    private final CheckStyleInspectionPanel configPanel
            = new CheckStyleInspectionPanel();

    /**
     * Produce a CheckStyle checker.
     *
     * @param checkStylePlugin the plugin.
     * @param project          the currently open project.
     * @param module           the current module. May be null.
     * @return a checker.
     */
    private Checker getChecker(final CheckStylePlugin checkStylePlugin,
                               final Project project,
                               final Module module) {
        LOG.debug("Getting CheckStyle checker for inspection.");

        try {
            final ConfigurationLocation configurationLocation = getConfigurationLocation(module, checkStylePlugin);

            final ClassLoader moduleClassLoader = checkStylePlugin.buildModuleClassLoader(module);

            LOG.info("Loading configuration from " + configurationLocation);
            return CheckerFactory.getInstance().getChecker(configurationLocation, module, moduleClassLoader);

        } catch (Exception e) {
            LOG.error("Checker could not be created.", e);
            throw new CheckStylePluginException("Couldn't create Checker", e);
        }
    }

    private CheckStylePlugin getPlugin(final Project project) {
        final CheckStylePlugin checkStylePlugin = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }
        return checkStylePlugin;
    }

    private ConfigurationLocation getConfigurationLocation(final Module module, final CheckStylePlugin checkStylePlugin) {
        final ConfigurationLocation configurationLocation;
        if (module != null) {
            final CheckStyleModulePlugin checkStyleModulePlugin = module.getComponent(CheckStyleModulePlugin.class);
            if (checkStyleModulePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle module plugin");
            }
            configurationLocation = checkStyleModulePlugin.getConfiguration().getActiveConfiguration();

        } else {
            configurationLocation = checkStylePlugin.getConfiguration().getActiveConfiguration();
        }
        return configurationLocation;
    }

    /**
     * Retrieve a CheckStyle configuration.
     *
     * @param checkStylePlugin the plugin.
     * @param module           the current module. May be null.
     * @return a checkstyle configuration.
     */
    private Configuration getConfig(final CheckStylePlugin checkStylePlugin,
                                    final Module module) {
        LOG.debug("Getting CheckStyle checker for inspection.");

        try {
            final ConfigurationLocation configurationLocation = getConfigurationLocation(module, checkStylePlugin);

            LOG.info("Loading configuration from " + configurationLocation);
            return CheckerFactory.getInstance().getConfig(configurationLocation);

        } catch (Exception e) {
            LOG.error("Checker could not be created.", e);
            throw new CheckStylePluginException("Couldn't create Checker", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public JComponent createOptionsPanel() {
        return configPanel;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getGroupDisplayName() {
        return IDEAUtilities.getResource("plugin.group", "CheckStyle");
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getDisplayName() {
        return IDEAUtilities.getResource("plugin.display-name",
                "Real-time scan");
    }

    @Pattern("[a-zA-Z_0-9.]+")
    @NotNull
    @Override
    public String getID() {
        return CheckStyleConstants.ID_INSPECTION;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @NonNls
    public String getShortName() {
        return CheckStyleConstants.ID_PLUGIN;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile psiFile,
                                         @NotNull final InspectionManager manager,
                                         final boolean isOnTheFly) {
        LOG.debug("Inspection has been invoked.");

        if (!psiFile.isValid() || !psiFile.isPhysical()
                || !CheckStyleUtilities.isValidFileType(psiFile.getFileType())) {
            LOG.debug("Skipping file as invalid: " + psiFile.getName());
            return null;
        }

        final CheckStylePlugin checkStylePlugin = getPlugin(manager.getProject());

        final Module module = ModuleUtil.findModuleForPsiElement(psiFile);

        final boolean checkTestClasses = checkStylePlugin.getConfiguration().isScanningTestClasses();
        if (!checkTestClasses && module != null) {
            final VirtualFile elementFile = psiFile.getContainingFile().getVirtualFile();
            if (elementFile != null) {
                final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                if (moduleRootManager != null && moduleRootManager.getFileIndex() != null
                        && moduleRootManager.getFileIndex().isInTestSourceContent(elementFile)) {
                    LOG.debug("Skipping test class " + psiFile.getName());
                    return null;
                }
            }
        }

        File tempFile = null;
        try {
            final Checker checker = getChecker(checkStylePlugin, manager.getProject(), module);
            final Configuration config = getConfig(checkStylePlugin, module);
            final List<Check> checks = CheckFactory.getChecks(config);

            // we need to copy to a file as IntelliJ may not have saved the
            // file recently (or the file may even be being edited at this moment)
            final Document fileDocument = PsiDocumentManager.getInstance(
                    manager.getProject()).getDocument(psiFile);
            if (fileDocument == null) {
                LOG.debug("Skipping check - file is binary or has no document: "
                        + psiFile.getName());
                return null;
            }

            final CodeStyleSettings codeStyleSettings
                    = CodeStyleSettingsManager.getSettings(psiFile.getProject());

            tempFile = File.createTempFile(CheckStyleConstants.TEMPFILE_NAME,
                    CheckStyleConstants.TEMPFILE_EXTENSION);
            final BufferedWriter tempFileOut = new BufferedWriter(
                    new FileWriter(tempFile));
            for (final char character : psiFile.getText().toCharArray()) {
                if (character == '\n') { // IDEA uses \n internally
                    tempFileOut.write(codeStyleSettings.getLineSeparator());
                } else {
                    tempFileOut.write(character);
                }
            }
            tempFileOut.flush();
            tempFileOut.close();

            final Map<String, PsiFile> filesToScan = Collections.singletonMap(tempFile.getAbsolutePath(), psiFile);

            final CheckStyleAuditListener listener
                    = new CheckStyleAuditListener(filesToScan, manager, false, checks);
            checker.addListener(listener);
            checker.process(Arrays.asList(tempFile));

            final List<ProblemDescriptor> problems = listener.getProblems(psiFile);
            return problems.toArray(new ProblemDescriptor[problems.size()]);

        } catch (ProcessCanceledException e) {
            LOG.warn("Process cancelled when scanning: " + psiFile.getName());
            return null;

        } catch (Throwable e) {
            final CheckStylePluginException processed = CheckStylePlugin.processError(
                    "The inspection could not be executed.", e);
            LOG.error("The inspection could not be executed.", processed);

            return null;

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
