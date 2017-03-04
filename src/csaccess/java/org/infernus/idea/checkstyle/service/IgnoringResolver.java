package org.infernus.idea.checkstyle.service;

import com.puppycrawl.tools.checkstyle.PropertyResolver;

/**
 * PropertyResolver that resolves everything to the empty String.
 */
public class IgnoringResolver implements PropertyResolver {

    @Override
    public String resolve(final String pName) {
        return "";
    }

}
