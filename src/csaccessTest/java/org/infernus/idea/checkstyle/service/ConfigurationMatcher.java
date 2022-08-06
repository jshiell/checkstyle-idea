package org.infernus.idea.checkstyle.service;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import static org.infernus.idea.checkstyle.service.cmd.CheckstyleBridge.messagesFrom;

public class ConfigurationMatcher extends TypeSafeMatcher<Configuration> {
    private static final int INDENT_SIZE = 2;

    private final Configuration expected;

    public ConfigurationMatcher(final Configuration expected) {
        this.expected = expected;
    }

    public static Matcher<? super Configuration> configEqualTo(final Configuration expected) {
        return new ConfigurationMatcher(expected);
    }

    protected boolean matchesSafely(final Configuration actual) {
        return equals(actual, expected);
    }

    private boolean equals(final Configuration config1, final Configuration config2) {
        if (config1 == config2) {
            return true;
        }
        return config1 != null
                && Objects.equals(config1.getName(), config2.getName())
                && config1.getChildren().length == config2.getChildren().length
                && Arrays.equals(config1.getAttributeNames(), config2.getAttributeNames())
                && !childrenAreNotEqual(config1, config2)
                && !attributesAreNotEqual(config1, config2)
                && !messagesAreNotEqual(config1, config2);
    }

    private boolean messagesAreNotEqual(final Configuration config1, final Configuration config2) {
        return !Objects.equals(messagesFrom(config1), messagesFrom(config2));
    }

    private boolean attributesAreNotEqual(final Configuration config1, final Configuration config2) {
        for (String attributeName : config1.getAttributeNames()) {
            if (!Objects.equals(attrValue(config1, attributeName), attrValue(config2, attributeName))) {
                return true;
            }
        }
        return false;
    }

    private boolean childrenAreNotEqual(final Configuration config1, final Configuration config2) {
        final Configuration[] sortedConfig1Children = sortedChildrenOf(config1);
        final Configuration[] sortedConfig2Children = sortedChildrenOf(config2);
        for (int i = 0; i < sortedConfig1Children.length; ++i) {
            if (!equals(sortedConfig1Children[i], sortedConfig2Children[i])) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private Configuration[] sortedChildrenOf(final Configuration config1) {
        final Configuration[] sortedConfigChildren = config1.getChildren();
        Arrays.sort(sortedConfigChildren, Comparator.comparing(Configuration::getName));
        return sortedConfigChildren;
    }

    @Override
    public void describeTo(final Description description) {
        description
                .appendText("equal to\n")
                .appendText(toString(expected, 1));
    }

    @Override
    protected void describeMismatchSafely(final Configuration actual, final Description mismatchDescription) {
        mismatchDescription
                .appendText("was\n")
                .appendText(toString(actual, 1));
    }

    private String toString(final Configuration configuration, final int indentLevel) {
        final StringBuilder out = new StringBuilder();
        out.append(indent(indentLevel)).append("<module name=\"").append(configuration.getName()).append("\"");
        for (String attrName : configuration.getAttributeNames()) {
            out.append(" ").append(attrName).append("=\"").append(attrValue(configuration, attrName)).append("\"");
        }
        if (configuration.getChildren().length > 0) {
            out.append(">\n");
            for (Configuration child : configuration.getChildren()) {
                out.append(toString(child, indentLevel + 1));
            }
            out.append(indent(indentLevel)).append("</module>\n");
        } else {
            out.append("/>\n");
        }
        return out.toString();
    }

    private String attrValue(final Configuration configuration, final String attributeName) {
        try {
            return configuration.getAttribute(attributeName);
        } catch (CheckstyleException e) {
            return "";
        }
    }

    private String indent(final int level) {
        return " ".repeat(Math.max(0, level * INDENT_SIZE));
    }
}
