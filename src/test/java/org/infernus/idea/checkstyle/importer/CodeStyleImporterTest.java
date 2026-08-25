package org.infernus.idea.checkstyle.importer;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.*;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementSettings;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.*;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CodeStyleImporterTest
        extends LightPlatformTestCase {
    private CodeStyleSettings codeStyleSettings;
    private CommonCodeStyleSettings javaSettings;

    private final Project project = mock(Project.class);
    private CheckstyleProjectService csService = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PluginConfigurationManager mockPluginConfig = mock(PluginConfigurationManager.class);
        final PluginConfiguration mockConfigDto = PluginConfigurationBuilder.testInstance("8.0").build();
        when(mockPluginConfig.getCurrent()).thenReturn(mockConfigDto);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(mockPluginConfig);

        csService = new CheckstyleProjectService(project);

        codeStyleSettings = CodeStyleSettingsManager.createTestSettings(CodeStyleSettings.getDefaults());
        javaSettings = codeStyleSettings.getCommonSettings(JavaLanguage.INSTANCE);
    }

    private static final String FILE_PREFIX =
            """
                    <?xml version="1.0"?>
                    <!DOCTYPE module PUBLIC
                              "-//Puppy Crawl//DTD Check Configuration 1.3//EN"
                              "http://www.puppycrawl.com/dtds/configuration_1_3.dtd">
                    <module name = "Checker">
                    """;
    private static final String FILE_SUFFIX =
            "</module>";

    private CheckStyleCodeStyleImporter importer;

    private void importConfiguration(@NotNull final String configuration) {
        String fullConfiguration = FILE_PREFIX + configuration + FILE_SUFFIX;

        importer = new CheckStyleCodeStyleImporter(csService);
        importer.importConfiguration(csService, loadConfiguration(fullConfiguration), codeStyleSettings);
    }

    private String inTreeWalker(@NotNull final String configuration) {
        return "<module name=\"TreeWalker\">" + configuration + "</module>";
    }

    private CheckstyleInternalObject loadConfiguration(@NotNull final String configuration) {
        return csService.getCheckstyleInstance().loadConfiguration(configuration);
    }

    public void testImportRightMargin() {
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="LineLength">
                                    <property name="max" value="100"/>
                                </module>"""
                )
        );
        assertEquals(100, javaSettings.RIGHT_MARGIN);
        assertTrue(javaSettings.WRAP_LONG_LINES);
        assertNull(importer.getAdditionalImportInfo(mock(CodeStyleScheme.class)));
    }

    public void testEmptyLineSeparator() {
        javaSettings.BLANK_LINES_AROUND_FIELD = 0;
        javaSettings.BLANK_LINES_AROUND_METHOD = 0;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="EmptyLineSeparator">
                                    <property name="tokens" value="VARIABLE_DEF, METHOD_DEF"/>
                                </module>"""
                )
        );
        assertEquals(1, javaSettings.BLANK_LINES_AROUND_FIELD);
        assertEquals(1, javaSettings.BLANK_LINES_AROUND_METHOD);
    }

    public void testImportFileTabCharacter() {
        CommonCodeStyleSettings xmlSettings = codeStyleSettings.getCommonSettings(XMLLanguage.INSTANCE);
        CommonCodeStyleSettings.IndentOptions javaIndentOptions = javaSettings.getIndentOptions();
        assertNotNull(javaIndentOptions);
        CommonCodeStyleSettings.IndentOptions xmlIndentOptions = xmlSettings.getIndentOptions();
        assertNotNull(xmlIndentOptions);
        javaIndentOptions.USE_TAB_CHARACTER = true;
        xmlIndentOptions.USE_TAB_CHARACTER = true;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="FileTabCharacter">
                                    <property name="eachLine" value="true" />
                                    <property name="fileExtensions" value="java,xml" />
                                </module>"""
                )
        );
        assertFalse(javaIndentOptions.USE_TAB_CHARACTER);
        assertFalse(xmlIndentOptions.USE_TAB_CHARACTER);
    }

    public void testImportFileTabCharacterNoExplicitExtensions() {
        CommonCodeStyleSettings xmlSettings = codeStyleSettings.getCommonSettings(XMLLanguage.INSTANCE);
        CommonCodeStyleSettings.IndentOptions javaIndentOptions = javaSettings.getIndentOptions();
        assertNotNull(javaIndentOptions);
        CommonCodeStyleSettings.IndentOptions xmlIndentOptions = xmlSettings.getIndentOptions();
        assertNotNull(xmlIndentOptions);
        javaIndentOptions.USE_TAB_CHARACTER = true;
        xmlIndentOptions.USE_TAB_CHARACTER = true;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="FileTabCharacter"/>
                                """
                )
        );
        assertFalse(javaIndentOptions.USE_TAB_CHARACTER);
        assertFalse(xmlIndentOptions.USE_TAB_CHARACTER);
    }

    public void testImportWhitespaceAfter() {
        javaSettings.SPACE_AFTER_COMMA = false;
        javaSettings.SPACE_AFTER_SEMICOLON = false;
        javaSettings.SPACE_AFTER_TYPE_CAST = false;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="WhitespaceAfter">
                                    <property name="tokens" value="COMMA, SEMI"/>
                                </module>"""
                )
        );
        assertTrue(javaSettings.SPACE_AFTER_COMMA);
        assertTrue(javaSettings.SPACE_AFTER_SEMICOLON);
        assertFalse(javaSettings.SPACE_AFTER_TYPE_CAST);
    }

    public void testImportWhitespaceAround() {
        javaSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS = false;
        javaSettings.SPACE_AROUND_EQUALITY_OPERATORS = false;
        javaSettings.SPACE_AROUND_BITWISE_OPERATORS = false;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="WhitespaceAround">
                                    <property name="tokens" value="ASSIGN"/>
                                    <property name="tokens" value="EQUAL"/>
                                </module>"""
                )
        );
        assertTrue(javaSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS);
        assertTrue(javaSettings.SPACE_AROUND_EQUALITY_OPERATORS);
        assertFalse(javaSettings.SPACE_AROUND_BITWISE_OPERATORS);
    }

    public void testNoWhitespaceBeforeImporter() {
        javaSettings.SPACE_BEFORE_SEMICOLON = true;
        javaSettings.SPACE_BEFORE_COMMA = true;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"NoWhitespaceBefore\"/>"
                )
        );
        assertFalse(javaSettings.SPACE_BEFORE_SEMICOLON);
        assertFalse(javaSettings.SPACE_BEFORE_COMMA);
    }

    public void testLeftCurlyImporter() {
        javaSettings.CLASS_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        javaSettings.METHOD_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        javaSettings.BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="LeftCurly">
                                    <property name="option" value="nl"/>
                                    <property name="tokens" value="CLASS_DEF,INTERFACE_DEF"/>
                                </module>
                                <module name="LeftCurly">
                                    <property name="option" value="eol"/>
                                    <property name="tokens" value="METHOD_DEF,LITERAL_IF"/>
                                </module>"""
                )
        );
        assertEquals(CommonCodeStyleSettings.NEXT_LINE, javaSettings.CLASS_BRACE_STYLE);
        assertEquals(CommonCodeStyleSettings.END_OF_LINE, javaSettings.METHOD_BRACE_STYLE);
        assertEquals(CommonCodeStyleSettings.END_OF_LINE, javaSettings.BRACE_STYLE);
    }

    public void testNeedBracesImporter() {
        javaSettings.DOWHILE_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        javaSettings.IF_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        javaSettings.FOR_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="NeedBraces">
                                    <property name="allowSingleLineStatement" value="true"/>
                                </module>"""
                )
        );
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.DOWHILE_BRACE_FORCE);
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.IF_BRACE_FORCE);
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.FOR_BRACE_FORCE);
    }

    public void testIndentationImporter() {
        javaSettings.INDENT_BREAK_FROM_CASE = false;
        CommonCodeStyleSettings.IndentOptions indentOptions = javaSettings.getIndentOptions();
        assertNotNull(indentOptions);
        indentOptions.INDENT_SIZE = 8;
        indentOptions.CONTINUATION_INDENT_SIZE = 8;
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="Indentation">
                                            <property name="basicOffset" value="2"/>
                                            <property name="braceAdjustment" value="0"/>
                                            <property name="caseIndent" value="2"/>
                                            <property name="throwsIndent" value="4"/>
                                            <property name="lineWrappingIndentation" value="4"/>
                                            <property name="arrayInitIndent" value="2"/>
                                </module>"""
                )
        );
        javaSettings.INDENT_BREAK_FROM_CASE = true;
        indentOptions.INDENT_SIZE = 2;
        indentOptions.CONTINUATION_INDENT_SIZE = 4;
    }

    public void testImportOrderImporter() {
        // group attribute
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,java,*"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(false, "java", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - top
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="top"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - bottom
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="bottom"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - above
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="above"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(true, "my.custom.package", true),
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - under
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="under"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(true, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - inflow
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="inflow"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            assertFalse(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separated attribute - top
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="top"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.BLANK_LINE_ENTRY,
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            assertFalse(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - bottom
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="bottom"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            };

            assertFalse(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - above
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="above"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(true, "my.custom.package", true),
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - under
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="under"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(true, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            };

            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - inflow
        {
            importConfiguration(
                    inTreeWalker(
                            """
                                     <module name="ImportOrder">
                                                <property name="groups" value="my.custom.package,*"/>
                                                <property name="option" value="inflow"/>
                                                <property name="separated" value="true"/>
                                    </module>"""
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            };

            assertFalse(codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
        }
    }

    public void testSeparatorWrapImporter_nl() {
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="SeparatorWrap">
                                    <property name="option" value="nl"/>
                                    <property name="tokens" value="DOT"/>
                                </module>"""
                )
        );
        assertTrue(javaSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN);
        assertEquals(CommonCodeStyleSettings.WRAP_ALWAYS, javaSettings.METHOD_CALL_CHAIN_WRAP);
    }

    public void testSeparatorWrapImporter_eol() {
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="SeparatorWrap">
                                    <property name="option" value="eol"/>
                                    <property name="tokens" value="DOT"/>
                                </module>"""
                )
        );
        assertFalse(javaSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN);
        assertEquals(CommonCodeStyleSettings.DO_NOT_WRAP, javaSettings.METHOD_CALL_CHAIN_WRAP);
    }

    public void testOperatorWrapImporter() {
        importConfiguration(
                inTreeWalker(
                        "<module name=\"OperatorWrap\"><property name=\"option\" value=\"eol\"/></module>"
                )
        );
        assertFalse(javaSettings.BINARY_OPERATION_SIGN_ON_NEXT_LINE);

        importConfiguration(
                inTreeWalker(
                        "<module name=\"OperatorWrap\"><property name=\"option\" value=\"nl\"/></module>"
                )
        );
        assertTrue(javaSettings.BINARY_OPERATION_SIGN_ON_NEXT_LINE);
    }

    public void testCustomImportOrderImporterSamePackageIsSkipped() {
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="CustomImportOrder">
                                    <property name="customImportOrderRules" value="SAME_PACKAGE(2)###STATIC"/>
                                </module>"""
                )
        );
        PackageEntry[] expected = {
                PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                PackageEntry.ALL_OTHER_IMPORTS_ENTRY
        };
        comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);

        assertEquals(CheckStyleBundle.message("import.custom-order.same-package-unsupported"),
                importer.getAdditionalImportInfo(mock(CodeStyleScheme.class)));
    }

    public void testAdditionalImportInfoPreservesConfigOrderAcrossMultipleWarnings() {
        importConfiguration(
                inTreeWalker(
                        "<module name=\"WarningOrderingFixture\"/>"
                )
        );
        assertEquals("first-warning\nsecond-warning",
                importer.getAdditionalImportInfo(mock(CodeStyleScheme.class)));
    }

    @SuppressWarnings("unchecked")
    public void testAdditionalImportInfoDoesNotLeakToASubsequentCleanImport() throws Exception {
        CodeStyleScheme currentScheme = mock(CodeStyleScheme.class);
        when(currentScheme.isDefault()).thenReturn(false);
        when(currentScheme.getCodeStyleSettings()).thenReturn(codeStyleSettings);
        SchemeFactory<CodeStyleScheme> schemeFactory = mock(SchemeFactory.class);

        ConfigurationLocationFactory configurationLocationFactory = mock(ConfigurationLocationFactory.class);
        ConfigurationLocation configurationLocation = mock(ConfigurationLocation.class);
        when(configurationLocationFactory.create(any(), any(), any(), any(), any(), any()))
                .thenReturn(configurationLocation);
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(configurationLocationFactory);
        when(project.getService(DependencyValidationManager.class))
                .thenReturn(mock(DependencyValidationManager.class));

        CheckStyleCodeStyleImporter schemeImporter = new CheckStyleCodeStyleImporter(csService);

        VirtualFile warningFile = new LightVirtualFile("checkstyle-warning.xml", FILE_PREFIX + inTreeWalker(
                """
                        <module name="CustomImportOrder">
                            <property name="customImportOrderRules" value="SAME_PACKAGE(2)###STATIC"/>
                        </module>"""
        ) + FILE_SUFFIX);
        schemeImporter.importScheme(project, warningFile, currentScheme, schemeFactory);
        assertEquals(CheckStyleBundle.message("import.custom-order.same-package-unsupported"),
                schemeImporter.getAdditionalImportInfo(mock(CodeStyleScheme.class)));

        VirtualFile cleanFile = new LightVirtualFile("checkstyle-clean.xml", FILE_PREFIX + inTreeWalker(
                """
                        <module name="LineLength">
                            <property name="max" value="100"/>
                        </module>"""
        ) + FILE_SUFFIX);
        schemeImporter.importScheme(project, cleanFile, currentScheme, schemeFactory);
        assertNull(schemeImporter.getAdditionalImportInfo(mock(CodeStyleScheme.class)));
    }

    public void testCustomImportOrderImporterSortAlphabetically() {
        // Default standardPackageRegExp is ["javax", "java"]; with sortImportsInGroupAlphabetically=true → ["java", "javax"]
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="CustomImportOrder">
                                    <property name="customImportOrderRules" value="STANDARD_JAVA_PACKAGE"/>
                                    <property name="sortImportsInGroupAlphabetically" value="true"/>
                                </module>"""
                )
        );
        PackageEntry[] expected = {
                new PackageEntry(false, "java", true),
                new PackageEntry(false, "javax", true),
                PackageEntry.ALL_OTHER_IMPORTS_ENTRY
        };
        comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
    }

    public void testCustomImportOrderImporterNoSortPreservesDefinedOrder() {
        // Default standardPackageRegExp is ["javax", "java"]; without sorting → defined order preserved
        importConfiguration(
                inTreeWalker(
                        """
                                <module name="CustomImportOrder">
                                    <property name="customImportOrderRules" value="STANDARD_JAVA_PACKAGE"/>
                                    <property name="sortImportsInGroupAlphabetically" value="false"/>
                                </module>"""
                )
        );
        PackageEntry[] expected = {
                new PackageEntry(false, "javax", true),
                new PackageEntry(false, "java", true),
                PackageEntry.ALL_OTHER_IMPORTS_ENTRY
        };
        comparePackageEntries(expected, codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class).IMPORT_LAYOUT_TABLE);
    }

    private static void comparePackageEntries(final PackageEntry[] expected, final PackageEntryTable actual) {
        assertEquals(expected.length, actual.getEntryCount());
        for (int x = 0; x < expected.length; x++) {
            assertEquals(expected[x], actual.getEntries()[x]);
        }
    }

    public void testAvoidStartImportImporter() {
        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                </module>"""
                )
        );
        JavaCodeStyleSettings customSettings = codeStyleSettings.getCustomSettings(JavaCodeStyleSettings.class);

        assertEquals(999, customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(999, customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                            <property name="allowClassImports" value="true"/>
                                            <property name="allowStaticMemberImports" value="true"/>
                                </module>"""
                )
        );

        assertEquals(1, customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(1, customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                            <property name="allowStaticMemberImports" value="true"/>
                                </module>"""
                )
        );

        assertEquals(999, customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(1, customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                            <property name="allowClassImports" value="true"/>
                                </module>"""
                )
        );

        assertEquals(1, customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(999, customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        """
                                 <module name="AvoidStarImport">
                                            <property name="excludes" value="a.b.c,d.e.f"/>
                                </module>"""
                )
        );

        PackageEntry[] expected = new PackageEntry[]{
                new PackageEntry(false, "a.b.c", false),
                new PackageEntry(false, "d.e.f", false),
        };

        comparePackageEntries(expected, customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND);
    }

    private static void resetAvoidStarImportSettings(final CodeStyleSettings settings) {
        JavaCodeStyleSettings customSettings = settings.getCustomSettings(JavaCodeStyleSettings.class);
        customSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;
        customSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;
        customSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.copyFrom(new PackageEntryTable());
    }

    public void testDeclarationOrderDefault() {
        importConfiguration(inTreeWalker("<module name=\"DeclarationOrder\"/>"));

        StdArrangementSettings arrangementSettings = (StdArrangementSettings) javaSettings.getArrangementSettings();
        assertNotNull(arrangementSettings);
        List<StdArrangementMatchRule> rules = arrangementSettings.getRules();
        assertEquals(10, rules.size());
        assertRuleTokens(rules.get(0), FIELD, STATIC, PUBLIC);
        assertRuleTokens(rules.get(1), FIELD, STATIC, PROTECTED);
        assertRuleTokens(rules.get(2), FIELD, STATIC, PACKAGE_PRIVATE);
        assertRuleTokens(rules.get(3), FIELD, STATIC, PRIVATE);
        assertRuleTokens(rules.get(4), FIELD, PUBLIC);
        assertRuleTokens(rules.get(5), FIELD, PROTECTED);
        assertRuleTokens(rules.get(6), FIELD, PACKAGE_PRIVATE);
        assertRuleTokens(rules.get(7), FIELD, PRIVATE);
        assertRuleTokens(rules.get(8), CONSTRUCTOR);
        assertRuleTokens(rules.get(9), METHOD);
    }

    public void testDeclarationOrderIgnoreBoth() {
        importConfiguration(inTreeWalker(
                "<module name=\"DeclarationOrder\">"
                        + "<property name=\"ignoreConstructors\" value=\"true\"/>"
                        + "<property name=\"ignoreModifiers\" value=\"true\"/>"
                        + "</module>"));

        StdArrangementSettings arrangementSettings = (StdArrangementSettings) javaSettings.getArrangementSettings();
        assertNotNull(arrangementSettings);
        List<StdArrangementMatchRule> rules = arrangementSettings.getRules();
        assertEquals(3, rules.size());
        assertRuleTokens(rules.get(0), FIELD, STATIC);
        assertRuleTokens(rules.get(1), FIELD);
        assertRuleTokens(rules.get(2), METHOD);
    }

    public void testDeclarationOrderIgnoreModifiers() {
        importConfiguration(inTreeWalker(
                "<module name=\"DeclarationOrder\"><property name=\"ignoreModifiers\" value=\"true\"/></module>"));

        StdArrangementSettings arrangementSettings = (StdArrangementSettings) javaSettings.getArrangementSettings();
        assertNotNull(arrangementSettings);
        List<StdArrangementMatchRule> rules = arrangementSettings.getRules();
        assertEquals(4, rules.size());
        assertRuleTokens(rules.get(0), FIELD, STATIC);
        assertRuleTokens(rules.get(1), FIELD);
        assertRuleTokens(rules.get(2), CONSTRUCTOR);
        assertRuleTokens(rules.get(3), METHOD);
    }

    public void testDeclarationOrderIgnoreConstructors() {
        importConfiguration(inTreeWalker(
                "<module name=\"DeclarationOrder\"><property name=\"ignoreConstructors\" value=\"true\"/></module>"));

        StdArrangementSettings arrangementSettings = (StdArrangementSettings) javaSettings.getArrangementSettings();
        assertNotNull(arrangementSettings);
        List<StdArrangementMatchRule> rules = arrangementSettings.getRules();
        assertEquals(9, rules.size());
        assertRuleTokens(rules.get(0), FIELD, STATIC, PUBLIC);
        assertRuleTokens(rules.get(1), FIELD, STATIC, PROTECTED);
        assertRuleTokens(rules.get(2), FIELD, STATIC, PACKAGE_PRIVATE);
        assertRuleTokens(rules.get(3), FIELD, STATIC, PRIVATE);
        assertRuleTokens(rules.get(4), FIELD, PUBLIC);
        assertRuleTokens(rules.get(5), FIELD, PROTECTED);
        assertRuleTokens(rules.get(6), FIELD, PACKAGE_PRIVATE);
        assertRuleTokens(rules.get(7), FIELD, PRIVATE);
        assertRuleTokens(rules.get(8), METHOD);
    }

    private static void assertRuleTokens(final StdArrangementMatchRule rule,
                                         final ArrangementSettingsToken... expectedTokens) {
        assertEquals(tokensOf(rule), Set.of(expectedTokens));
    }

    private static Set<ArrangementSettingsToken> tokensOf(final StdArrangementMatchRule rule) {
        ArrangementMatchCondition condition = rule.getMatcher().getCondition();
        Set<ArrangementSettingsToken> tokens = new HashSet<>();
        if (condition instanceof ArrangementAtomMatchCondition atom) {
            tokens.add(atom.getType());
        } else if (condition instanceof ArrangementCompositeMatchCondition composite) {
            for (ArrangementMatchCondition operand : composite.getOperands()) {
                if (operand instanceof ArrangementAtomMatchCondition atom) {
                    tokens.add(atom.getType());
                }
            }
        }
        return tokens;
    }

}
