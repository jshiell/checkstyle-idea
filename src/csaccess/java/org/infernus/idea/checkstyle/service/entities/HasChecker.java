package org.infernus.idea.checkstyle.service.entities;

import com.puppycrawl.tools.checkstyle.Checker;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;

import java.util.concurrent.locks.Lock;


public interface HasChecker extends CheckstyleInternalObject {

    Checker getChecker();

    Lock getCheckerLock();

}
