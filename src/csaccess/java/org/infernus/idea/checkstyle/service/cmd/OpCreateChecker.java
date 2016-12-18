package org.infernus.idea.checkstyle.service.cmd;

import java.util.Map;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.Configurations;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.jetbrains.annotations.NotNull;


/**
 * Command which creates new {@link CheckStyleChecker}s.
 */
public class OpCreateChecker
        implements CheckstyleCommand<CheckStyleChecker>
{
    private final Module module;

    private final ConfigurationLocation location;

    private final Map<String, String> variables;


    public OpCreateChecker(final Module pModule, final ConfigurationLocation pLocation, final Map<String, String> pVariables) {
        module = pModule;
        location = pLocation;
        variables = pVariables;
    }


    @Override
    @NotNull
    public CheckStyleChecker execute(@NotNull final Project pProject) throws CheckstyleException {

        final Configuration config = loadConfig();

        final Checker checker = new Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.configure(config);

        CheckerWithConfig cwc = new CheckerWithConfig(checker, config);
        Configurations configurations = new Configurations(module);
        return new CheckStyleChecker(cwc, configurations.tabWidth(config), configurations.baseDir(config));
    }


    private Configuration loadConfig() throws CheckstyleException {
        return new OpLoadConfiguration(location, variables, module).execute(module.getProject()).getConfiguration();
    }
}
