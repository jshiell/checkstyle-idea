package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.Configurations;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


/**
 * Command which creates new {@link CheckStyleChecker}s.
 */
public class OpCreateChecker
        implements CheckstyleCommand<CheckStyleChecker>
{
    private final Module module;

    private final ConfigurationLocation location;

    private final Map<String, String> variables;

    private final TabWidthAndBaseDirProvider configurations;

    private final ClassLoader loaderOfCheckedCode;


    public OpCreateChecker(@Nullable final Module pModule, @NotNull final ConfigurationLocation pLocation,
                           final Map<String, String> pVariables, @Nullable final TabWidthAndBaseDirProvider pConfigurations,
                           @NotNull final ClassLoader pLoaderOfCheckedCode) {
        module = pModule;
        location = pLocation;
        variables = pVariables;
        configurations = pConfigurations;
        loaderOfCheckedCode = pLoaderOfCheckedCode;
    }


    @Override
    @NotNull
    @SuppressWarnings("deprecation")  // setClassloader() must be used for backwards compatibility
    public CheckStyleChecker execute(@NotNull final Project pProject) throws CheckstyleException {

        final Configuration csConfig = loadConfig(pProject);

        final Checker checker = new Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());   // for Checkstyle to load modules (checks)
        checker.setClassloader(loaderOfCheckedCode);  // for checks to load the classes and resources to be analyzed
        checker.configure(csConfig);

        CheckerWithConfig cwc = new CheckerWithConfig(checker, csConfig);
        final TabWidthAndBaseDirProvider configs = configurations != null ? configurations : new Configurations
                (module, csConfig);
        return new CheckStyleChecker(cwc, configs.tabWidth(), configs.baseDir(), loaderOfCheckedCode);
    }


    private Configuration loadConfig(@NotNull final Project pProject) throws CheckstyleException {
        return new OpLoadConfiguration(location, variables, module).execute(pProject).getConfiguration();
    }
}
