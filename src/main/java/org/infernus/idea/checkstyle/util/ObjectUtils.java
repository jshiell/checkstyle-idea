package org.infernus.idea.checkstyle.util;

public class ObjectUtils {

    // ideally we'd use common-lang here, but I've learnt my lesson on relying on the IDEA CP

    public static boolean equals(final Object obj1, final Object obj2) {
        if (obj1 == obj2) {
            return true;

        } else if (obj1 == null || obj2 == null) {
            return false;
        }

        return obj1.equals(obj2);
    }

}
