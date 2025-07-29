package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
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
        implements CheckstyleCommand<CheckStyleChecker> {

    private final Module module;
    private final ConfigurationLocation location;
    private final Map<String, String> variables;
    private final TabWidthAndBaseDirProvider configurations;
    private final CheckstyleProjectService checkstyleProjectService;

    public OpCreateChecker(@Nullable final Module module,
                           @NotNull final ConfigurationLocation location,
                           final Map<String, String> variables,
                           @Nullable final TabWidthAndBaseDirProvider configurations,
                           @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this.module = module;
        this.location = location;
        this.variables = variables;
        this.configurations = configurations;
        this.checkstyleProjectService = checkstyleProjectService;
    }

    @Override
    @NotNull
    public CheckStyleChecker execute(@NotNull final Project project) throws CheckstyleException {

        final Configuration csConfig = loadConfig(project);

        final Checker checker = new Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());   // for Checkstyle to load modules (checks)

        try {
            checker.configure(csConfig);
        } catch (Error e) {
            // e.g. java.lang.NoClassDefFoundError thrown by Checkstyle for pre-8.0 custom checks
            throw new CheckstyleToolException(e);
        }

        CheckerWithConfig cwc = new CheckerWithConfig(checker, csConfig);
        final TabWidthAndBaseDirProvider configs = configurations != null
                ? configurations
                : new Configurations(module, csConfig);
        return new CheckStyleChecker(cwc, configs.tabWidth(), configs.baseDir(),
                checkstyleProjectService.getCheckstyleInstance(), location.getNamedScope());
    }

    private Configuration loadConfig(@NotNull final Project project) throws CheckstyleException {
        return new OpLoadConfiguration(location, variables, module, checkstyleProjectService).execute(project).getConfiguration();
    }
}
