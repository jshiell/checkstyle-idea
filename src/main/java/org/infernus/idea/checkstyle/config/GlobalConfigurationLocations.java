package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves the IDE-wide (application-level) global rule locations stored in {@link ApplicationConfigurationState}
 * into {@link ConfigurationLocation}s instantiated in a given project's context, so path resolution and caching work
 * correctly.
 * <p>
 * This is the single place where a stored {@link ApplicationConfigurationState.GlobalConfigurationLocation} is turned
 * into a {@link ConfigurationLocation}. Callers choose which locations they want: {@link #activeLocations()} for the rules that
 * apply by default, {@link #availableLocations()} for pickers that offer every known location.
 */
public class GlobalConfigurationLocations {

    private static final Logger LOG = Logger.getInstance(GlobalConfigurationLocations.class);

    private final Project project;

    public GlobalConfigurationLocations(@NotNull final Project project) {
        this.project = project;
    }

    /**
     * The global locations that apply: only those marked active, and only when the global fallback
     * ("use global rules by default") is enabled.
     *
     * @return the active global locations, or an empty set if the fallback is disabled or none are active.
     */
    @NotNull
    public SortedSet<ConfigurationLocation> activeLocations() {
        final ApplicationConfigurationState appState = appState();
        if (!appState.isUseGlobalRulesByDefault()) {
            return Collections.emptySortedSet();
        }
        final List<String> activeIds = appState.getActiveGlobalLocationIds();
        if (activeIds.isEmpty()) {
            return Collections.emptySortedSet();
        }
        var globalLocations = appState.getGlobalLocations();
        return mapToProjectLocations(globalLocations, containsActiveLocation(activeIds))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Predicate that helps filter global configuration locations: retains only global configuration locations
     * that are enabled / active
     *
     * @param activeIds {@link List} of global location IDs that are active
     * @return {@link Predicate}
     */
    private static Predicate<ApplicationConfigurationState.GlobalConfigurationLocation> containsActiveLocation(List<String> activeIds) {
        return location -> activeIds.contains(location.id);
    }

    /**
     * Every global location, whether it is marked active or not, for pickers such as the tool window's rules
     * override. Like {@link #activeLocations()}, this is empty when the global fallback is disabled.
     *
     * @return all global locations, in their stored order.
     */
    @NotNull
    public List<ConfigurationLocation> availableLocations() {
        final ApplicationConfigurationState appState = appState();
        if (!appState.isUseGlobalRulesByDefault()) {
            return List.of();
        }
        var globalLocations = appState.getGlobalLocations();
        return mapToProjectLocations(globalLocations, isGlobalConfigurationLocation())
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Predicate that helps filter global configuration locations: include all global configuration locations -
     * whether enabled or not
     *
     * @return {@link Predicate}
     */
    private static Predicate<ApplicationConfigurationState.GlobalConfigurationLocation> isGlobalConfigurationLocation() {
        return globalConfigurationLocation -> true;
    }

    private Stream<ConfigurationLocation> mapToProjectLocations(
            @NotNull final List<ApplicationConfigurationState.GlobalConfigurationLocation> globalLocations,
            @NotNull final Predicate<ApplicationConfigurationState.GlobalConfigurationLocation> filter) {
        final ConfigurationLocationFactory factory = project.getService(ConfigurationLocationFactory.class);
        return globalLocations.stream()
                .filter(filter)
                .map(globalLocation -> toProjectLocation(factory, globalLocation))
                .flatMap(Optional::stream);
    }

    @NotNull
    private Optional<ConfigurationLocation> toProjectLocation(
            @NotNull final ConfigurationLocationFactory factory,
            @NotNull final ApplicationConfigurationState.GlobalConfigurationLocation globalLocation) {
        try {
            final ConfigurationType type = ConfigurationType.parse(globalLocation.type);
            if (type == null) {
                return Optional.empty();
            }
            final ConfigurationLocation location = factory.create(
                    project, globalLocation.id, type,
                    Objects.requireNonNullElse(globalLocation.location, "").trim(),
                    globalLocation.description, null);
            location.setNamedScope(NamedScopeHelper.getScopeByIdWithDefaultFallback(
                    project,
                    Objects.requireNonNullElse(globalLocation.scope, NamedScopeHelper.DEFAULT_SCOPE_ID)));
            if (globalLocation.properties != null) {
                location.setProperties(globalLocation.properties);
            }
            return Optional.of(location);
        } catch (Exception e) {
            LOG.error("Failed to deserialize global configuration location: " + globalLocation, e);
            return Optional.empty();
        }
    }

    @NotNull
    private static ApplicationConfigurationState appState() {
        return ApplicationManager.getApplication().getService(ApplicationConfigurationState.class);
    }
}