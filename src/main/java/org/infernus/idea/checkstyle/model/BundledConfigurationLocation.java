package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

import static org.infernus.idea.checkstyle.util.Streams.inMemoryCopyOf;


public class BundledConfigurationLocation extends ConfigurationLocation {
    @NotNull
    private final BundledConfig bundledConfig;

    BundledConfigurationLocation(@NotNull final BundledConfig bundledConfig,
                                 @NotNull final Project project) {
        super(bundledConfig.getId(), ConfigurationType.BUNDLED, project);
        super.setLocation(bundledConfig.getLocation());
        super.setDescription(bundledConfig.getDescription());

        this.bundledConfig = bundledConfig;
    }

    @NotNull
    public BundledConfig getBundledConfig() {
        return bundledConfig;
    }

    @Override
    public void setLocation(final String location) {
        // do nothing, we always use the hard-coded location
    }

    @Override
    public void setDescription(@Nullable final String description) {
        // do nothing, we always use the hard-coded description
    }

    @Override
    @NotNull
    protected InputStream resolveFile(@NotNull final ClassLoader checkstyleClassLoader) throws IOException {
        try {
            InputStream source = checkstyleClassLoader.loadClass("com.puppycrawl.tools.checkstyle.Checker").getResourceAsStream(bundledConfig.getPath());
            if (source == null) {
                throw new IOException("Could not read " + bundledConfig.getPath() + " from classpath");
            }
            return inMemoryCopyOf(source);
        } catch (ClassNotFoundException e) {
            throw new IOException("Couldn't find Checkstyle on classpath", e);
        }
    }

    public boolean isRemovable() {
        return false;
    }


    @Override
    @NotNull
    public BundledConfigurationLocation clone() {
        return new BundledConfigurationLocation(bundledConfig, getProject());
    }
}
