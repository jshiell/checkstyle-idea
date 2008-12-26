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
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.net.*;
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
     * Flag to track if a scan is in progress.
     */
    boolean scanInProgress;

    /**
     * Classloader for third party libraries.
     */
    private ClassLoader thirdPartyClassloader;

    /**
     * Configuration store.
     */
    CheckStyleConfiguration configuration
            = new CheckStyleConfiguration();

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
                    = configuration.getListProperty(
                    CheckStyleConfiguration.THIRDPARTY_CLASSPATH);
            if (thirdPartyClasses.size() > 0) {
                final URL[] urlList = new URL[thirdPartyClasses.size()];
                int index = 0;
                for (final String pathElement : thirdPartyClasses) {
                    try {
                        final String untokenisedPath = untokenisePath(
                                pathElement);
                        // toURI().toURL() escapes, whereas toURL() doesn't.
                        urlList[index] = new File(
                                untokenisedPath).toURI().toURL();
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

        // load configuration
        final String configFile = configuration.getProperty(
                CheckStyleConfiguration.CONFIG_FILE);
        
        if (configFile != null) {
            configPanel.setConfigFile(configFile,
                    configuration.getDefinedProperies());
        } else {
            configPanel.setConfigUrl(configuration.getProperty(
                    CheckStyleConfiguration.CONFIG_URL),
                    configuration.getDefinedProperies());
        }
        configPanel.setScanTestClasses(configuration.getBooleanProperty(
                CheckStyleConfiguration.CHECK_TEST_CLASSES, true));
        configPanel.setThirdPartyClasspath(configuration.getListProperty(
                CheckStyleConfiguration.THIRDPARTY_CLASSPATH));

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

        final String errorMessage = configPanel.validateData();
        if (errorMessage != null) {
            throw new ConfigurationException(errorMessage);
        }

        final String configurationFile = configPanel.getConfigFile();
        if (configurationFile != null) {
            configuration.setProperty(CheckStyleConfiguration.CONFIG_FILE,
                    configPanel.getConfigFile());

        } else {
            configuration.remove(CheckStyleConfiguration.CONFIG_FILE);
        }

        final String configurationUrl = configPanel.getConfigUrl();
        if (configurationUrl != null) {
            configuration.setProperty(CheckStyleConfiguration.CONFIG_URL,
                    configPanel.getConfigUrl());

        } else {
            configuration.remove(CheckStyleConfiguration.CONFIG_URL);
        }

        configuration.setProperty(CheckStyleConfiguration.CHECK_TEST_CLASSES,
                Boolean.toString(configPanel.isScanTestClasses()));

        final List<String> thirdPartyClasspath
                = configPanel.getThirdPartyClasspath();
        if (thirdPartyClasspath.isEmpty()) {
            configuration.remove(CheckStyleConfiguration.THIRDPARTY_CLASSPATH);
        } else {
            configuration.setProperty(
                    CheckStyleConfiguration.THIRDPARTY_CLASSPATH,
                    thirdPartyClasspath);
        }

        configuration.clearDefinedProperies();
        final Map<String, String> properties = configPanel.getProperties();
        if (!properties.isEmpty()) {
            configuration.setDefinedProperies(properties);
        }

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

        final String configFile = configuration.getProperty(
                CheckStyleConfiguration.CONFIG_FILE);

        if (configFile != null) {
            configPanel.setConfigFile(configFile,
                    configuration.getDefinedProperies());
        } else {
            configPanel.setConfigUrl(configuration.getProperty(
                    CheckStyleConfiguration.CONFIG_URL),
                    configuration.getDefinedProperies());
        }
        
        configPanel.setThirdPartyClasspath(configuration.getListProperty(
                CheckStyleConfiguration.THIRDPARTY_CLASSPATH));
        configPanel.setScanTestClasses(configuration.getBooleanProperty(
                CheckStyleConfiguration.CHECK_TEST_CLASSES, true));
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
            final Map<String, String> checkstyleProperties
                    = configuration.getDefinedProperies();

            final Checker checker;
            String configFile = configuration.getProperty(
                    CheckStyleConfiguration.CONFIG_FILE);
            String configUrl = configuration.getProperty(
                    CheckStyleConfiguration.CONFIG_URL);
            if (configFile != null) {
                // swap prefix if required
                final File configFileToLoad = new File(untokenisePath(configFile));
                if (!configFileToLoad.exists()) {
                    throw new CheckStylePluginException("CheckStyle file does not exist at "
                            + configFileToLoad.getAbsolutePath());
                }

                LOG.info("Loading configuration from " + configFileToLoad.getAbsolutePath());
                checker = CheckerFactory.getInstance().getChecker(
                        configFileToLoad, classLoader,
                        checkstyleProperties, true);

            } else if (configUrl != null) {
                LOG.info("Loading configuration from " + configUrl);

                final File checkstyleFile = getUrl(configUrl);
                if (checkstyleFile != null) {
                    checker = CheckerFactory.getInstance().getChecker(
                            checkstyleFile, classLoader,
                            checkstyleProperties, true);
                } else {
                    throw new CheckStylePluginException("CheckStyle file does not exist at " + configUrl);
                }

            } else {
                LOG.info("Loading default configuration");

                final InputStream in
                        = CheckStyleInspection.class.getResourceAsStream(
                        CheckStyleConfiguration.DEFAULT_CONFIG);
                checker = CheckerFactory.getInstance().getChecker(
                        in, classLoader, checkstyleProperties);
                in.close();
            }

            return checker;

        } catch (Throwable e) {
            throw new CheckStylePluginException("Couldn't create Checker", e);
        }
    }

    /**
     * Process a stored file path for any tokens.
     *
     * @param path the path to process.
     * @return the processed path.
     */
    public String untokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        LOG.debug("Processing file: " + path);

        if (path.startsWith(CheckStyleConstants.PROJECT_DIR)) {
            final File projectPath = getProjectPath();
            if (projectPath != null) {
                final File fullConfigFile = new File(projectPath,
                        path.substring(CheckStyleConstants.PROJECT_DIR.length()));
                return fullConfigFile.getAbsolutePath();
            } else {
                LOG.warn("Could not untokenise path as project dir is unset: "
                        + path);
            }
        }

        return path;
    }

    /**
     * Process a path and add tokens as necessary.
     *
     * @param path the path to processed.
     * @return the tokenised path.
     */
    public String tokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        final File projectPath = getProjectPath();
        if (projectPath != null) {
            final String projectPathAbs = projectPath.getAbsolutePath();
            if (path.startsWith(projectPathAbs)) {
                return CheckStyleConstants.PROJECT_DIR + path.substring(
                        projectPathAbs.length());
            }
        }

        return path;
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

        scanInProgress = true;
        checkFilesThread.start();
    }

    public Map<PsiFile, List<ProblemDescriptor>> scanFiles(final List<VirtualFile> files,
                                                           final Map<PsiFile, List<ProblemDescriptor>> results) {
        LOG.info("Scanning current file(s).");
        if (files == null) {
            LOG.debug("No files provided.");
            return results;
        }
        final ScanFilesThread scanFilesThread = new ScanFilesThread(this, files, results);
        scanInProgress = true;
        scanFilesThread.start();
        try {
            scanFilesThread.join();
        } catch (final Throwable e) {
            LOG.error("Error scanning files");
        } finally {
            scanInProgress = false;
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

    /**
     * Fetch the contents of a URL.
     *
     * @param url the URL. If invalid null will be returned.
     * @return the contents in a temporary file, or null if retrieval failed.
     */
    public File getUrl(final String url) {
        Reader reader = null;
        Writer writer = null;
        try {
            final URLConnection urlConnection = new URL(url).openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);

            final File tempFile = File.createTempFile("checkStyle", ".xml");
            writer = new BufferedWriter(new FileWriter(tempFile));

            urlConnection.connect();
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            int readChar;
            while ((readChar = reader.read()) != -1) {
                writer.write(readChar);
            }

            writer.flush();
            return tempFile;

        } catch (IOException e) {
            LOG.error("Couldn't read URL: " + url, e);
            return null;

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
