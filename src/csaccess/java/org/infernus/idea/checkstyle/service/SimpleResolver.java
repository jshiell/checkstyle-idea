package org.infernus.idea.checkstyle.service;

import java.util.Map;

import com.puppycrawl.tools.checkstyle.PropertyResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class SimpleResolver implements PropertyResolver {

    private final Map<String, String> properties;

    public SimpleResolver(@NotNull final Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    @Nullable
    public String resolve(@Nullable final String name) {
        String result = null;
        if (name != null) {
            result = properties.get(name);
        }
        return result;
    }
}
