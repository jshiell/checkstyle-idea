package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.puppycrawl.tools.checkstyle.Checker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Main class for the CheckStyle static scanning plug-n.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStylePlugin implements ProjectComponent, Configurable,
        JDOMExternalizable {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            CheckStylePlugin.class);

    /**
     * The configuration panel for the plug-in.
     */
    private final CheckStyleConfigPanel configPanel = new CheckStyleConfigPanel();

    private final Project project;
    private ToolWindow toolWindow;

    private String toolWindowId;

    private boolean scanInProgress;

    private CheckStyleConfiguration configuration
            = new CheckStyleConfiguration();

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public CheckStylePlugin(final Project project) {
        if (project != null) {
            LOG.info("CheckStyle Plugin loaded with project: \""
                    + project.getProjectFilePath() + "\"");
        } else {
            LOG.info("CheckStyle Plugin loaded with no project.");
        }

        this.project = project;
    }

    /**
     * Get the plugin configuration.
     *
     * @return the plug-in configuration.
     */
    public CheckStyleConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get the ID for the tool window.
     *
     * @return the ID for the tool window.
     */
    public String getToolWindowId() {
        return toolWindowId;
    }

    /**
     * Is a scan in progress?
     * <p/>
     * This is only expected to be called from the event thread.
     *
     * @return true if a scan is in progress.
     */
    public boolean isScanInProgress() {
        return scanInProgress;
    }

    /**
     * Set if a scan is in progress.
     * <p/>
     * This is only expected to be called from the event thread.
     *
     * @param scanInProgress true if a scan is in progress.
     */
    public void setScanInProgress(final boolean scanInProgress) {
        this.scanInProgress = scanInProgress;
    }

    /**
     * Register the tool window with IDEA.
     */
    private void registerToolWindow() {
        final ToolWindowManager toolWindowManager
                = ToolWindowManager.getInstance(project);

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        toolWindowId = resources.getString("plugin.toolwindow.name");

        toolWindow = toolWindowManager.registerToolWindow(toolWindowId,
                new ToolWindowPanel(project), ToolWindowAnchor.BOTTOM);


        toolWindow.setIcon(IDEAUtilities.getIcon("/debugger/watches.png"));
        toolWindow.setType(ToolWindowType.DOCKED, null);
    }

    /**
     * Un-register the tool window from IDEA.
     */
    private void unregisterToolWindow() {
        final ToolWindowManager toolWindowManager
                = ToolWindowManager.getInstance(project);

        toolWindowManager.unregisterToolWindow(toolWindowId);
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
    public void readExternal(final Element element)
            throws InvalidDataException {
        if (configuration == null) {
            configuration = new CheckStyleConfiguration();
        }

        final Element childElement = element.getChild(
                CheckStyleConstants.CONFIG_ELEMENT);
        configuration.readExternal(childElement);
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(final Element element)
            throws WriteExternalException {
        if (configuration == null) {
            configuration = new CheckStyleConfiguration();
        }

        final Element configElement = new Element(
                CheckStyleConstants.CONFIG_ELEMENT);
        configuration.writeExternal(configElement);
        element.addContent(configElement);
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
        // do nada
    }
    
    /**
     * {@inheritDoc}
     */
    public void disposeComponent() {
        // do nada
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
        return configPanel;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isModified() {
        return configPanel.isModified();
    }

    /**
     * {@inheritDoc}
     */
    public void apply() throws ConfigurationException {
        configuration.setProperty(CheckStyleConfiguration.CONFIG_FILE,
                configPanel.getConfigFile());
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        configPanel.reset();
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
            final String configFile = configuration.getProperty(
                    CheckStyleConfiguration.CONFIG_FILE);
            if (configFile == null) {
                LOG.info("Loading default configuration");
                
                final InputStream in = CheckStyleInspection.class.getResourceAsStream(
                        CheckStyleConfiguration.DEFAULT_CONFIG);
                checker = CheckerFactory.getInstance().getChecker(in);
                in.close();
                
            } else {
                LOG.info("Loading configuration from " + configFile);
                checker = CheckerFactory.getInstance().getChecker(
                        new File(configFile));
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
     * @param files the files to check.
     * @param event the event that triggered this action.
     */
    public void checkFiles(final List<VirtualFile> files, final AnActionEvent event) {
        if (files == null) {
            return;
        }

        checkFiles(files.toArray(new VirtualFile[files.size()]), event);
    }

    /**
     * Run a scan on the currently selected file.
     *
     * @param files the files to check.
     * @param event the event that triggered this action.
     */
    public void checkFiles(final VirtualFile[] files, final AnActionEvent event) {
        LOG.info("Scanning current file(s).");

        if (files == null) {
            LOG.debug("No files provided.");
            return;
        }
        
        final CheckFilesThread checkFilesThread = new CheckFilesThread(files);
        scanInProgress = true;
        checkFilesThread.start();
    }

    /**
     * Get the tool window panel for result display.
     *
     * @return the tool window panel.
     */
    private ToolWindowPanel getToolWindowPanel() {
        return ((ToolWindowPanel) toolWindow.getComponent());
    }

    /**
     * Scan a PSI file with CheckStyle.
     *
     * @param element the PSI element to scan. This will be ignored if not
     *                a java file.
     * @return a list of tree nodes representing the result tree for this
     *         file, an empty list or null if this file is invalid or has no errors.
     */
    private List<ProblemDescriptor> checkPsiFile(final PsiElement element) {
        if (element == null || !element.isValid() || !element.isPhysical()
                || !PsiFile.class.isAssignableFrom(element.getClass())) {
            return null;
        }

        final PsiFile psiFile = (PsiFile) element;
        LOG.debug("Scanning " + psiFile.getName());   

        final InspectionManager manager
                = InspectionManager.getInstance(psiFile.getProject());

        if (!CheckStyleUtilities.isValidFileType(psiFile.getFileType())) {
            return null;
        }

        File tempFile = null;
        try {
            final Checker checker = getChecker();

            // we need to copy to a file as IntelliJ may not have saved the file recently...
            final CreateTempFileThread fileThread = new CreateTempFileThread(
                    psiFile);
            ApplicationManager.getApplication().runReadAction(fileThread);

            // rethrow any error from the thread.
            if (fileThread.getFailure() != null) {
                if (Error.class.isAssignableFrom(
                        fileThread.getFailure().getClass())) {
                    throw (Error) fileThread.getFailure();
                } else if (RuntimeException.class.isAssignableFrom(
                        fileThread.getFailure().getClass())) {
                    throw (RuntimeException) fileThread.getFailure();
                } else if (IOException.class.isAssignableFrom(
                        fileThread.getFailure().getClass())) {
                    throw (IOException) fileThread.getFailure();
                }
                throw new RuntimeException(fileThread.getFailure());
            }

            tempFile = fileThread.getFile();
            if (tempFile == null) {
                throw new IllegalStateException(
                        "Failed to create temporary file.");
            }

            final CheckStyleAuditListener listener
                    = new CheckStyleAuditListener(psiFile, manager, true);
            checker.addListener(listener);
            checker.process(new File[]{tempFile});
            checker.destroy();

            return listener.getProblems();

        } catch (IOException e) {
            LOG.error("Failure when creating temp file", e);
            throw new IllegalStateException("Couldn't create temp file", e);

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Thread to read the file to a temporary file.
     */
    private class CreateTempFileThread implements Runnable {

        private Throwable failure;
        private PsiFile psiFile;
        private File file;

        /**
         * Create a thread to read the given file to a temporary file.
         *
         * @param psiFile the file to read.
         */
        public CreateTempFileThread(final PsiFile psiFile) {
            this.psiFile = psiFile;
        }

        /**
         * Get any failure that occurred in this thread.
         *
         * @return the failure, if any.
         */
        public Throwable getFailure() {
            return failure;
        }

        /**
         * Get the temporary file.
         *
         * @return the temporary file.
         */
        public File getFile() {
            return file;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                file = File.createTempFile(CheckStyleConstants.TEMPFILE_NAME,
                        CheckStyleConstants.TEMPFILE_EXTENSION);
                final BufferedWriter tempFileOut = new BufferedWriter(
                        new FileWriter(file));
                tempFileOut.write(psiFile.getText());
                tempFileOut.flush();
                tempFileOut.close();

            } catch (Throwable e) {
                failure = e;

            } 
        }
    }

    /**
     * Thread for file checking, to ensure we don't lock up the UI.
     */
    private class CheckFilesThread extends Thread {

        private final List<PsiFile> files = new ArrayList<PsiFile>();

        /**
         * Create a thread to check the given files.
         *
         * @param virtualFiles the files to check.
         */
        public CheckFilesThread(final VirtualFile[] virtualFiles) {
            if (virtualFiles == null) {
                throw new IllegalArgumentException("Files may not be null.");
            }

            final List<VirtualFile> fileList = new ArrayList<VirtualFile>();
            for (final VirtualFile virtualFile : virtualFiles) {
                fileList.addAll(flattenFiles(virtualFile));
            }

            // this needs to be done on the main thread.
            final PsiManager psiManager = PsiManager.getInstance(project);
            for (final VirtualFile virtualFile : fileList) {
                files.add(psiManager.findFile(virtualFile));
            }
        }

        /**
         * Execute the file check.
         */
        public void run() {
            try {
                // set progress bar
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getToolWindowPanel().setProgressBarMax(files.size());
                        getToolWindowPanel().displayInProgress();
                    }
                });

                final Map<PsiFile, List<ProblemDescriptor>> fileResults
                        = new HashMap<PsiFile, List<ProblemDescriptor>>();

                for (final PsiFile psiFile : files) {
                    final List<ProblemDescriptor> results = checkPsiFile(psiFile);

                    // add results if necessary
                    if (results != null && results.size() > 0) {
                        fileResults.put(psiFile, results);
                    }

                    // increment progress bar
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getToolWindowPanel().incrementProgressBar();
                        }
                    });
                }

                // invoke Swing fun in Swing thread.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getToolWindowPanel().displayResults(fileResults);
                        getToolWindowPanel().expandTree();
                        getToolWindowPanel().clearProgressBar();
                        getToolWindowPanel().setProgressText(null);

                        scanInProgress = false;
                    }
                });
                
            } catch (Throwable e) {
                LOG.error("An error occurred while scanning a file.", e);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getToolWindowPanel().displayErrorResult();
                        getToolWindowPanel().clearProgressBar();
                        getToolWindowPanel().setProgressText(null);

                        scanInProgress = false;
                    }
                });
            }
        }

        /**
         * Flatten the tree structure represented by a virtual file.
         *
         * @param file the tree to flatten.
         * @return a list of flattened files.
         */
        private List<VirtualFile> flattenFiles(final VirtualFile file) {
            final List<VirtualFile> elementList = new ArrayList<VirtualFile>();
            elementList.add(file);

            if (file.getChildren() != null) {
                for (final VirtualFile childFile : file.getChildren()) {
                    elementList.addAll(flattenFiles(childFile));
                }
            }

            return elementList;
        }
    }

}
