package org.infernus.idea.checkstyle.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.csapi.ConfigVisitor;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.exception.CheckstyleServiceException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.cmd.CheckstyleCommand;
import org.infernus.idea.checkstyle.service.cmd.OpCreateChecker;
import org.infernus.idea.checkstyle.service.cmd.OpDestroyChecker;
import org.infernus.idea.checkstyle.service.cmd.OpLoadConfiguration;
import org.infernus.idea.checkstyle.service.cmd.OpPeruseConfiguration;
import org.infernus.idea.checkstyle.service.cmd.OpScan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CheckstyleActionsImpl implements CheckstyleActions {

    private final Project project;
    private final CheckstyleProjectService checkstyleProjectService;

    public CheckstyleActionsImpl(@NotNull final Project project,
                                 @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this.project = project;
        this.checkstyleProjectService = checkstyleProjectService;
    }

    @Override
    public CheckStyleChecker createChecker(@Nullable final Module module,
                                           @NotNull final ConfigurationLocation location,
                                           final Map<String, String> properties,
                                           @NotNull final ClassLoader loaderOfCheckedCode) {
        return createChecker(module, location, properties, null, loaderOfCheckedCode);
    }

    @Override
    public CheckStyleChecker createChecker(@Nullable final Module module,
                                           @NotNull final ConfigurationLocation location,
                                           final Map<String, String> properties,
                                           @Nullable final TabWidthAndBaseDirProvider configurations,
                                           @NotNull final ClassLoader loaderOfCheckedCode) {
        return executeCommand(new OpCreateChecker(
                module, location, properties, configurations, loaderOfCheckedCode, checkstyleProjectService));
    }

    @Override
    public void destroyChecker(@NotNull final CheckstyleInternalObject checkerWithConfig) {
        executeCommand(new OpDestroyChecker(checkerWithConfig));
    }


    @Override
    public Map<PsiFile, List<Problem>> scan(@NotNull final CheckstyleInternalObject checkerWithConfig,
                                            @NotNull final List<ScannableFile> scannableFiles,
                                            final boolean isSuppressingErrors,
                                            final int tabWidth,
                                            final Optional<String> baseDir) {
        return executeCommand(new OpScan(checkerWithConfig, scannableFiles, isSuppressingErrors, tabWidth,
                baseDir));
    }


    @Override
    public CheckstyleInternalObject loadConfiguration(@NotNull final ConfigurationLocation inputFile,
                                                      final boolean ignoreVariables,
                                                      @Nullable final Map<String, String> variables) {
        OpLoadConfiguration cmd;
        if (ignoreVariables) {
            cmd = new OpLoadConfiguration(inputFile, checkstyleProjectService);
        } else {
            cmd = new OpLoadConfiguration(inputFile, variables, checkstyleProjectService);
        }
        return executeCommand(cmd);
    }

    @Override
    public CheckstyleInternalObject loadConfiguration(@NotNull final ConfigurationLocation inputFile,
                                                      @Nullable final Map<String, String> variables,
                                                      @Nullable final Module module) {
        return executeCommand(new OpLoadConfiguration(inputFile, variables, module, checkstyleProjectService));
    }

    @Override
    public CheckstyleInternalObject loadConfiguration(@NotNull final VirtualFile inputFile,
                                                      final boolean ignoreVariables,
                                                      @Nullable final Map<String, String> variables) {
        OpLoadConfiguration cmd;
        if (ignoreVariables) {
            cmd = new OpLoadConfiguration(inputFile, checkstyleProjectService);
        } else {
            cmd = new OpLoadConfiguration(inputFile, variables, checkstyleProjectService);
        }
        return executeCommand(cmd);
    }

    @Override
    public CheckstyleInternalObject loadConfiguration(@NotNull final String pXmlConfig) {
        return executeCommand(new OpLoadConfiguration(pXmlConfig, checkstyleProjectService));
    }


    @Override
    public void peruseConfiguration(@NotNull final CheckstyleInternalObject configuration,
                                    @NotNull final ConfigVisitor visitor) {
        executeCommand(new OpPeruseConfiguration(configuration, visitor));
    }


    private <R> R executeCommand(@NotNull final CheckstyleCommand<R> command) {
        R result;
        try {
            result = command.execute(project);
        } catch (CheckstyleException e) {
            CheckStylePluginException wrapped = new ExceptionWrapper().wrap(null, e);
            if (wrapped instanceof CheckStylePluginParseException) {
                throw wrapped;
            }
            throw new CheckstyleToolException(e);
        } catch (RuntimeException | ExceptionInInitializerError e) {
            CheckStylePluginException wrapped = new ExceptionWrapper().wrap(null, e);
            if (wrapped instanceof CheckStylePluginParseException) {
                throw wrapped;
            }
            final CheckstyleException csCause = digUpCheckstyleCause(e);
            if (csCause != null) {
                throw new CheckstyleToolException(csCause);
            } else {
                throw new CheckstyleServiceException("Error executing command '"
                        + command.getClass().getSimpleName() + "': " + e.getMessage(), e);
            }
        }
        return result;
    }


    @Nullable
    private CheckstyleException digUpCheckstyleCause(@Nullable final Throwable pThrowable) {
        CheckstyleException result = null;
        if (pThrowable != null) {
            if (pThrowable.getCause() instanceof CheckstyleException) {
                result = (CheckstyleException) pThrowable.getCause();
            } else {
                result = digUpCheckstyleCause(pThrowable.getCause());
            }
        }
        return result;
    }
}
