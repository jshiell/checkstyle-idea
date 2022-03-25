package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.exception.CheckstyleServiceException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.Configurations;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private final ClassLoader loaderOfCheckedCode;
    private final CheckstyleProjectService checkstyleProjectService;

    public OpCreateChecker(@Nullable final Module module,
                           @NotNull final ConfigurationLocation location,
                           final Map<String, String> variables,
                           @Nullable final TabWidthAndBaseDirProvider configurations,
                           @NotNull final ClassLoader loaderOfCheckedCode,
                           @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this.module = module;
        this.location = location;
        this.variables = variables;
        this.configurations = configurations;
        this.loaderOfCheckedCode = loaderOfCheckedCode;
        this.checkstyleProjectService = checkstyleProjectService;
    }

    @Override
    @NotNull
    public CheckStyleChecker execute(@NotNull final Project project) throws CheckstyleException {

        final Configuration csConfig = loadConfig(project);

        final Checker checker = new Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());   // for Checkstyle to load modules (checks)
        setClassLoader(checker, loaderOfCheckedCode); // for checks to load the classes and resources to be analyzed

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

    private void setClassLoader(final Checker checker, final ClassLoader classLoader) {
        try {
            Method classLoaderMethod = null;
            try {
                classLoaderMethod = Checker.class.getMethod("setClassloader", ClassLoader.class);
            } catch (NoSuchMethodException | SecurityException e) {
                try {
                    classLoaderMethod = Checker.class.getMethod("setClassLoader", ClassLoader.class); // 8.0 and above
                } catch (NoSuchMethodException | SecurityException ignored) {
                    // it is a no-op from 8.27, and removed entirely in 8.31
                }
            }

            if (classLoaderMethod != null) {
                classLoaderMethod.invoke(checker, classLoader);
            }

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new CheckstyleServiceException("Failed to set classloader", e);
        }
    }

    private Configuration loadConfig(@NotNull final Project project) throws CheckstyleException {
        return new OpLoadConfiguration(location, variables, module, checkstyleProjectService).execute(project).getConfiguration();
    }
}
