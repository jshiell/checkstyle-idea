package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.handlers.ScanFilesBeforeCheckinHandler;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Main class for the CheckStyle static scanning plug-n.
 *
 * @author James Shiell
 * @version 1.0
 */
@State(
        name = CheckStyleConstants.ID_PLUGIN,
        storages = {
                @Storage(
                        id = "other",
                        file = "$PROJECT_FILE$"
                )}
)
public final class CheckStylePlugin extends CheckinHandlerFactory implements ProjectComponent, Configurable,
        PersistentStateComponent<CheckStylePlugin.ConfigurationBean> {

    /**
     * Logger for this class.
     */
    @NonNls
    private static final Log LOG = LogFactory.getLog(CheckStylePlugin.class);

    /**
     * Any threads in progress.
     */
    private final Set<AbstractCheckerThread> checksInProgress
            = new HashSet<AbstractCheckerThread>();

    /**
     * The configuration panel for the plug-in.
     */
    private CheckStyleConfigPanel configPanel;

    /**
     * A reference to the current project.
     */
    final Project project;

    /**
     * The tool window for the plugin.
     */
    private ToolWindow toolWindow;

    /**
     * Classloader for third party libraries.
     */
    private ClassLoader thirdPartyClassloader;

    /**
     * Configuration store.
     */
    CheckStyleConfiguration configuration;

    /**
     * {@inheritDoc}
     */
    public CheckStylePlugin.ConfigurationBean getState() {
        final ConfigurationBean configBean = new ConfigurationBean();
        for (final Enumeration confNames = configuration.propertyNames(); confNames.hasMoreElements();) {
            final String elementName = (String) confNames.nextElement();
            configBean.configuration.put(elementName, configuration.getProperty(elementName));
        }
        return configBean;
    }

    /**
     * {@inheritDoc}
     */
    public void loadState(final CheckStylePlugin.ConfigurationBean newConfiguration) {
        configuration.clear();

        if (newConfiguration != null && newConfiguration.configuration != null) {
            for (final String key : newConfiguration.configuration.keySet()) {
                configuration.setProperty(key, newConfiguration.configuration.get(key));
            }
        }
    }

    /**
     * Project getter.
     *
     * @return Project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Get the base path of the project.
     *
     * @return the base path of the project.
     */
    @Nullable
    public File getProjectPath() {
        if (project == null) {
            return null;
        }

        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public CheckStylePlugin(final Project project) {
        this.project = project;
        this.configuration = new CheckStyleConfiguration(project);

        try {
            if (project != null) {
                LOG.info("CheckStyle Plugin loaded with project base dir: \""
                        + getProjectPath() + "\"");
            } else {
                LOG.info("CheckStyle Plugin loaded with no project.");
            }

            this.configPanel = new CheckStyleConfigPanel(this);

        } catch (Throwable t) {
            LOG.error("Project initialisation failed.", t);
        }
    }

    /**
     * Get classloader for third party libraries.
     *
     * @return the classloader for third party libraries.
     */
    public synchronized ClassLoader getThirdPartyClassloader() {
        if (thirdPartyClassloader == null) {
            final List<String> thirdPartyClasses
                    = configuration.getThirdPartyClassPath();
            if (thirdPartyClasses.size() > 0) {
                final URL[] urlList = new URL[thirdPartyClasses.size()];
                int index = 0;
                for (final String pathElement : thirdPartyClasses) {
                    try {
                        // toURI().toURL() escapes, whereas toURL() doesn't.
                        urlList[index] = new File(pathElement).toURI().toURL();
                        ++index;

                    } catch (MalformedURLException e) {
                        LOG.error("Third party classpath element is malformed: "
                                + pathElement, e);
                    }
                }

                thirdPartyClassloader = new URLClassLoader(urlList,
                        getClass().getClassLoader());

            } else {
                thirdPartyClassloader = getClass().getClassLoader();
            }
        }

        return thirdPartyClassloader;
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
     * Is a scan in progress?
     * <p/>
     * This is only expected to be called from the event thread.
     *
     * @return true if a scan is in progress.
     */
    public boolean isScanInProgress() {
        synchronized (checksInProgress) {
            return checksInProgress.size() > 0;
        }
    }

    /**
     * Register the tool window with IDEA.
     */
    private void registerToolWindow() {
        LOG.debug("Registering tool window.");

        final ToolWindowManager toolWindowManager
                = ToolWindowManager.getInstance(project);

        toolWindow = toolWindowManager.registerToolWindow(CheckStyleConstants.ID_TOOLWINDOW,
                false, ToolWindowAnchor.BOTTOM);

        final Content toolContent = toolWindow.getContentManager().getFactory().createContent(
                new ToolWindowPanel(project), IDEAUtilities.getResource("plugin.toolwindow.action",
                        "Scan"), false);
        toolWindow.getContentManager().addContent(toolContent);

        toolWindow.setTitle(IDEAUtilities.getResource("plugin.toolwindow.name",
                "Scan"));
        toolWindow.setIcon(IDEAUtilities.getIcon(
                "/org/infernus/idea/checkstyle/images/checkstyle16.png"));
        toolWindow.setType(ToolWindowType.DOCKED, null);
    }

    /**
     * Un-register the tool window from IDEA.
     */
    private void unregisterToolWindow() {
        LOG.debug("Deregistering tool window.");

        final ToolWindowManager toolWindowManager
                = ToolWindowManager.getInstance(project);

        toolWindowManager.unregisterToolWindow(CheckStyleConstants.ID_TOOLWINDOW);
    }

    /**
     * {@inheritDoc}
     */
    public void projectOpened() {
        LOG.debug("Project opened.");

        registerToolWindow();
    }

    /**
     * {@inheritDoc}
     */
    public void projectClosed() {
        LOG.debug("Project closed.");

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
        ProjectLevelVcsManager.getInstance(this.project).registerCheckinHandlerFactory(this);
    }

    /**
     * {@inheritDoc}
     */
    public void disposeComponent() {
        ProjectLevelVcsManager.getInstance(this.project).unregisterCheckinHandlerFactory(this);
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return IDEAUtilities.getResource("plugin.configuration-name",
                "CheckStyle Plugin");
    }

    /**
     * {@inheritDoc}
     */
    public Icon getIcon() {
        return IDEAUtilities.getIcon(
                "/org/infernus/idea/checkstyle/images/checkstyle32.png");
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
        if (configPanel == null) {
            return null;
        }

        reset();

        return configPanel;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isModified() {
        return configPanel != null && configPanel.isModified();
    }

    /**
     * {@inheritDoc}
     */
    public void apply() throws ConfigurationException {
        if (configPanel == null) {
            return;
        }

        configuration.setConfigurationLocations(configPanel.getConfigurationLocations());
        configuration.setActiveConfiguration(configPanel.getActiveLocation());

        configuration.setScanningTestClasses(configPanel.isScanTestClasses());

        final List<String> thirdPartyClasspath
                = configPanel.getThirdPartyClasspath();
        configuration.setThirdPartyClassPath(thirdPartyClasspath);

        reset(); // save current data as unmodified

        thirdPartyClassloader = null; // reset to force reload
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        if (configPanel == null) {
            return;
        }

        configPanel.setDefaultLocation(configuration.getDefaultLocation());
        configPanel.setConfigurationLocations(configuration.getConfigurationLocations());
        configPanel.setActiveLocation(configuration.getActiveConfiguration());
        configPanel.setScanTestClasses(configuration.isScanningTestClasses());
        configPanel.setThirdPartyClasspath(configuration.getThirdPartyClassPath());
    }

    /**
     * {@inheritDoc}
     */
    public void disposeUIResources() {

    }

    /**
     * Process an error.
     *
     * @param message a description of the error. May be null.
     * @param error   the exception.
     * @return any exception to be passed upwards.
     */
    public static CheckStylePluginException processError(final String message,
                                                         @NotNull final Throwable error) {
        Throwable root = error;
        while (root.getCause() != null
                && !(root instanceof CheckstyleException)) {
            root = root.getCause();
        }

        if (message != null) {
            return new CheckStylePluginException(message, root);
        }

        return new CheckStylePluginException(root.getMessage(), root);
    }

    /**
     * Produce a CheckStyle checker.
     *
     * @param classLoader CheckStyle classloader or null if default
     *                    should be used.
     * @return a checker.
     */
    public Checker getChecker(final ClassLoader classLoader) {
        LOG.debug("Getting CheckStyle checker.");

        try {
            final ConfigurationLocation location = configuration.getActiveConfiguration();
            if (location == null) {
                return null;
            }

            // TODO caching

            final Checker checker = CheckerFactory.getInstance().getChecker(
                    location.resolve(), classLoader,
                    location.getProperties());

            return checker;

        } catch (Throwable e) {
            throw new CheckStylePluginException("Couldn't create Checker", e);
        }
    }

    /**
     * Run a scan on the currently selected file.
     *
     * @param files the files to check.
     * @param event the event that triggered this action.
     */
    public void checkFiles(final List<VirtualFile> files,
                           final AnActionEvent event) {
        LOG.info("Scanning current file(s).");

        if (files == null) {
            LOG.debug("No files provided.");
            return;
        }

        final CheckFilesThread checkFilesThread = new CheckFilesThread(this, files);
        checkFilesThread.setPriority(Thread.MIN_PRIORITY);

        synchronized (checksInProgress) {
            checksInProgress.add(checkFilesThread);
        }

        checkFilesThread.start();
    }

    /**
     * Stop any checks in progress.
     */
    public void stopChecks() {
        synchronized (checksInProgress) {
            for (final AbstractCheckerThread thread : checksInProgress) {
                thread.stopCheck();
            }

            checksInProgress.clear();
        }
    }

    /**
     * Mark a thread as complete.
     *
     * @param thread the thread to mark.
     */
    public void setThreadComplete(final AbstractCheckerThread thread) {
        if (thread == null) {
            return;
        }

        synchronized (checksInProgress) {
            checksInProgress.remove(thread);
        }
    }

    public Map<PsiFile, List<ProblemDescriptor>> scanFiles(final List<VirtualFile> files,
                                                           final Map<PsiFile, List<ProblemDescriptor>> results) {
        LOG.info("Scanning current file(s).");
        if (files == null) {
            LOG.debug("No files provided.");
            return results;
        }
        final ScanFilesThread scanFilesThread = new ScanFilesThread(this, files, results);

        synchronized (checksInProgress) {
            checksInProgress.add(scanFilesThread);
        }

        scanFilesThread.start();
        try {
            scanFilesThread.join();

        } catch (final Throwable e) {
            LOG.error("Error scanning files");

        } finally {
            synchronized (checksInProgress) {
                checksInProgress.remove(scanFilesThread);
            }
        }
        return results;
    }

    /**
     * Get the tool window panel for result display.
     *
     * @return the tool window panel.
     */
    public ToolWindowPanel getToolWindowPanel() {
        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null) {
            return ((ToolWindowPanel) content.getComponent());
        }
        return null;
    }

    public void activeToolWindow(boolean activate) {
        if (activate) {
            this.toolWindow.show(null);
        } else {
            this.toolWindow.hide(null);
        }
    }

    /**
     * Build a class loader for the compilation path of the module.
     *
     * @param module the module in question.
     * @return the class loader to use, or null if none applicable.
     * @throws MalformedURLException if the URL conversion fails.
     */
    ClassLoader buildModuleClassLoader(final Module module)
            throws MalformedURLException {

        if (module == null) {
            return null;
        }

        final ModuleRootManager rootManager
                = ModuleRootManager.getInstance(module);
        if (rootManager == null) {
            LOG.debug("Could not find root manager for module: "
                    + module.getName());
            return null;
        }

        final List<URL> outputPaths = new ArrayList<URL>();
        for (final VirtualFile outputPath : rootManager.getFiles(OrderRootType.COMPILATION_CLASSES)) {
            String filePath = outputPath.getPath();
            if (filePath.endsWith("!/")) { // filter JAR suffix
                filePath = filePath.substring(0, filePath.length() - 2);
            }
            outputPaths.add(new File(filePath).toURL());
        }

        return new URLClassLoader(outputPaths.toArray(
                new URL[outputPaths.size()]), getThirdPartyClassloader());
    }

    @NotNull
    public CheckinHandler createHandler(CheckinProjectPanel checkinProjectPanel) {
        return new ScanFilesBeforeCheckinHandler(this, checkinProjectPanel);
    }

    /**
     * Wrapper class for IDEA state serialisation.
     */
    public static class ConfigurationBean {
        public Map<String, String> configuration = new HashMap<String, String>();
    }
}
