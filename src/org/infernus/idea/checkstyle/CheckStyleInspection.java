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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.ui.CheckStyleInspectionPanel;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.infernus.idea.checkstyle.checker.CheckerFactory;
import org.infernus.idea.checkstyle.checker.CheckStyleAuditListener;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

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

            final ConfigurationLocation configurationLocation
                    = checkStylePlugin.getConfiguration().getActiveConfiguration();

            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);
            final ClassLoader moduleClassLoader
                    = checkStylePlugin.buildModuleClassLoader(module);

            LOG.info("Loading configuration from " + configurationLocation);
            checker = CheckerFactory.getInstance().getChecker(configurationLocation, moduleClassLoader);

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

        final CheckStylePlugin checkStylePlugin
                = manager.getProject().getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException(
                    "Couldn't get checkstyle plugin");
        }

        final boolean checkTestClasses = checkStylePlugin.getConfiguration().isScanningTestClasses();
        if (!checkTestClasses) {
            final VirtualFile elementFile = psiFile.getContainingFile().getVirtualFile();
            if (elementFile != null) {
                final Module module = ModuleUtil.findModuleForFile(elementFile, manager.getProject());
                if (module != null) {
                    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                    if (moduleRootManager != null && moduleRootManager.getFileIndex() != null
                            && moduleRootManager.getFileIndex().isInTestSourceContent(elementFile)) {
                        LOG.debug("Skipping test class " + psiFile.getName());
                        return null;
                    }
                }
            }
        }

        File tempFile = null;
        try {
            final Checker checker = getChecker(manager.getProject(), psiFile);

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

            final CheckStyleAuditListener listener
                    = new CheckStyleAuditListener(psiFile, manager);
            checker.addListener(listener);
            checker.process(new File[]{tempFile});
            checker.destroy();

            final List<ProblemDescriptor> problems = listener.getProblems();
            return problems.toArray(new ProblemDescriptor[problems.size()]);

        } catch (ProcessCanceledException e) {
            LOG.warn("Process cancelled when scanning: " + psiFile.getName());
            return null;

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
