package org.infernus.idea.checkstyle.gradle;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.model.idea.IdeaModule;
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
        return Set.of(CheckstyleGradleModel.class);
    }

    @NotNull
    @Override
    public Set<Class<?>> getToolingExtensionsClasses() {
        return Set.of(CheckstyleGradleModelBuilder.class, CheckstyleGradleModel.class,
                CheckstyleGradleModelImpl.class);
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
