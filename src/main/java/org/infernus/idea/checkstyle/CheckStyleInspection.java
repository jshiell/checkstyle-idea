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
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.ui.CheckStyleInspectionPanel;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Async.asyncResultOf;
import static org.infernus.idea.checkstyle.util.Notifications.showWarning;

public class CheckStyleInspection extends LocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(CheckStyleInspection.class);
    private static final List<Problem> NO_PROBLEMS_FOUND = Collections.emptyList();

    private final CheckStyleInspectionPanel configPanel = new CheckStyleInspectionPanel();

    private CheckStylePlugin plugin(final Project project) {
        final CheckStylePlugin checkStylePlugin = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }
        return checkStylePlugin;
    }

    @Nullable
    public JComponent createOptionsPanel() {
        return configPanel;
    }

    @Override
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile psiFile,
                                         @NotNull final InspectionManager manager,
                                         final boolean isOnTheFly) {
        final Module module = moduleOf(psiFile);
        return asProblemDescriptors(asyncResultOf(() -> inspectFile(psiFile, module, manager), NO_PROBLEMS_FOUND), manager);
    }

    @Nullable
    private Module moduleOf(@NotNull final PsiFile psiFile) {
        return ModuleUtil.findModuleForPsiElement(psiFile);
    }

    @Nullable
    public List<Problem> inspectFile(@NotNull final PsiFile psiFile,
                                     @Nullable final Module module,
                                     @NotNull final InspectionManager manager) {
        LOG.debug("Inspection has been invoked.");

        final CheckStylePlugin plugin = plugin(manager.getProject());

        ConfigurationLocation configurationLocation = null;
        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            configurationLocation = plugin.getConfigurationLocation(module, null);
            if (configurationLocation == null || configurationLocation.isBlacklisted()) {
                return NO_PROBLEMS_FOUND;
            }

            scannableFiles.addAll(ScannableFile.createAndValidate(singletonList(psiFile), plugin, module));

            return checkerFactory(psiFile.getProject())
                    .checker(module, configurationLocation)
                    .map(checker -> checker.scan(scannableFiles, plugin.getConfiguration().getCurrentPluginConfig().isSuppressErrors()))
                    .map(results -> results.get(psiFile))
                    .map(this::dropIgnoredProblems)
                    .orElseGet(() -> NO_PROBLEMS_FOUND);

        } catch (ProcessCanceledException | AssertionError e) {
            LOG.debug("Process cancelled when scanning: " + psiFile.getName());
            return NO_PROBLEMS_FOUND;

        } catch (CheckStylePluginParseException e) {
            LOG.debug("Parse exception caught when scanning: " + psiFile.getName(), e);
            return NO_PROBLEMS_FOUND;

        } catch (CheckStylePluginException e) {
            handlePluginException(e, psiFile, plugin, configurationLocation, manager.getProject());
            return NO_PROBLEMS_FOUND;

        } catch (Throwable e) {
            LOG.warn("The inspection could not be executed.", e);
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

    private void handlePluginException(final CheckStylePluginException e,
                                       final @NotNull PsiFile psiFile,
                                       final CheckStylePlugin plugin,
                                       final ConfigurationLocation configurationLocation,
                                       final @NotNull Project project) {
        if (e.getCause() != null && e.getCause() instanceof FileNotFoundException) {
            disableActiveConfiguration(plugin, project);

        } else if (e.getCause() != null && e.getCause() instanceof IOException) {
            showWarning(project, message("checkstyle.file-io-failed"));
            blacklist(configurationLocation);

        } else {
            LOG.warn("CheckStyle threw an exception when scanning: " + psiFile.getName(), e);
            Notifications.showException(project, e);
            blacklist(configurationLocation);
        }
    }

    private void disableActiveConfiguration(final CheckStylePlugin plugin, final Project project) {
        plugin.getConfiguration().disableActiveConfiguration();
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
                .map(problems -> problems.stream()
                        .map(problem -> problem.toProblemDescriptor(manager))
                        .toArray(ProblemDescriptor[]::new))
                .orElseGet(() -> ProblemDescriptor.EMPTY_ARRAY);
    }

    private CheckerFactory checkerFactory(final Project project) {
        return ServiceManager.getService(project, CheckerFactory.class);
    }


}
