package org.infernus.idea.checkstyle.gradle;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.model.idea.IdeaModule;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModel;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModelBuilder;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;

/**
 * Reads the {@link CheckstyleGradleModel} built by {@link CheckstyleGradleModelBuilder} during Gradle
 * sync and attaches it to the module's data-node tree for {@link GradleCheckstyleDataService} to pick
 * up later. A child {@link CheckstyleGradleModuleData} node is attached unconditionally — even when no
 * model was built at all — so {@code GradleCheckstyleDataService} always has something to react to for
 * every Gradle module, including "nothing configured here" and "the checkstyle plugin removed" cases.
 *
 * <p>{@code AbstractProjectResolverExtension} is a chain-of-responsibility base: {@code super} must be
 * called after our own work so every other registered resolver extension still runs.
 */
public class GradleCheckstyleResolver extends AbstractProjectResolverExtension {

    private static final Logger LOG = Logger.getInstance(GradleCheckstyleResolver.class);

    @Override
    public void populateModuleExtraModels(@NotNull final IdeaModule gradleModule,
                                           @NotNull final DataNode<ModuleData> ideModule) {
        final CheckstyleGradleModel model = resolverCtx.getExtraProject(gradleModule, CheckstyleGradleModel.class);
        final String gradleProjectPath = gradleModule.getGradleProject().getPath();

        ideModule.createChild(CheckstyleGradleModuleData.KEY, toModuleData(gradleProjectPath, model));

        super.populateModuleExtraModels(gradleModule, ideModule);
    }

    @NotNull
    @Override
    public Set<Class<?>> getExtraProjectModelClasses() {
        return isGradleImportEnabled() ? Set.of(CheckstyleGradleModel.class) : Set.of();
    }

    @NotNull
    @Override
    public Set<Class<?>> getToolingExtensionsClasses() {
        return isGradleImportEnabled()
                ? Set.of(CheckstyleGradleModelBuilder.class, CheckstyleGradleModel.class,
                        CheckstyleGradleModelImpl.class)
                : Set.of();
    }

    /**
     * The model and tooling classes are only handed to Gradle's Tooling API when the project has opted in:
     * older Gradle versions inspect every registered class while serialising the sync action, so registering
     * them unconditionally puts them in the blast radius of every Gradle sync (#710). Must never throw, as
     * that would fail the sync of every Gradle project; any doubt means "not enabled".
     */
    private boolean isGradleImportEnabled() {
        try {
            if (resolverCtx == null) {
                return false;
            }
            final ExternalSystemTaskId taskId = resolverCtx.getExternalSystemTaskId();
            final Project project = taskId != null ? taskId.findProject() : null;
            if (project == null || project.isDisposed()) {
                return false;
            }
            final PluginConfigurationManager configurationManager = project.getService(PluginConfigurationManager.class);
            return configurationManager != null && configurationManager.getCurrent().isImportSettingsFromGradle();
        } catch (final RuntimeException e) {
            LOG.warn("Could not determine whether Gradle settings import is enabled; treating it as disabled", e);
            return false;
        }
    }

    @NotNull
    private static CheckstyleGradleModuleData toModuleData(@NotNull final String gradleProjectPath,
                                                             final CheckstyleGradleModel model) {
        if (model == null) {
            return new CheckstyleGradleModuleData(gradleProjectPath, null, Map.of(), null);
        }
        return new CheckstyleGradleModuleData(gradleProjectPath, model.getConfigFile(),
                model.getConfigProperties(), model.getToolVersion());
    }
}
