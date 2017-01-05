package org.infernus.idea.checkstyle.service.cmd;

import java.util.Map;

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


    public OpCreateChecker(@NotNull final Module pModule, final ConfigurationLocation pLocation, final Map<String,
            String> pVariables, @Nullable final TabWidthAndBaseDirProvider pConfigurations) {
        module = pModule;
        location = pLocation;
        variables = pVariables;
        configurations = pConfigurations;
    }


    @Override
    @NotNull
    public CheckStyleChecker execute(@NotNull final Project pProject) throws CheckstyleException {

        final Configuration csConfig = loadConfig();

        final Checker checker = new Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());
        checker.configure(csConfig);

        CheckerWithConfig cwc = new CheckerWithConfig(checker, csConfig);
        final TabWidthAndBaseDirProvider configs = configurations != null ? configurations : new Configurations
                (module, csConfig);
        return new CheckStyleChecker(cwc, configs.tabWidth(), configs.baseDir());
    }


    private Configuration loadConfig() throws CheckstyleException {
        return new OpLoadConfiguration(location, variables, module).execute(module.getProject()).getConfiguration();
    }
}
