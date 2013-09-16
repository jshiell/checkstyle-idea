package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
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
import org.infernus.idea.checkstyle.util.ScannableFile;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Inspection for CheckStyle integration for IntelliJ IDEA.
 */
public class CheckStyleInspection extends LocalInspectionTool {

    private static final Log LOG = LogFactory.getLog(CheckStyleInspection.class);

    private final CheckStyleInspectionPanel configPanel = new CheckStyleInspectionPanel();

    /**
     * Produce a CheckStyle checker.
     *
     * @param checkStylePlugin the plugin.
     * @param module           the current module. May be null.
     * @return a checker.
     */
    private Checker getChecker(final CheckStylePlugin checkStylePlugin,
                               @Nullable final Module module) {
        LOG.debug("Getting CheckStyle checker for inspection.");

        if (module == null) {
            return null;
        }

        ConfigurationLocation configurationLocation = null;
        try {
            configurationLocation = getConfigurationLocation(module, checkStylePlugin);
            if (configurationLocation == null) {
                return null;
            }

            if (configurationLocation.isBlacklisted()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Configuration is blacklisted, skipping: " + configurationLocation);
                }
                return null;
            }

            LOG.info("Loading configuration from " + configurationLocation);
            return getCheckerFactory().getChecker(configurationLocation, module);

        } catch (Exception e) {
            LOG.error("Checker could not be created.", e);

            if (configurationLocation != null) {
                configurationLocation.blacklist();
            }

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

    private CheckerFactory getCheckerFactory() {
        return ServiceManager.getService(CheckerFactory.class);
    }

    private ConfigurationLocation getConfigurationLocation(final Module module,
                                                           final CheckStylePlugin checkStylePlugin) {
        final ConfigurationLocation configurationLocation;
        if (module != null) {
            final CheckStyleModuleConfiguration moduleConfiguration
                    = ModuleServiceManager.getService(module, CheckStyleModuleConfiguration.class);
            if (moduleConfiguration == null) {
                throw new IllegalStateException("Couldn't get checkstyle module configuration");
            }

            if (moduleConfiguration.isExcluded()) {
                configurationLocation = null;
            } else {
                configurationLocation = moduleConfiguration.getActiveConfiguration();
            }

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
            if (configurationLocation == null) {
                return null;
            }

            LOG.info("Loading configuration from " + configurationLocation);
            return getCheckerFactory().getConfig(configurationLocation, module);

        } catch (Exception e) {
            LOG.error("Checker could not be created.", e);
            throw new CheckStylePluginException("Couldn't create Checker", e);
        }
    }

    @Nullable
    public JComponent createOptionsPanel() {
        return configPanel;
    }

    @NotNull
    public String getGroupDisplayName() {
        return IDEAUtilities.getResource("plugin.group", "CheckStyle");
    }

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

    @NotNull
    @NonNls
    public String getShortName() {
        return CheckStyleConstants.ID_PLUGIN;
    }

    @Nullable
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile psiFile,
                                         @NotNull final InspectionManager manager,
                                         final boolean isOnTheFly) {
        LOG.debug("Inspection has been invoked.");

        try {
            if (!psiFile.isValid() || !psiFile.isPhysical()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping file as invalid: " + psiFile.getName());
                }
                return null;
            }

            final CheckStylePlugin checkStylePlugin = getPlugin(manager.getProject());

            if (!checkStylePlugin.getConfiguration().isScanningNonJavaFiles()
                    && !CheckStyleUtilities.isJavaFile(psiFile.getFileType())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping as file is not a Java file: " + psiFile.getName());
                }
                return null;
            }

            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);

            final boolean checkTestClasses = checkStylePlugin.getConfiguration().isScanningTestClasses();
            if (!checkTestClasses && module != null) {
                final VirtualFile elementFile = psiFile.getContainingFile().getVirtualFile();
                if (elementFile != null) {
                    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                    if (moduleRootManager != null && moduleRootManager.getFileIndex().isInTestSourceContent(elementFile)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Skipping test class " + psiFile.getName());
                        }
                        return null;
                    }
                }
            }

            return scanFile(psiFile, manager, checkStylePlugin, module);

        } catch (ProcessCanceledException e) {
            LOG.warn("Process cancelled when scanning: " + psiFile.getName());
            return null;

        } catch (AssertionError e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Assertion error caught, exiting quietly", e);
            }
            return null;

        } catch (Throwable e) {
            final CheckStylePluginException processed = CheckStylePlugin.processError(
                    "The inspection could not be executed.", e);
            LOG.error("The inspection could not be executed.", processed);

            return null;
        }
    }

    private ProblemDescriptor[] scanFile(final PsiFile psiFile,
                                         final InspectionManager manager,
                                         final CheckStylePlugin checkStylePlugin,
                                         final Module module)
            throws IOException {
        ScannableFile scannableFile = null;
        try {
            final Checker checker = getChecker(checkStylePlugin, module);
            final Configuration config = getConfig(checkStylePlugin, module);
            if (checker == null || config == null) {
                return new ProblemDescriptor[0];
            }

            final List<Check> checks = CheckFactory.getChecks(config);

            final Document fileDocument = PsiDocumentManager.getInstance(
                    manager.getProject()).getDocument(psiFile);
            if (fileDocument == null) {
                LOG.debug("Skipping check - file is binary or has no document: " + psiFile.getName());
                return null;
            }

            scannableFile = new ScannableFile(psiFile);

            final Map<String, PsiFile> filesToScan = Collections.singletonMap(scannableFile.getAbsolutePath(), psiFile);

            final boolean suppressingErrors = checkStylePlugin.getConfiguration().isSuppressingErrors();
            final CheckStyleAuditListener listener = new CheckStyleAuditListener(filesToScan, manager, false, suppressingErrors, checks);
            synchronized (checker) {
                checker.addListener(listener);
                checker.process(Arrays.asList(scannableFile.getFile()));
                checker.removeListener(listener);
            }

            final List<ProblemDescriptor> problems = listener.getProblems(psiFile);
            return problems.toArray(new ProblemDescriptor[problems.size()]);

        } finally {
            if (scannableFile != null) {
                scannableFile.deleteIfRequired();
            }
        }
    }

}
