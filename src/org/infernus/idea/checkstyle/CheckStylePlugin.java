package org.infernus.idea.checkstyle;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiElement;
import com.puppycrawl.tools.checkstyle.Checker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

/**
 * Main class for the CheckStyle static scanning plug-n.
 *
 * @author James Shiell
 * @version 1.0
 */
public class CheckStylePlugin implements ProjectComponent, Configurable {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            CheckStylePlugin.class);

    private final Project project;
    private ToolWindow toolWindow;

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public CheckStylePlugin(final Project project) {
        if (project != null) {
            LOG.info("CheckStyle Plugin loaded with project: \"" + project.getProjectFilePath() + "\"");
        } else {
            LOG.info("CheckStyle Plugin loaded with no project.");
        }

        this.project = project;
    }

    /**
     * Register the tool window with IDEA.
     */
    private void registerToolWindow() {
        final ToolWindowManager toolWindowManager
                = ToolWindowManager.getInstance(project);

        toolWindow = toolWindowManager.registerToolWindow(
                CheckStyleConstants.ID_TOOL_WINDOW,
                new ToolWindowPanel(project),
                ToolWindowAnchor.BOTTOM);

        toolWindow.setIcon(IDEAUtilities.getIcon("/debugger/watches.png"));
        toolWindow.setType(ToolWindowType.DOCKED, null);
    }

    /**
     * Un-register the tool window from IDEA.
     */
    private void unregisterToolWindow() {
        final ToolWindowManager toolWindowManager
                = ToolWindowManager.getInstance(project);

        toolWindowManager.unregisterToolWindow(CheckStyleConstants.ID_TOOL_WINDOW);
    }

    /**
     * {@inheritDoc}
     */
    public void projectOpened() {
        registerToolWindow();
    }

    /**
     * {@inheritDoc}
     */
    public void projectClosed() {
        unregisterToolWindow();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getComponentName() {
        return CheckStyleConstants.ID_PLUGIN;
    }

    /**
     * {@inheritDoc}
     */
    public void initComponent() {

    }

    /**
     * {@inheritDoc}
     */
    public void disposeComponent() {

    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        return resources.getString("plugin.configuration-name");
    }

    /**
     * {@inheritDoc}
     */
    public Icon getIcon() {
        return IDEAUtilities.getIcon("/general/configurableErrorHighlighting.png");
    }

    /**
     * {@inheritDoc}
     */
    public String getHelpTopic() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public JComponent createComponent() {
        // TODO configuration panel
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isModified() {
        // TODO test if config is modified
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void apply() throws ConfigurationException {
        // TODO apply configuration
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        // TODO reset configuration
    }

    /**
     * {@inheritDoc}
     */
    public void disposeUIResources() {

    }

    /**
     * Produce a CheckStyle checker.
     *
     * @return a checker.
     */
    public Checker getChecker() {
        try {
            final Checker checker;
            final File configFile = null; // TODO load from configuration
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
     * Run a scan on the currently selected file.
     *
     * @param event the event that triggered this action.
     */
    public void checkCurrentFile(final AnActionEvent event) {
        LOG.info("Scanning current file(s).");
        final PsiElement[] selectedElements = (PsiElement[]) event.getDataContext().getData(
                DataConstants.PSI_ELEMENT_ARRAY);
        if (selectedElements == null) {
            return;
        }

        // build flattened list of elements
        final List<PsiElement> elementList = new ArrayList<PsiElement>();
        for (final PsiElement element : selectedElements) {
            elementList.addAll(flattenElements(element));
        }

        // TODO make me work!

        for (final PsiElement element : elementList) {
            if (!element.isValid() || element.isPhysical()
                    || !PsiFile.class.isAssignableFrom(element.getClass())) {
                continue;
            }

            final PsiFile psiFile = (PsiFile) element;
            if (!CheckStyleConstants.FILETYPE_JAVA.equals(psiFile.getFileType())) {
                continue;
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

//                final CheckStyleAuditListener listener
//                        = new CheckStyleAuditListener(psiFile, manager);
//                checker.addListener(listener);
//                checker.process(new File[]{tempFile});
//                checker.destroy();
//
//                final List<ProblemDescriptor> problems = listener.getProblems();
//                return problems.toArray(new ProblemDescriptor[problems.size()]);

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

    /**
     * Flatten the tree structure represented by element.
     *
     * @param element the tree to flatten.
     * @return a list of flattened elements.
     */
    private List<PsiElement> flattenElements(final PsiElement element) {
        final List<PsiElement> elementList = new ArrayList<PsiElement>();
        elementList.add(element);

        if (element.getChildren() != null) {
            for (final PsiElement childElement : element.getChildren()) {
                elementList.addAll(flattenElements(childElement));
            }
        }

        return elementList;
    }


    /**
     * Run a scan on all files in the current module.
     *
     * @param event the event that triggered this action.
     */
    public void checkCurrentModuleFiles(final AnActionEvent event) {
        LOG.info("Scanning current module.");
        // TODO
    }


    /**
     * Run a scan on all project files.
     *
     * @param event the event that triggered this action.
     */
    public void checkProjectFiles(final AnActionEvent event) {
        LOG.info("Scanning current project.");
        // TODO
    }

}
