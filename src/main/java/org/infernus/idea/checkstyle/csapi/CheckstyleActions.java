package org.infernus.idea.checkstyle.csapi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface CheckstyleActions {

    /**
     * Create a new Checkstyle checker.
     *
     * @param module              IntelliJ module
     * @param location            configuration location
     * @param properties          property values needed in the configuration file
     * @param loaderOfCheckedCode class loader which Checkstyle shall use to load classes and resources of the code
     *                            that it is checking - this is not for loading checks and modules, the module class
     *                            loader is used for that
     * @return the new checker
     */
    CheckStyleChecker createChecker(@Nullable Module module,
                                    @NotNull ConfigurationLocation location,
                                    @Nullable Map<String, String> properties,
                                    @NotNull ClassLoader loaderOfCheckedCode);

    /**
     * Create a new Checkstyle checker.
     *
     * @param module              IntelliJ module
     * @param location            configuration location
     * @param properties          property values needed in the configuration file
     * @param configurations      an internal object, intended for mocking in unit tests
     * @param loaderOfCheckedCode class loader which Checkstyle shall use to load classes and resources of the code
     *                            that it is checking - this is not for loading checks and modules, the module class
     *                            loader is used for that
     * @return the new checker
     */
    CheckStyleChecker createChecker(@Nullable Module module,
                                    @NotNull ConfigurationLocation location,
                                    @Nullable Map<String, String> properties,
                                    @Nullable TabWidthAndBaseDirProvider configurations,
                                    @NotNull ClassLoader loaderOfCheckedCode);


    /**
     * Destroy a checker.
     *
     * @param checkerWithConfig the checker along with its configuration
     */
    void destroyChecker(@NotNull CheckstyleInternalObject checkerWithConfig);


    /**
     * Run a Checkstyle scan with the given checker on the given files.
     *
     * @param checkerWithConfig   the checker along with its configuration
     * @param scannableFiles      the list of files to scan
     * @param isSuppressingErrors flag indicating whether errors should be suppressed (from plugin config)
     * @param tabWidth            number of characters per tab
     * @param baseDir             the base dir
     * @return list of problems per file
     */
    Map<PsiFile, List<Problem>> scan(@NotNull CheckstyleInternalObject checkerWithConfig,
                                     @NotNull List<ScannableFile> scannableFiles,
                                     boolean isSuppressingErrors,
                                     int tabWidth,
                                     Optional<String> baseDir);


    /**
     * Load a Checkstyle configuration file.
     *
     * @param inputFile       the file to load
     * @param ignoreVariables if <code>true</code>, all variables in the config file wil be replaced with the empty
     *                        String; if <code>false</code>, the variables will be filled from the given map
     * @param variables       variables to substitute in the loaded config file
     * @return a Checkstyle configuration object
     */
    CheckstyleInternalObject loadConfiguration(@NotNull ConfigurationLocation inputFile,
                                               boolean ignoreVariables,
                                               @Nullable Map<String, String> variables);

    /**
     * Load a Checkstyle configuration file, with variable substitution and path resolution.
     *
     * @param inputFile the file to load
     * @param variables variables to substitute in the loaded config file
     * @param module    the currently active module, used for resolving file paths
     * @return a Checkstyle configuration object
     */
    CheckstyleInternalObject loadConfiguration(@NotNull ConfigurationLocation inputFile,
                                               @Nullable Map<String, String> variables,
                                               @Nullable Module module);

    /**
     * Load a Checkstyle configuration file.
     *
     * @param inputFile       the file to load
     * @param ignoreVariables if <code>true</code>, all variables in the config file wil be replaced with the empty
     *                        String; if <code>false</code>, the variables will be filled from the given map
     * @param variables       variables to substitute in the loaded config file
     * @return a Checkstyle configuration object
     */
    CheckstyleInternalObject loadConfiguration(@NotNull VirtualFile inputFile,
                                               boolean ignoreVariables,
                                               @Nullable Map<String, String> variables);

    /**
     * Load a Checkstyle configuration file.
     *
     * @param pXmlConfig the file to load as UTF-8 encoded text
     * @return a Checkstyle configuration object
     */
    CheckstyleInternalObject loadConfiguration(@NotNull String pXmlConfig);


    /**
     * Traverse a Checkstyle configuration, calling the visitor on each module.
     *
     * @param configuration the Checkstyle configuration to traverse
     * @param visitor       the visitor to call
     */
    void peruseConfiguration(@NotNull CheckstyleInternalObject configuration,
                             @NotNull ConfigVisitor visitor);
}
