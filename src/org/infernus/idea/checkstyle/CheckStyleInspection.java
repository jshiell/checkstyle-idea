package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.util.List;
import java.util.ResourceBundle;

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
     * @param project the currently open project.
     * @param psiFile the psiFile being scanned.
     * @return a checker.
     */
    public Checker getChecker(final Project project,
                              final PsiFile psiFile) {
        LOG.debug("Getting CheckStyle checker for inspection.");

        try {
            final Checker checker;

            final CheckStylePlugin checkStylePlugin
                    = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException(
                        "Couldn't get checkstyle plugin");
            }

            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);
            final ClassLoader moduleClassLoader
                    = checkStylePlugin.buildModuleClassLoader(module);

            String configFile = checkStylePlugin.getConfiguration().getProperty(
                    CheckStyleConfiguration.CONFIG_FILE);
            if (configFile == null) {
                LOG.info("Loading default configuration");

                final InputStream in = CheckStyleInspection.class.getResourceAsStream(
                        CheckStyleConfiguration.DEFAULT_CONFIG);
                checker = CheckerFactory.getInstance().getChecker(in, null);
                in.close();

            } else {
                configFile = checkStylePlugin.processConfigFilePath(configFile);

                LOG.info("Loading configuration from " + configFile);
                checker = CheckerFactory.getInstance().getChecker(
                        new File(configFile), moduleClassLoader, false);
            }

            return checker;

        } catch (Exception e) {
            LOG.error("Error", e);
            throw new RuntimeException("Couldn't create Checker", e);
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
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        return resources.getString("plugin.group");
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getDisplayName() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        return resources.getString("plugin.display-name");
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

        File tempFile = null;
        try {
            final Checker checker = getChecker(manager.getProject(), psiFile);

            // we need to copy to a file as IntelliJ may not have saved the
            // file recently...
            tempFile = File.createTempFile(CheckStyleConstants.TEMPFILE_NAME,
                    CheckStyleConstants.TEMPFILE_EXTENSION);
            final BufferedWriter tempFileOut = new BufferedWriter(
                    new FileWriter(tempFile));
            tempFileOut.write(psiFile.getText());
            tempFileOut.flush();
            tempFileOut.close();

            final CheckStyleAuditListener listener
                    = new CheckStyleAuditListener(psiFile, manager);
            checker.addListener(listener);
            checker.process(new File[] {tempFile});
            checker.destroy();

            final List<ProblemDescriptor> problems = listener.getProblems();
            return problems.toArray(new ProblemDescriptor[problems.size()]);

        } catch (IOException e) {
            LOG.error("Failure when creating temp file", e);
            throw new RuntimeException("Couldn't create temp file", e);

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

}
