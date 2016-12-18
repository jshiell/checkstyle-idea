package org.infernus.idea.checkstyle;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.csapi.ConfigVisitor;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface CheckstyleActions
{
    /**
     * Create a new Checkstyle checker.
     *
     * @param pModule IntelliJ module
     * @param pLocation configuration location
     * @param pProperties property values needed in the configuration file
     * @return the new checker
     */
    CheckStyleChecker createChecker(final Module pModule, final ConfigurationLocation pLocation, final Map<String,
            String> pProperties);


    /**
     * Destroy a checker.
     *
     * @param pCheckerWithConfig the checker along with its configuration
     */
    void destroyChecker(@NotNull final CheckstyleInternalObject pCheckerWithConfig);


    /**
     * Run a Checkstyle scan with the given checker on the given files.
     *
     * @param pCheckerWithConfig the checker along with its configuration
     * @param pScannableFiles the list of files to scan
     * @param pIsSuppressingErrors flag indicating whether errors should be suppressed (from plugin config)
     * @param pTabWidth number of characters per tab
     * @param pBaseDir the base dir
     * @return list of problems per file
     */
    Map<PsiFile, List<Problem>> scan(@NotNull final CheckstyleInternalObject pCheckerWithConfig, @NotNull final
    List<ScannableFile> pScannableFiles, final boolean pIsSuppressingErrors, final int pTabWidth, final
    Optional<String> pBaseDir);


    /**
     * Load a Checkstyle configuration file.
     *
     * @param pInputFile the file to load
     * @param pIgnoreVariables if <code>true</code>, all variables in the config file wil be replaced with the empty
     * String; if <code>false</code>, the variables will be filled from the given map
     * @param pVariables variables to substitute in the loaded config file
     * @return a Checkstyle configuration object
     */
    CheckstyleInternalObject loadConfiguration(@NotNull final ConfigurationLocation pInputFile, final boolean
            pIgnoreVariables, @Nullable final Map<String, String> pVariables);


    /**
     * Load a Checkstyle configuration file, with variable substitution and path resolution.
     *
     * @param pInputFile the file to load
     * @param pVariables variables to substitute in the loaded config file
     * @param pModule the currently active module, used for resolving file paths
     * @return a Checkstyle configuration object
     */
    CheckstyleInternalObject loadConfiguration(@NotNull final ConfigurationLocation pInputFile, @Nullable final
    Map<String, String> pVariables, @NotNull final Module pModule);

    /**
     * Load a Checkstyle configuration file.
     *
     * @param pInputFile the file to load
     * @param pIgnoreVariables if <code>true</code>, all variables in the config file wil be replaced with the empty
     * String; if <code>false</code>, the variables will be filled from the given map
     * @param pVariables variables to substitute in the loaded config file
     * @return a Checkstyle configuration object
     */
    CheckstyleInternalObject loadConfiguration(@NotNull final VirtualFile pInputFile, final boolean pIgnoreVariables,
                                               @Nullable final Map<String, String> pVariables);

    /**
     * Load a Checkstyle configuration file.
     *
     * @param pXmlConfig the file to load as UTF-8 encoded text
     * @return a Checkstyle configuration object
     */
    CheckstyleInternalObject loadConfiguration(@NotNull final String pXmlConfig);


    /**
     * Traverse a Checkstyle configuration, calling the visitor on each module.
     *
     * @param pConfiguration the Checkstyle configuration to traverse
     * @param pVisitor the visitor to call
     */
    void peruseConfiguration(@NotNull final CheckstyleInternalObject pConfiguration, @NotNull final ConfigVisitor
            pVisitor);
}
