package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class ClasspathConfigurationLocation extends ConfigurationLocation {

    private static final Logger LOG = Logger.getInstance(ClasspathConfigurationLocation.class);

    private static final int CONTENT_CACHE_SECONDS = 2;
    private static final int ONE_SECOND = 1000;

    private byte[] cachedContent;
    private long cacheExpiry;

    ClasspathConfigurationLocation(final Project project) {
        super(ConfigurationType.PLUGIN_CLASSPATH, project);
    }

    @NotNull
    protected InputStream resolveFile() throws IOException {
        if (cachedContent != null && cacheExpiry > System.currentTimeMillis()) {
            return new ByteArrayInputStream(cachedContent);
        }

        try {
            cachedContent = asBytes(getLocation());
            cacheExpiry = System.currentTimeMillis() + (CONTENT_CACHE_SECONDS * ONE_SECOND);
            return new ByteArrayInputStream(cachedContent);

        } catch (IOException e) {
            LOG.info("Couldn't read file from classpath: " + getLocation(), e);
            cachedContent = null;
            cacheExpiry = 0;
            throw e;
        }
    }

    private byte[] asBytes(final String classpathLocation) throws IOException {
        CheckstyleProjectService checkstyleProjectService = ServiceManager.getService(getProject(), CheckstyleProjectService.class);

        InputStream resourceStream = checkstyleProjectService.underlyingClassLoader().getResourceAsStream(classpathLocation);
        if (resourceStream == null) {
            throw new FileNotFoundException("Couldn't read classpath resource: " + classpathLocation);
        }

        final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(responseBody));
             Reader reader = new BufferedReader(new InputStreamReader(resourceStream))) {
            int readChar;
            while ((readChar = reader.read()) != -1) {
                writer.write(readChar);
            }

            writer.flush();

            return responseBody.toByteArray();
        }
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new ClasspathConfigurationLocation(getProject()));
    }
}
