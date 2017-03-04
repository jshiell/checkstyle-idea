package org.infernus.idea.checkstyle.csapi;

import java.util.Optional;


public interface TabWidthAndBaseDirProvider {

    int tabWidth();

    Optional<String> baseDir();
}
