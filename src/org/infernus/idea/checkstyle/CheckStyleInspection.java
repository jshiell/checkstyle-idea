package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.ui.CheckStyleInspectionPanel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
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

            final Map<String, String> checkstyleProperties
                    = checkStylePlugin.getConfiguration().getDefinedProperies();

            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);
            final ClassLoader moduleClassLoader
                    = checkStylePlugin.buildModuleClassLoader(module);

            String configFile = checkStylePlugin.getConfiguration().getProperty(
                    CheckStyleConfiguration.CONFIG_FILE);
            if (configFile == null) {
                LOG.info("Loading default configuration");

                final InputStream in = CheckStyleInspection.class.getResourceAsStream(
                        CheckStyleConfiguration.DEFAULT_CONFIG);
                checker = CheckerFactory.getInstance().getChecker(in, 
                        moduleClassLoader, checkstyleProperties);
                in.close();

            } else {
                configFile = checkStylePlugin.untokenisePath(configFile);

                LOG.info("Loading configuration from " + configFile);
                checker = CheckerFactory.getInstance().getChecker(
                        new File(configFile), moduleClassLoader,
                        checkstyleProperties, false);
            }

            return checker;

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

        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePlugin.processError(
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
