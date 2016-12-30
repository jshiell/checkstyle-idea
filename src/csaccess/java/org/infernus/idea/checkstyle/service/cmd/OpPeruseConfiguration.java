package org.infernus.idea.checkstyle.service.cmd;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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


/**
 * Iterate on the configuration modules recursively, calling a visitor on each one.
 */
public class OpPeruseConfiguration
        implements CheckstyleCommand<Void>
{
    private static final String TOKENS_PROP = "tokens";

    private final Configuration configuration;

    private final ConfigVisitor visitor;


    public OpPeruseConfiguration(@NotNull final CheckstyleInternalObject pConfiguration, @NotNull final ConfigVisitor
            pVisitor) {
        if (!(pConfiguration instanceof HasCsConfig)) {
            throw new CheckstyleVersionMixException(HasCsConfig.class, pConfiguration);
        }
        configuration = ((HasCsConfig) pConfiguration).getConfiguration();
        visitor = pVisitor;
    }


    @Override
    public Void execute(@NotNull final Project pProject) throws CheckstyleException {

        runVisitor(configuration);
        return null;
    }


    private void runVisitor(@Nullable final Configuration pConfiguration) throws CheckstyleException {

        if (pConfiguration == null) {
            return;
        }
        final ConfigurationModule moduleInfo = buildModuleInfo(pConfiguration);
        if (moduleInfo != null) {
            visitor.visit(moduleInfo);
        }
        for (Configuration childConfig : pConfiguration.getChildren()) {
            runVisitor(childConfig);
        }
    }


    @Nullable
    private ConfigurationModule buildModuleInfo(@NotNull final Configuration pConfiguration) throws
            CheckstyleException {

        final String name = pConfiguration.getName();
        final Map<String, String> messages = pConfiguration.getMessages();

        final Map<String, String> properties = new HashMap<>();
        Set<KnownTokenTypes> knownTokenTypes = EnumSet.noneOf(KnownTokenTypes.class);
        for (String key : pConfiguration.getAttributeNames()) {
            if (key != null) {
                String value = pConfiguration.getAttribute(key);
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


    private Set<KnownTokenTypes> buildKnownTokenTypesSet(final String pValue) {

        final Set<KnownTokenTypes> result = EnumSet.noneOf(KnownTokenTypes.class);
        final String[] tokenStrings = pValue.split("\\s*,\\s*");
        for (String tokenStr : tokenStrings) {
            KnownTokenTypes knownToken = null;
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
