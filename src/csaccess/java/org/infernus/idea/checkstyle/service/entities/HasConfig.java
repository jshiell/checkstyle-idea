package org.infernus.idea.checkstyle.service.entities;

import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;


public interface HasConfig
    extends CheckstyleInternalObject
{
    Configuration getConfiguration();
}
