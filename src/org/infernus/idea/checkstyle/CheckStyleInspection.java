package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;

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
     * The configuration panel for the inspection.
     */
    private final CheckStyleFilePanel configPanel = new CheckStyleFilePanel();

    /**
     * Produce a CheckStyle checker.
     *
     * @return a checker.
     */
    public Checker getChecker() {
        try {
            final Checker checker;
            final File configFile = configPanel.getConfigFile();
            if (configFile == null) {
                final InputStream in = CheckStyleInspection.class.getResourceAsStream(
                        CheckStyleConstants.DEFAULT_CONFIG);
                checker = CheckerFactory.getInstance().getChecker(in);
                in.close();
            } else {
                checker = CheckerFactory.getInstance().getChecker(configFile);
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
    public void readSettings(final Element element)
            throws InvalidDataException {
        final Element configPathElement = element.getChild(
                CheckStyleConstants.CONFIG_FILEPATH_ELEMENT);
        if (configPathElement != null) {
            final String filePath = configPathElement.getText();
            if (filePath != null && filePath.trim().length() > 0) {
                configPanel.setConfigFile(new File(filePath));
            } else {
                configPanel.setConfigFile(null);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeSettings(final Element element)
            throws WriteExternalException {
        final File configFile = configPanel.getConfigFile();
        if (configFile != null) {
            final Element configPathElement = new Element(
                    CheckStyleConstants.CONFIG_FILEPATH_ELEMENT);
            configPathElement.setText(configFile.getAbsolutePath());
            element.addContent(configPathElement);
        }
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
        if (!psiFile.isValid() || !psiFile.isPhysical()
                || !CheckStyleUtilities.isValidFileType(psiFile.getFileType())) {
            return null;
        }

        File tempFile = null;
        try {
            final Checker checker = getChecker();

            // we need to copy to a file as IntelliJ may not have saved the file recently...
            tempFile = File.createTempFile(CheckStyleConstants.TEMPFILE_NAME, CheckStyleConstants.TEMPFILE_EXTENSION);
            final BufferedWriter tempFileOut = new BufferedWriter(
                    new FileWriter(tempFile));
            tempFileOut.write(psiFile.getText());
            tempFileOut.flush();
            tempFileOut.close();

            final CheckStyleAuditListener listener = new CheckStyleAuditListener(psiFile, manager);
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
