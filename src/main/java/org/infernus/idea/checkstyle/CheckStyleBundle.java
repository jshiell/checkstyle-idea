package org.infernus.idea.checkstyle;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

public final class CheckStyleBundle {

    private static Reference<ResourceBundle> bundleReference;

    @NonNls
    private static final String BUNDLE = "org.infernus.idea.checkstyle.CheckStyleBundle";

    private CheckStyleBundle() {
        // utility class
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) final String key, final Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = null;

        if (bundleReference != null) {
            bundle = bundleReference.get();
        }

        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            bundleReference = new SoftReference<>(bundle);
        }

        return bundle;
    }

}
