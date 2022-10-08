package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.infernus.idea.checkstyle.util.Strings.isBlank;

public class Descriptor {
    private final ConfigurationType configurationType;
    private final String location;
    private final String description;
    private final NamedScope scope;

    public static Descriptor parse(@NotNull final String descriptor,
                                   @NotNull final Project project) {
        if (isBlank(descriptor)) {
            throw new IllegalArgumentException("A non-blank representation is required");
        }

        final int typeSplitIndex = descriptor.indexOf(':');
        if (indexIsOutOfBounds(typeSplitIndex, descriptor)) {
            throw new IllegalArgumentException("Invalid string representation: " + descriptor);
        }
        final String typeString = descriptor.substring(0, typeSplitIndex);

        final int descriptionAndScopeSplitIndex = descriptor.lastIndexOf(':');
        if (typeSplitIndex == descriptionAndScopeSplitIndex
                || indexIsOutOfBounds(descriptionAndScopeSplitIndex, descriptor)) {
            throw new IllegalArgumentException("Invalid string representation: " + descriptor);
        }
        final String location = descriptor.substring(typeSplitIndex + 1, descriptionAndScopeSplitIndex);


        String description = "";
        String scopeString = "";
        if (descriptionAndScopeSplitIndex < (descriptor.length() - 1)) {
            String descriptionAndScope = descriptor.substring(descriptionAndScopeSplitIndex + 1);
            final String[] split = descriptionAndScope.split(";");
            description = split[0];
            scopeString = split.length < 2 ? NamedScopeHelper.DEFAULT_SCOPE_ID : split[1];
        }

        final ConfigurationType type = ConfigurationType.parse(typeString);
        final NamedScope scope = NamedScopeHelper.getScopeByIdWithDefaultFallback(project, scopeString);

        return new Descriptor(type, location, description, scope);
    }

    private static boolean indexIsOutOfBounds(final int index, final String descriptor) {
        return index <= 0 || index >= descriptor.length() - 1;
    }

    public static Descriptor of(@NotNull final ConfigurationLocation configurationLocation,
                                @NotNull final Project project) {
        return new Descriptor(configurationLocation.getType(),
                configurationLocation.getRawLocation(),
                configurationLocation.getDescription(),
                configurationLocation.getNamedScope().orElseGet(() -> NamedScopeHelper.getDefaultScope(project)));
    }

    @NotNull
    public Optional<ConfigurationLocation> findIn(@NotNull final Collection<ConfigurationLocation> locations,
                                                  @NotNull final Project project) {
        // This is a horrid hack to get around IDEA resolving $PROJECT_DIR$ in descriptors, hence we need to do the whole load
        // logic to make things consistent
        // Ideally we should switch from descriptors to GUIDs or similar to avoid such things, although we'd
        // still need the logic for legacy values
        Descriptor descriptorToFind = Descriptor.of(toConfigurationLocation(project), project);

        return locations.stream()
                .filter(it -> Descriptor.of(it, project).equals(descriptorToFind))
                .limit(1)
                .findFirst();
    }

    public Descriptor(@NotNull final ConfigurationType configurationType,
                      @NotNull final String location,
                      @NotNull final String description,
                      @NotNull final NamedScope scope) {
        this.configurationType = configurationType;
        this.location = location;
        this.description = description;
        this.scope = scope;
    }

    public @NotNull ConfigurationLocation toConfigurationLocation(@NotNull final Project project) {
        return configurationLocationFactory(project).create(
                project,
                UUID.randomUUID().toString(),
                configurationType,
                location,
                description,
                scope);
    }

    private ConfigurationLocationFactory configurationLocationFactory(final Project project) {
        return project.getService(ConfigurationLocationFactory.class);
    }

    public ConfigurationType getConfigurationType() {
        return configurationType;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public NamedScope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return configurationType + ":" + location + ":" + description + ";" + scope.getScopeId();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Descriptor that = (Descriptor) o;
        return configurationType == that.configurationType
                && Objects.equals(location, that.location)
                && Objects.equals(description, that.description)
                && Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurationType, location, description, scope);
    }
}
