package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class FileTabCharacterImporter extends ModuleImporter {
    private final static String FILE_EXTENSIONS_PROP = "fileExtensions";
    private String[] extensions;
    
    @Override
    protected boolean handleAttribute(@NotNull final String attrName, @NotNull final String attrValue) {
        if (FILE_EXTENSIONS_PROP.equals(attrName)) {
            extensions = attrValue.split("\\s*,\\s*");
            return true;
        }
        return false;
    }

    @Override
    public void importTo(@NotNull final CodeStyleSettings settings) {
        if (extensions != null) {
            for (String extension : extensions) {
                if (!extension.isEmpty()) {
                    FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension);
                    setNoTabChar(settings, fileType);
                }
            }
        }
        else {
            for (FileType fileType : FileTypeManager.getInstance().getRegisteredFileTypes()) {
                setNoTabChar(settings, fileType);
            }
        }
    }

    private void setNoTabChar(final @NotNull CodeStyleSettings settings, final FileType fileType) {
        CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptions(fileType);
        if (indentOptions != null) {
            indentOptions.USE_TAB_CHARACTER = false;
        }
    }
}
