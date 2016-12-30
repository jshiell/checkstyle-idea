package org.infernus.idea.checkstyle.service.cmd;

import java.util.Map;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.Configurations;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.infernus.idea.checkstyle.service.entities.HasInfernusConfigurations;
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

    private final Configurations configurations;


    public OpCreateChecker(@NotNull final Module pModule, final ConfigurationLocation pLocation, final Map<String,
            String> pVariables) {
        this(pModule, pLocation, pVariables, null);
    }

    public OpCreateChecker(@NotNull final Module pModule, final ConfigurationLocation pLocation, final Map<String,
            String> pVariables, @Nullable final CheckstyleInternalObject pConfigurations) {
        module = pModule;
        location = pLocation;
        variables = pVariables;

        if (pConfigurations != null) {
            if (!(pConfigurations instanceof HasInfernusConfigurations)) {
                throw new CheckstyleVersionMixException(HasInfernusConfigurations.class, pConfigurations);
            }
            configurations = ((HasInfernusConfigurations) pConfigurations).getConfigurations();
        } else {
            configurations = null;
        }
    }


    @Override
    @NotNull
    public CheckStyleChecker execute(@NotNull final Project pProject) throws CheckstyleException {

        final Configuration csConfig = loadConfig();

        final Checker checker = new Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.configure(csConfig);

        CheckerWithConfig cwc = new CheckerWithConfig(checker, csConfig);
        final Configurations configs = configurations != null ? configurations : new Configurations(module);
        return new CheckStyleChecker(cwc, configs.tabWidth(csConfig), configs.baseDir(csConfig));
    }


    private Configuration loadConfig() throws CheckstyleException {
        return new OpLoadConfiguration(location, variables, module).execute(module.getProject()).getConfiguration();
    }
}
