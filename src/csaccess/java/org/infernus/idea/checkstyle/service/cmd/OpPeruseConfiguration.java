package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.csapi.ConfigVisitor;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.csapi.KnownTokenTypes;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.entities.HasCsConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.infernus.idea.checkstyle.service.cmd.CheckstyleBridge.messagesFrom;


/**
 * Iterate on the configuration modules recursively, calling a visitor on each one.
 */
public class OpPeruseConfiguration implements CheckstyleCommand<Void> {

    private static final String TOKENS_PROP = "tokens";

    private final Configuration configuration;
    private final ConfigVisitor visitor;

    public OpPeruseConfiguration(@NotNull final CheckstyleInternalObject configuration,
                                 @NotNull final ConfigVisitor visitor) {
        if (!(configuration instanceof HasCsConfig)) {
            throw new CheckstyleVersionMixException(HasCsConfig.class, configuration);
        }
        this.configuration = ((HasCsConfig) configuration).getConfiguration();
        this.visitor = visitor;
    }


    @Override
    public Void execute(@NotNull final Project project) throws CheckstyleException {
        runVisitor(configuration);
        return null;
    }


    private void runVisitor(@Nullable final Configuration currentConfig) throws CheckstyleException {
        if (currentConfig == null) {
            return;
        }
        final ConfigurationModule moduleInfo = buildModuleInfo(currentConfig);
        if (moduleInfo != null) {
            visitor.visit(moduleInfo);
        }
        for (Configuration childConfig : currentConfig.getChildren()) {
            runVisitor(childConfig);
        }
    }


    @Nullable
    private ConfigurationModule buildModuleInfo(@NotNull final Configuration currentConfig)
            throws CheckstyleException {
        final String name = currentConfig.getName();
        final Map<String, String> messages = messagesFrom(currentConfig);

        final Map<String, String> properties = new HashMap<>();
        Set<KnownTokenTypes> knownTokenTypes = EnumSet.noneOf(KnownTokenTypes.class);
        for (String key : currentConfig.getAttributeNames()) {
            if (key != null) {
                String value = currentConfig.getAttribute(key);
                if (value != null) {
                    if (TOKENS_PROP.equals(key)) {
                        knownTokenTypes = buildKnownTokenTypesSet(value);
                    } else {
                        properties.put(key, value);
                    }
                }
            }
        }

        ConfigurationModule result = null;
        if (name != null) {
            result = new ConfigurationModule(name, properties, knownTokenTypes, messages);
        }
        return result;
    }


    private Set<KnownTokenTypes> buildKnownTokenTypesSet(final String value) {

        final Set<KnownTokenTypes> result = EnumSet.noneOf(KnownTokenTypes.class);
        final String[] tokenStrings = value.split("\\s*,\\s*");
        for (String tokenStr : tokenStrings) {
            KnownTokenTypes knownToken;
            try {
                knownToken = KnownTokenTypes.valueOf(tokenStr);
            } catch (IllegalArgumentException e) {
                knownToken = null;
            }
            if (knownToken != null) {
                result.add(knownToken);
            }
        }
        return result;
    }
}
