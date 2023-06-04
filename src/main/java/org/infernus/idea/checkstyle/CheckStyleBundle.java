package org.infernus.idea.checkstyle;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public final class CheckStyleBundle extends AbstractBundle {

    @NonNls
    private static final String BUNDLE = "org.infernus.idea.checkstyle.CheckStyleBundle";
    private static final CheckStyleBundle INSTANCE = new CheckStyleBundle();

    private CheckStyleBundle() {
        super(BUNDLE);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) final String key, final Object... params) {
        return INSTANCE.getMessage(key, params);
    }

}
