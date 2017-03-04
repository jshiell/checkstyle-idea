package org.infernus.idea.checkstyle.service.entities;

import com.puppycrawl.tools.checkstyle.Checker;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;


public interface HasChecker extends CheckstyleInternalObject {

    Checker getChecker();

}
