package org.infernus.idea.checkstyle.importer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.csapi.KnownTokenTypes;
import org.jetbrains.annotations.NotNull;

public abstract class ModuleImporter {

    private Set<KnownTokenTypes> tokens;

    @NotNull
    protected CommonCodeStyleSettings getJavaSettings(@NotNull final CodeStyleSettings settings) {
        return settings.getCommonSettings(JavaLanguage.INSTANCE);
    }

    public void setFrom(@NotNull final ConfigurationModule moduleConfig) {
        tokens = moduleConfig.getKnownTokenTypes();
        for (Map.Entry<String, String> entry : moduleConfig.getProperties().entrySet()) {
            handleAttribute(entry.getKey(), entry.getValue());
        }
    }

    protected abstract void handleAttribute(@NotNull String attrName, @NotNull String attrValue);


    protected boolean appliesTo(final KnownTokenTypes token) {
        return tokens == null || tokens.isEmpty() || tokens.contains(token);
    }

    protected boolean appliesToOneOf(final Set<KnownTokenTypes> tokenSet) {
        return tokens == null || tokens.isEmpty() || !Collections.disjoint(tokens, tokenSet);
    }


    public abstract void importTo(@NotNull CodeStyleSettings settings);


    protected int getIntOrDefault(@NotNull final String intStr, final int defaultValue) {
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
