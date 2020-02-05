package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.CheckerFactory;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.config.ConfigurationLocationSource;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.ui.CheckStyleInspectionPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Async.asyncResultOf;
import static org.infernus.idea.checkstyle.util.Notifications.showException;
import static org.infernus.idea.checkstyle.util.Notifications.showWarning;

public class CheckStyleInspection extends LocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(CheckStyleInspection.class);
    private static final List<Problem> NO_PROBLEMS_FOUND = Collections.emptyList();
    private static final long FIVE_SECONDS = 5000L;

    private final CheckStyleInspectionPanel configPanel = new CheckStyleInspectionPanel();

    @Nullable
    public JComponent createOptionsPanel() {
        return configPanel;
    }

    @Override
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile psiFile,
                                         @NotNull final InspectionManager manager,
                                         final boolean isOnTheFly) {
        final Module module = moduleOf(psiFile);
        List<ScannableFile> scannableFiles = ScannableFile.createAndValidate(singletonList(psiFile), manager.getProject(), module);

        try {
            return asProblemDescriptors(
                    asyncResultOf(() -> {
                        try {
                            return inspectFile(psiFile, scannableFiles, module, manager);
                        } finally {
                            scannableFiles.forEach(ScannableFile::deleteIfRequired);
                        }
                    }, NO_PROBLEMS_FOUND, FIVE_SECONDS),
                    manager);

        } catch (ProcessCanceledException | AssertionError e) {
            LOG.debug("Inspection cancelled when scanning: " + psiFile.getName());
            return noProblemsFound(manager);

        } catch (Throwable e) {
            LOG.warn("CheckStyle threw an exception when inspecting: " + psiFile.getName(), e);
            showException(manager.getProject(), e);
            return noProblemsFound(manager);
        }
    }

    private ConfigurationLocationSource configurationLocationSource(final Project project) {
        return ServiceManager.getService(project, ConfigurationLocationSource.class);
    }

    @NotNull
    private ProblemDescriptor[] noProblemsFound(@NotNull final InspectionManager manager) {
        return asProblemDescriptors(NO_PROBLEMS_FOUND, manager);
    }

    @Nullable
    private Module moduleOf(@NotNull final PsiFile psiFile) {
        return ModuleUtil.findModuleForPsiElement(psiFile);
    }

    private List<Problem> inspectFile(@NotNull final PsiFile psiFile,
                                      @NotNull final List<ScannableFile> scannableFiles,
                                      @Nullable final Module module,
                                      @NotNull final InspectionManager manager) {
        LOG.debug("Inspection has been invoked for " + psiFile.getName());

        ConfigurationLocation configurationLocation = null;
        try {
            configurationLocation = configurationLocationSource(manager.getProject())
                    .getConfigurationLocation(module, null);
            if (configurationLocation == null || configurationLocation.isBlacklisted()) {
                return NO_PROBLEMS_FOUND;
            }

            scannableFiles.addAll(ScannableFile.createAndValidate(singletonList(psiFile), manager.getProject(), module));

            return checkerFactory(psiFile.getProject())
                    .checker(module, configurationLocation)
                    .map(checker -> checker.scan(scannableFiles, configurationManager(psiFile.getProject()).getCurrent().isSuppressErrors()))
                    .map(results -> results.get(psiFile))
                    .map(this::dropIgnoredProblems)
                    .orElse(NO_PROBLEMS_FOUND);

        } catch (ProcessCanceledException | AssertionError e) {
            LOG.debug("Process cancelled when scanning: " + psiFile.getName());
            return NO_PROBLEMS_FOUND;

        } catch (CheckStylePluginParseException e) {
            LOG.debug("Parse exception caught when scanning: " + psiFile.getName(), e);
            return NO_PROBLEMS_FOUND;

        } catch (Throwable e) {
            handlePluginException(e, psiFile, configurationLocation, manager.getProject());
            return NO_PROBLEMS_FOUND;

        } finally {
            scannableFiles.forEach(ScannableFile::deleteIfRequired);
        }
    }

    private List<Problem> dropIgnoredProblems(final List<Problem> problems) {
        return problems.stream()
                .filter(problem -> problem.severityLevel() != SeverityLevel.Ignore)
                .collect(toList());
    }

    private void handlePluginException(final Throwable e,
                                       final @NotNull PsiFile psiFile,
                                       final ConfigurationLocation configurationLocation,
                                       final @NotNull Project project) {
        if (e.getCause() != null && e.getCause() instanceof ProcessCanceledException) {
            LOG.debug("Process cancelled when scanning: " + psiFile.getName());

        } else if (e.getCause() != null && e.getCause() instanceof FileNotFoundException) {
            disableActiveConfiguration(project);

        } else if (e.getCause() != null && e.getCause() instanceof IOException) {
            showWarning(project, message("checkstyle.file-io-failed"));
            blacklist(configurationLocation);

        } else {
            LOG.warn("CheckStyle threw an exception when scanning: " + psiFile.getName(), e);
            showException(project, e);
            blacklist(configurationLocation);
        }
    }

    private void disableActiveConfiguration(final Project project) {
        configurationManager(project).disableActiveConfiguration();
        showWarning(project, message("checkstyle.configuration-disabled.file-not-found"));
    }

    private void blacklist(final ConfigurationLocation configurationLocation) {
        if (configurationLocation != null) {
            configurationLocation.blacklist();
        }
    }

    @NotNull
    private ProblemDescriptor[] asProblemDescriptors(final List<Problem> results, final InspectionManager manager) {
        return ofNullable(results)
                .map(TreeSet::new)
                .map(problems -> problems.stream()
                        .map(problem -> problem.toProblemDescriptor(manager))
                        .toArray(ProblemDescriptor[]::new))
                .orElse(ProblemDescriptor.EMPTY_ARRAY);
    }

    private CheckerFactory checkerFactory(final Project project) {
        return ServiceManager.getService(project, CheckerFactory.class);
    }

    private PluginConfigurationManager configurationManager(final Project project) {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }

}
