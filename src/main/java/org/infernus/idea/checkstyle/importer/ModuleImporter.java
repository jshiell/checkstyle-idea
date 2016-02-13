package org.infernus.idea.checkstyle.importer;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public abstract class ModuleImporter {
    private static final String TOKENS_PROP = "tokens";

    private int[] tokens;

    @NotNull
    protected CommonCodeStyleSettings getJavaSettings(@NotNull CodeStyleSettings settings) {
        return settings.getCommonSettings(JavaLanguage.INSTANCE);
    }

    public void setFrom(@NotNull Configuration moduleConfig) {
        for (String attrName : moduleConfig.getAttributeNames()) {
            try {
                handleAttribute(attrName, moduleConfig.getAttribute(attrName));
            } catch (CheckstyleException e) {
                // Ignore, shouldn't happen
            }
        }
    }

    protected boolean handleAttribute(@NotNull String attrName, @NotNull String attrValue) {
        if (TOKENS_PROP.equals(attrName)) {
            tokens = TokenSetUtil.getTokens(attrValue);
        }
        return false;
    }

    protected boolean appliesTo(int tokenId) {
        if (tokens != null) {
            for (int token : tokens) {
                if (token == tokenId) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    protected boolean appliesToOneOf(Set<Integer> tokenSet) {
        if (tokens != null) {
            for (int token : tokens) {
                if (tokenSet.contains(token)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    protected static Set<Integer> setOf(int... ids) {
        Set<Integer> tokenSet = new HashSet<>(ids.length);
        for (int id : ids) {
            tokenSet.add(id);
        }
        return tokenSet;
    }

    public abstract void importTo(@NotNull CodeStyleSettings settings);

    protected int getIntOrDefault(@NotNull String intStr, int defaultValue) {
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
