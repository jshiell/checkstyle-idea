package org.infernus.idea.checkstyle.csapi;


/**
 * Objects which implement this interface are keepers of objects internal to the Checkstyle tool, so from within the
 * normal plugin code we cannot know what they are. They are to be used from within the 'csaccess' source set only.
 * In this way, we can <em>store</em> objects from the Checkstyle tool itself in other parts of the plugin, although we
 * cannot <em>use</em> these objects.
 * <p>It is important to make sure that these objects do not outlive the classloader that loaded them (at least not by
 * much). When the Checkstyle version is changed in the configuration, all objects which implement this interface
 * must be discarded. The new classloader with the new Checkstyle version would not be able to use them and
 * {@link org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException CheckstyleVersionMixException}s would
 * result.</p>
 */
public interface CheckstyleInternalObject {
    // tagging interface
}
