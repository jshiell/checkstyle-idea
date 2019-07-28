package org.infernus.idea.checkstyle.service;

import java.util.Optional;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class Configurations implements TabWidthAndBaseDirProvider {

    private static final String TREE_WALKER_ELEMENT = "TreeWalker";
    private static final int DEFAULT_CHECKSTYLE_TAB_SIZE = 8;

    private final Module module;
    private final Configuration rootElement;

    public Configurations(@Nullable final Module module, @NotNull final Configuration rootElement) {
        this.module = module;
        this.rootElement = rootElement;
    }

    public int tabWidth() {
        for (final Configuration currentChild : rootElement.getChildren()) {
            if (TREE_WALKER_ELEMENT.equals(currentChild.getName())) {
                return intValueOrDefault(getAttributeOrNull(currentChild, "tabWidth"), defaultTabSize());
            }
        }
        return defaultTabSize();
    }

    private int defaultTabSize() {
        try {
            return currentCodeStyleSettings().getTabSize(JavaFileType.INSTANCE);
        } catch (AssertionError e) {
            // #278 - there appears to be a timing issue where the code style settings fetch will sometimes
            // fail on startup
            return DEFAULT_CHECKSTYLE_TAB_SIZE;
        }
    }

    @NotNull
    CodeStyleSettings currentCodeStyleSettings() {
        if (module != null) {
            return CodeStyle.getSettings(module.getProject());
        }
        return CodeStyle.getDefaultSettings();
    }

    public Optional<String> baseDir() {
        for (final String attributeName : rootElement.getAttributeNames()) {
            if ("basedir".equals(attributeName)) {
                return ofNullable(getAttributeOrNull(rootElement, "basedir"));
            }
        }
        return empty();
    }

    private int intValueOrDefault(final String value, final int defaultValue) {
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private String getAttributeOrNull(final Configuration element, final String attributeName) {
        try {
            return element.getAttribute(attributeName);
        } catch (CheckstyleException e) {
            return null;
        }
    }
}
