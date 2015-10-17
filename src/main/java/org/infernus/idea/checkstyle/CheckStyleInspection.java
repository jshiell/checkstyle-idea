package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checker.CheckerFactory;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.ui.CheckStyleInspectionPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.infernus.idea.checkstyle.util.Async.asyncResultOf;

public class CheckStyleInspection extends LocalInspectionTool {

    private static final Log LOG = LogFactory.getLog(CheckStyleInspection.class);
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
        return asProblemDescriptors(asyncResultOf(() -> inspectFile(psiFile, manager), NO_PROBLEMS_FOUND), manager);
    }

    @Nullable
    public List<Problem> inspectFile(@NotNull final PsiFile psiFile,
                                     @NotNull final InspectionManager manager) {
        LOG.debug("Inspection has been invoked.");

        final CheckStylePlugin plugin = plugin(manager.getProject());

        ConfigurationLocation configurationLocation = null;
        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            final Module module = moduleOf(psiFile);

            configurationLocation = plugin.getConfigurationLocation(module, null);
            if (configurationLocation == null || configurationLocation.isBlacklisted()) {
                return NO_PROBLEMS_FOUND;
            }

            scannableFiles.addAll(ScannableFile.createAndValidate(singletonList(psiFile), plugin, module));

            return checkerFactory(psiFile.getProject())
                    .checker(module, configurationLocation)
                    .map(checker -> checker.scan(scannableFiles, plugin.getConfiguration()))
                    .map(results -> results.get(psiFile))
                    .orElseGet(() -> NO_PROBLEMS_FOUND);

        } catch (ProcessCanceledException | AssertionError e) {
            LOG.debug("Process cancelled when scanning: " + psiFile.getName());
            return NO_PROBLEMS_FOUND;

        } catch (CheckStylePluginParseException e) {
            LOG.debug("Parse exception caught when scanning: " + psiFile.getName(), e);
            return NO_PROBLEMS_FOUND;

        } catch (CheckStylePluginException e) {
            blacklist(configurationLocation);
            LOG.error("CheckStyle threw an exception when scanning: " + psiFile.getName(), e);
            return NO_PROBLEMS_FOUND;

        } catch (Throwable e) {
            LOG.warn("The inspection could not be executed.", e);
            return NO_PROBLEMS_FOUND;

        } finally {
            scannableFiles.forEach(ScannableFile::deleteIfRequired);
        }
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

    private Module moduleOf(@NotNull final PsiFile psiFile) {
        return ModuleUtil.findModuleForPsiElement(psiFile);
    }

    private CheckerFactory checkerFactory(final Project project) {
        return ServiceManager.getService(project, CheckerFactory.class);
    }


}
