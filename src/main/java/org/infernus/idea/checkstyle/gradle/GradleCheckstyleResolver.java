package org.infernus.idea.checkstyle.gradle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import java.util.Set;
import org.gradle.tooling.model.idea.IdeaModule;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModel;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModelBuilder;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;

/**
 * Spike (Increment 0): proves ServiceLoader discovery, classpath injection, and
 * {@link org.jetbrains.plugins.gradle.service.project.ProjectResolverContext#getExtraProject} all work
 * end to end. Increment 5 replaces this body with the real data-node attachment.
 */
public class GradleCheckstyleResolver extends AbstractProjectResolverExtension {

    private static final Logger LOG = Logger.getInstance(GradleCheckstyleResolver.class);

    @Override
    public void populateModuleExtraModels(@NotNull final IdeaModule gradleModule,
                                           @NotNull final DataNode<ModuleData> ideModule) {
        final CheckstyleGradleModel model = resolverCtx.getExtraProject(gradleModule, CheckstyleGradleModel.class);
        LOG.info("CheckstyleGradleModel for " + gradleModule.getName() + ": "
                + (model != null ? model.getConfigFilePath() : "null"));

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
}
