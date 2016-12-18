package org.infernus.idea.checkstyle.csapi;


/**
 * Objects which implement this interface are keepers of objects internal to the Checkstyle tool, so from within the
 * normal plugin code we cannot know what they are. They are to be used from within the 'csaccess' source set only.
 * In this way, we can <em>store</em> objects from the Checkstyle tool itself in other parts of the plugin, although we
 * cannot <em>use</em> these objects.
 */
public interface CheckstyleInternalObject
{
    // tagging interface
}
