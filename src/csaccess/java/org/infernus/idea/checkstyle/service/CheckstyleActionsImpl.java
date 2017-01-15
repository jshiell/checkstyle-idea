package org.infernus.idea.checkstyle.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
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


public class CheckstyleActionsImpl
        implements CheckstyleActions
{
    private final Project project;


    public CheckstyleActionsImpl(@NotNull final Project pProject) {
        project = pProject;
    }


    @Override
    public CheckStyleChecker createChecker(@Nullable final Module pModule, @NotNull final ConfigurationLocation
            pLocation, final Map<String, String> pProperties, @NotNull final ClassLoader pLoaderOfCheckedCode) {
        return createChecker(pModule, pLocation, pProperties, null, pLoaderOfCheckedCode);
    }

    @Override
    public CheckStyleChecker createChecker(@Nullable final Module pModule, @NotNull final ConfigurationLocation
            pLocation, final Map<String, String> pProperties, @Nullable final TabWidthAndBaseDirProvider
            pConfigurations, @NotNull final ClassLoader pLoaderOfCheckedCode) {
        return executeCommand(
                new OpCreateChecker(pModule, pLocation, pProperties, pConfigurations, pLoaderOfCheckedCode));
    }

    @Override
    public void destroyChecker(@NotNull final CheckstyleInternalObject pCheckerWithConfig) {
        executeCommand(new OpDestroyChecker(pCheckerWithConfig));
    }


    @Override
    public Map<PsiFile, List<Problem>> scan(@NotNull final CheckstyleInternalObject pCheckerWithConfig, @NotNull
    final List<ScannableFile> pScannableFiles, final boolean pIsSuppressingErrors, final int pTabWidth, final
    Optional<String> pBaseDir) {
        return executeCommand(new OpScan(pCheckerWithConfig, pScannableFiles, pIsSuppressingErrors, pTabWidth,
                pBaseDir));
    }


    @Override
    public CheckstyleInternalObject loadConfiguration(@NotNull final ConfigurationLocation pInputFile, final boolean
            pIgnoreVariables, @Nullable final Map<String, String> pVariables) {
        OpLoadConfiguration cmd = null;
        if (pIgnoreVariables) {
            cmd = new OpLoadConfiguration(pInputFile);
        } else {
            cmd = new OpLoadConfiguration(pInputFile, pVariables);
        }
        return executeCommand(cmd);
    }

    @Override
    public CheckstyleInternalObject loadConfiguration(@NotNull final ConfigurationLocation pInputFile, @Nullable
    final Map<String, String> pVariables, @Nullable final Module pModule) {
        return executeCommand(new OpLoadConfiguration(pInputFile, pVariables, pModule));
    }

    @Override
    public CheckstyleInternalObject loadConfiguration(@NotNull final VirtualFile pInputFile, final boolean
            pIgnoreVariables, @Nullable final Map<String, String> pVariables) {
        OpLoadConfiguration cmd = null;
        if (pIgnoreVariables) {
            cmd = new OpLoadConfiguration(pInputFile);
        } else {
            cmd = new OpLoadConfiguration(pInputFile, pVariables);
        }
        return executeCommand(cmd);
    }

    @Override
    public CheckstyleInternalObject loadConfiguration(@NotNull final String pXmlConfig) {
        return executeCommand(new OpLoadConfiguration(pXmlConfig));
    }


    @Override
    public void peruseConfiguration(@NotNull final CheckstyleInternalObject pConfiguration, @NotNull final
    ConfigVisitor pVisitor) {
        executeCommand(new OpPeruseConfiguration(pConfiguration, pVisitor));
    }


    private <R> R executeCommand(@NotNull final CheckstyleCommand<R> pCommand) {

        R result = null;
        try {
            result = pCommand.execute(project);
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
                throw new CheckstyleServiceException("Error executing command '" + pCommand.getClass().getSimpleName
                        () + "': " + e.getMessage(), e);
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
