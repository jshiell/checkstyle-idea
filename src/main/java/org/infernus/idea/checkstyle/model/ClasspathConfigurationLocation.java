package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.infernus.idea.checkstyle.util.Streams.readContentOf;

public class ClasspathConfigurationLocation extends ConfigurationLocation {

    private static final Logger LOG = Logger.getInstance(ClasspathConfigurationLocation.class);

    private static final int CONTENT_CACHE_SECONDS = 2;
    private static final int ONE_SECOND = 1000;

    private byte[] cachedContent;
    private long cacheExpiry;

    ClasspathConfigurationLocation(@NotNull final Project project,
                                   @NotNull final String id) {
        super(id, ConfigurationType.PLUGIN_CLASSPATH, project);
    }

    @NotNull
    protected InputStream resolveFile(@NotNull ClassLoader checkstyleClassLoader) throws IOException {
        if (cachedContent != null && cacheExpiry > System.currentTimeMillis()) {
            return new ByteArrayInputStream(cachedContent);
        }

        try {
            cachedContent = readContentOf(streamOf(getLocation()));
            cacheExpiry = System.currentTimeMillis() + (CONTENT_CACHE_SECONDS * ONE_SECOND);
            return new ByteArrayInputStream(cachedContent);

        } catch (IOException e) {
            LOG.info("Couldn't read file from classpath: " + getLocation(), e);
            cachedContent = null;
            cacheExpiry = 0;
            throw e;
        }
    }

    private InputStream streamOf(final String classpathLocation) throws IOException {
        InputStream resourceStream = checkstyleClassLoader().getResourceAsStream(classpathLocation);
        if (resourceStream == null) {
            throw new FileNotFoundException("Couldn't read classpath resource: " + classpathLocation);
        }
        return resourceStream;
    }

    private ClassLoader checkstyleClassLoader() {
        return checkstyleProjectService(getProject()).underlyingClassLoader();
    }

    private CheckstyleProjectService checkstyleProjectService(@NotNull final Project project) {
        // we can't bring this in at construction time due to a cyclic dep
        return project.getService(CheckstyleProjectService.class);
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new ClasspathConfigurationLocation(getProject(), getId()));
    }
}
