package org.infernus.idea.checkstyle.util;

public final class Exceptions {

    private Exceptions() {
    }

    public static Throwable rootCauseOf(final Throwable t) {
        if (t.getCause() != null && t.getCause() != t) {
            return rootCauseOf(t.getCause());
        }
        return t;
    }

}
