package org.infernus.idea.checkstyle.csapi;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Provider that providers the additional bundled checkstyle configs
 */
public interface BundledConfigProvider {

    /**
     * @return the configs
     */
    @NotNull
    Collection<BasicConfig> getConfigs();

    class BasicConfig {
        private final String id;
        private final String path;
        private final String description;

        public BasicConfig(final String id, final String description, final String path) {
            this.id = requiredNotBlank(id, "id");
            this.path = requiredNotBlank(path, "path");
            this.description = requiredNotBlank(description, "description");
        }

        static String requiredNotBlank(final String val, final String name) {
            if (StringUtils.isBlank(val)) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return val;
        }

        public String getId() {
            return id;
        }

        public String getPath() {
            return path;
        }

        public String getDescription() {
            return description;
        }
    }
}
