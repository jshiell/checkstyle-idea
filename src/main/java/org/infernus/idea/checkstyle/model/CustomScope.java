package org.infernus.idea.checkstyle.model;

public class CustomScope {

    private final static CustomScope DEFAULT = new CustomScope("default");

    private final String name;

    CustomScope(final String name) {
        this.name = name;
    }

    public static CustomScope getDefaultValue() {
        return DEFAULT;
    }

    public String getName() {
        return name;
    }
}
