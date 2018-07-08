package org.infernus.idea.checkstyle.service;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


/**
 * Describes how rules may be passed to the {@link org.infernus.idea.checkstyle.service.cmd.OpLoadConfiguration}
 * command.
 */
public interface RulesContainer
{
    /**
     * Getter.
     *
     * @return the file path to where the rules are stored
     */
    @Nullable
    String filePath();

    /**
     * Creates an input stream to the rules file.
     *
     * @return input stream
     * @throws IOException failed creating the stream
     * @param checkstyleClassLoader
     */
    InputStream inputStream(ClassLoader checkstyleClassLoader) throws IOException;

    @Nullable
    default String resolveAssociatedFile(@Nullable final String fileName,
                                         @NotNull final Project project,
                                         @Nullable final Module module,
                                         @NotNull final ClassLoader checkstyleClassLoader) throws IOException {
        return null;
    }


    class ConfigurationLocationRulesContainer implements RulesContainer {
        private final ConfigurationLocation configurationLocation;

        public ConfigurationLocationRulesContainer(final ConfigurationLocation configurationLocation) {
            this.configurationLocation = configurationLocation;
        }

        @Override
        public String filePath() {
            return configurationLocation.getLocation();
        }

        @Override
        public InputStream inputStream(@NotNull final ClassLoader checkstyleClassLoader) throws IOException {
            return configurationLocation.resolve(checkstyleClassLoader);
        }

        public String resolveAssociatedFile(final String fileName,
                                            @NotNull final Project project,
                                            final Module module,
                                            @NotNull final ClassLoader checkstyleClassLoader) throws IOException {
            return configurationLocation.resolveAssociatedFile(fileName, module, checkstyleClassLoader);
        }
    }


    class VirtualFileRulesContainer implements RulesContainer {
        private final VirtualFile virtualFile;

        public VirtualFileRulesContainer(final VirtualFile virtualFile) {
            this.virtualFile = virtualFile;
        }

        @Override
        public String filePath() {
            return virtualFile.getPath();
        }

        @Override
        public InputStream inputStream(ClassLoader checkstyleClassLoader) throws IOException {
            return virtualFile.getInputStream();
        }
    }


    class ContentRulesContainer implements RulesContainer {
        private final String content;

        public ContentRulesContainer(final String content) {
            this.content = content;
        }

        @Override
        public String filePath() {
            return null;
        }

        @Override
        public InputStream inputStream(ClassLoader checkstyleClassLoader) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
    }


    class BundledRulesContainer implements RulesContainer {
        private final BundledConfig bundledConfig;

        public BundledRulesContainer(@NotNull final BundledConfig bundledConfig) {
            this.bundledConfig = bundledConfig;
        }

        @Override
        @Nullable
        public String filePath() {
            return "(bundled " + bundledConfig.getPath() + ")";
        }

        @Override
        public InputStream inputStream(ClassLoader checkstyleClassLoader) {
            // using the csaccess classloader:
            return getClass().getResourceAsStream(bundledConfig.getPath());
        }
    }
}
