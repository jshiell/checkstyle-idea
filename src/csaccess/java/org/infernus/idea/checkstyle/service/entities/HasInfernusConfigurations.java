package org.infernus.idea.checkstyle.service.entities;

import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.service.Configurations;


public interface HasInfernusConfigurations
        extends CheckstyleInternalObject
{
    Configurations getConfigurations();
}
