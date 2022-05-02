package org.infernus.idea.checkstyle.exception;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * The service layer did not understand a {@link CheckstyleInternalObject} passed to it. This exception always
 * indicates a bug! The root cause can be a simple oversight (CheckstyleInternalObject is just a tagging interface,
 * so it may just be the wrong object), or the CheckstyleInternalObject was cached for too long, and the classloader
 * of the service layer has changed.
 * <p><b>Important:</b> Be sure to throw it <em>only</em> from the 'csaccess' sourceset!</p>
 */
public class CheckstyleVersionMixException extends CheckstyleServiceException {

    public CheckstyleVersionMixException(@NotNull final Class<? extends CheckstyleInternalObject> expectedClass,
                                         @Nullable final CheckstyleInternalObject actualObject) {
        super(buildMessage(expectedClass, actualObject));
    }


    @NotNull
    private static String buildMessage(@NotNull final Class<? extends CheckstyleInternalObject> expectedClass,
                                       @Nullable final CheckstyleInternalObject actualObject) {
        StringBuilder sb = new StringBuilder("internal error - A ");
        sb.append(CheckstyleInternalObject.class.getSimpleName());
        sb.append(" passed to the service layer could not be processed. Expected: ");
        sb.append(expectedClass.getName());
        sb.append(", actual: ");
        if (actualObject != null) {
            sb.append(actualObject.getClass().getName());
            sb.append(" [interfaces: ");
            for (Iterator<String> iter = getAllInterfaces(actualObject.getClass()).iterator(); iter.hasNext();) {
                sb.append(iter.next());
                if (iter.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        } else {
            sb.append("null");
        }
        sb.append(". ");
        sb.append("This is a bug. Either the wrong object (or null) was passed to the service layer, or a ");
        sb.append(CheckstyleInternalObject.class.getSimpleName());
        sb.append(" was cached for too long and the service layer's classloader has changed.");
        return sb.toString();
    }


    @NotNull
    private static SortedSet<String> getAllInterfaces(@Nullable final Class<?> theClass) {
        SortedSet<String> result = new TreeSet<>();
        for (Class<?> c = theClass; c != null; c = c.getSuperclass()) {
            for (Class<?> intf : c.getInterfaces()) {
                result.add(intf.getName());
                result.addAll(getAllInterfaces(intf));  // get super interfaces
            }
        }
        return result;
    }
}
