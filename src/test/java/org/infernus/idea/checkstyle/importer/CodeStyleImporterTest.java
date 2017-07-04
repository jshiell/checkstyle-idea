package org.infernus.idea.checkstyle.importer;

import java.util.Collections;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.PackageEntry;
import com.intellij.psi.codeStyle.PackageEntryTable;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.PluginConfigDto;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;


public class CodeStyleImporterTest
        extends LightPlatformTestCase
{
    private CodeStyleSettings codeStyleSettings;
    private CommonCodeStyleSettings javaSettings;

    private final Project project = Mockito.mock(Project.class);
    private CheckstyleProjectService csService = null;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        CheckStyleConfiguration mockPluginConfig = Mockito.mock(CheckStyleConfiguration.class);
        final PluginConfigDto mockConfigDto = new PluginConfigDto("7.1.1", ScanScope.AllSources, false,
                Collections.emptySortedSet(), Collections.emptyList(), null, false);
        Mockito.when(mockPluginConfig.getCurrentPluginConfig()).thenReturn(mockConfigDto);
        CheckStyleConfiguration.activateMock4UnitTesting(mockPluginConfig);

        csService = new CheckstyleProjectService(project);

        codeStyleSettings = new CodeStyleSettings(false);
        javaSettings = codeStyleSettings.getCommonSettings(JavaLanguage.INSTANCE);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        CheckStyleConfiguration.activateMock4UnitTesting(null);
    }

    private final static String FILE_PREFIX =
            "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE module PUBLIC\n" +
            "          \"-//Puppy Crawl//DTD Check Configuration 1.3//EN\"\n" +
            "          \"http://www.puppycrawl.com/dtds/configuration_1_3.dtd\">\n" +
            "<module name = \"Checker\">\n";
    private final static String FILE_SUFFIX =
            "</module>";

    private void importConfiguration(@NotNull String configuration) throws Exception {
        configuration = FILE_PREFIX + configuration + FILE_SUFFIX;
        new CheckStyleCodeStyleImporter(csService).importConfiguration(
                loadConfiguration(configuration), codeStyleSettings);
    }

    private String inTreeWalker(@NotNull String configuration) {
        return "<module name=\"TreeWalker\">" + configuration + "</module>";
    }

    private CheckstyleInternalObject loadConfiguration(@NotNull final String configuration) {
        return csService.getCheckstyleInstance().loadConfiguration(configuration);
    }

    public void testImportRightMargin() throws Exception {
        importConfiguration(
                inTreeWalker(
                        "<module name=\"LineLength\">\n" +
                        "    <property name=\"max\" value=\"100\"/>\n" +
                        "</module>"
                )
        );
        assertEquals(100, javaSettings.RIGHT_MARGIN);
    }

    public void testEmptyLineSeparator() throws Exception {
        javaSettings.BLANK_LINES_AROUND_FIELD = 0;
        javaSettings.BLANK_LINES_AROUND_METHOD = 0;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"EmptyLineSeparator\">\n" +
                        "    <property name=\"tokens\" value=\"VARIABLE_DEF, METHOD_DEF\"/>\n" +
                        "</module>"
                )
        );
        assertEquals(1, javaSettings.BLANK_LINES_AROUND_FIELD);
        assertEquals(1, javaSettings.BLANK_LINES_AROUND_METHOD);
    }

    public void testImportFileTabCharacter() throws Exception {
        CommonCodeStyleSettings xmlSettings = codeStyleSettings.getCommonSettings(XMLLanguage.INSTANCE);
        CommonCodeStyleSettings.IndentOptions javaIndentOptions = javaSettings.getIndentOptions();
        assertNotNull(javaIndentOptions);
        CommonCodeStyleSettings.IndentOptions xmlIndentOptions = xmlSettings.getIndentOptions();
        assertNotNull(xmlIndentOptions);
        javaIndentOptions.USE_TAB_CHARACTER = true;
        xmlIndentOptions.USE_TAB_CHARACTER = true;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"FileTabCharacter\">\n" +
                        "    <property name=\"eachLine\" value=\"true\" />\n" +
                        "    <property name=\"fileExtensions\" value=\"java,xml\" />\n" +
                        "</module>"
                )
        );
        assertFalse(javaIndentOptions.USE_TAB_CHARACTER);
        assertFalse(xmlIndentOptions.USE_TAB_CHARACTER);
    }

    public void testImportFileTabCharacterNoExplicitExtensions() throws Exception {
        CommonCodeStyleSettings xmlSettings = codeStyleSettings.getCommonSettings(XMLLanguage.INSTANCE);
        CommonCodeStyleSettings.IndentOptions javaIndentOptions = javaSettings.getIndentOptions();
        assertNotNull(javaIndentOptions);
        CommonCodeStyleSettings.IndentOptions xmlIndentOptions = xmlSettings.getIndentOptions();
        assertNotNull(xmlIndentOptions);
        javaIndentOptions.USE_TAB_CHARACTER = true;
        xmlIndentOptions.USE_TAB_CHARACTER = true;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"FileTabCharacter\"/>\n"
                )
        );
        assertFalse(javaIndentOptions.USE_TAB_CHARACTER);
        assertFalse(xmlIndentOptions.USE_TAB_CHARACTER);
    }

    public void testImportWhitespaceAfter() throws Exception {
        javaSettings.SPACE_AFTER_COMMA = false;
        javaSettings.SPACE_AFTER_SEMICOLON = false;
        javaSettings.SPACE_AFTER_TYPE_CAST = false;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"WhitespaceAfter\">\n" +
                        "    <property name=\"tokens\" value=\"COMMA, SEMI\"/>\n" +
                        "</module>"
                )
        );
        assertTrue(javaSettings.SPACE_AFTER_COMMA);
        assertTrue(javaSettings.SPACE_AFTER_SEMICOLON);
        assertFalse(javaSettings.SPACE_AFTER_TYPE_CAST);
    }

    public void testImportWhitespaceAround() throws Exception {
        javaSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS = false;
        javaSettings.SPACE_AROUND_EQUALITY_OPERATORS = false;
        javaSettings.SPACE_AROUND_BITWISE_OPERATORS = false;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"WhitespaceAround\">\n" +
                        "    <property name=\"tokens\" value=\"ASSIGN\"/>\n" +
                        "    <property name=\"tokens\" value=\"EQUAL\"/>\n" +
                        "</module>"
                )
        );
        assertTrue(javaSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS);
        assertTrue(javaSettings.SPACE_AROUND_EQUALITY_OPERATORS);
        assertFalse(javaSettings.SPACE_AROUND_BITWISE_OPERATORS);
    }

    public void testNoWhitespaceBeforeImporter() throws Exception {
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

    public void testLeftCurlyImporter() throws Exception {
        javaSettings.CLASS_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        javaSettings.METHOD_BRACE_STYLE =  CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        javaSettings.BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE_SHIFTED;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"LeftCurly\">\n" +
                        "    <property name=\"option\" value=\"nl\"/>\n" +
                        "    <property name=\"tokens\" value=\"CLASS_DEF,INTERFACE_DEF\"/>\n" +
                        "</module>\n" +
                        "<module name=\"LeftCurly\">\n" +
                        "    <property name=\"option\" value=\"eol\"/>\n" +
                        "    <property name=\"tokens\" value=\"METHOD_DEF,LITERAL_IF\"/>\n" +
                        "</module>"
                )
        );
        assertEquals(CommonCodeStyleSettings.NEXT_LINE, javaSettings.CLASS_BRACE_STYLE);
        assertEquals(CommonCodeStyleSettings.END_OF_LINE, javaSettings.METHOD_BRACE_STYLE);
        assertEquals(CommonCodeStyleSettings.END_OF_LINE, javaSettings.BRACE_STYLE);
    }

    public void testNeedBracesImporter() throws Exception {
        javaSettings.DOWHILE_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        javaSettings.IF_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        javaSettings.FOR_BRACE_FORCE = CommonCodeStyleSettings.DO_NOT_FORCE;
        importConfiguration(
                inTreeWalker(
                        "<module name=\"NeedBraces\">\n" +
                        "    <property name=\"allowSingleLineStatement\" value=\"true\"/>\n" +
                        "</module>"
                )
        );
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.DOWHILE_BRACE_FORCE);
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.IF_BRACE_FORCE);
        assertEquals(CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE, javaSettings.FOR_BRACE_FORCE);
    }

    public void testIndentationImporter() throws Exception {
        javaSettings.INDENT_BREAK_FROM_CASE = false;
        CommonCodeStyleSettings.IndentOptions indentOptions = javaSettings.getIndentOptions();
        assertNotNull(indentOptions);
        indentOptions.INDENT_SIZE = 8;
        indentOptions.CONTINUATION_INDENT_SIZE = 8;
        importConfiguration(
                inTreeWalker(
                        " <module name=\"Indentation\">\n" +
                        "            <property name=\"basicOffset\" value=\"2\"/>\n" +
                        "            <property name=\"braceAdjustment\" value=\"0\"/>\n" +
                        "            <property name=\"caseIndent\" value=\"2\"/>\n" +
                        "            <property name=\"throwsIndent\" value=\"4\"/>\n" +
                        "            <property name=\"lineWrappingIndentation\" value=\"4\"/>\n" +
                        "            <property name=\"arrayInitIndent\" value=\"2\"/>\n" +
                        "</module>"
                )
        );
        javaSettings.INDENT_BREAK_FROM_CASE = true;
        indentOptions.INDENT_SIZE = 2;
        indentOptions.CONTINUATION_INDENT_SIZE = 4;
    }

    public void testImportOrderImporter() throws Exception {
        // group attribute
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,java,*\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(false, "java", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY
            };

            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - top
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"top\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    };

            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - bottom
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"bottom\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    };

            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - above
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"above\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(true, "my.custom.package", true),
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    };

            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - under
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"under\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(true, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    };

            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // staticPosition attribute - inflow
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"inflow\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    };

            assertEquals(false, codeStyleSettings.LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // separated attribute - top
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"top\"/>\n" +
                            "            <property name=\"separated\" value=\"true\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.BLANK_LINE_ENTRY,
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    };

            assertEquals(false, codeStyleSettings.LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - bottom
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"bottom\"/>\n" +
                            "            <property name=\"separated\" value=\"true\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    };

            assertEquals(false, codeStyleSettings.LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - above
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"above\"/>\n" +
                            "            <property name=\"separated\" value=\"true\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(true, "my.custom.package", true),
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    };

            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - under
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"under\"/>\n" +
                            "            <property name=\"separated\" value=\"true\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    new PackageEntry(true, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
                    };

            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }

        // separate attribute - inflow
        {
            importConfiguration(
                    inTreeWalker(
                            " <module name=\"ImportOrder\">\n" +
                            "            <property name=\"groups\" value=\"my.custom.package,*\"/>\n" +
                            "            <property name=\"option\" value=\"inflow\"/>\n" +
                            "            <property name=\"separated\" value=\"true\"/>\n" +
                            "</module>"
                    )
            );
            PackageEntry[] expected = new PackageEntry[]{
                    new PackageEntry(false, "my.custom.package", true),
                    PackageEntry.BLANK_LINE_ENTRY,
                    PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
                    };

            assertEquals(false, codeStyleSettings.LAYOUT_STATIC_IMPORTS_SEPARATELY);
            comparePackageEntries(expected, codeStyleSettings.IMPORT_LAYOUT_TABLE);
        }
    }

    private static void comparePackageEntries(PackageEntry[] expected, PackageEntryTable actual) {
        assertEquals(expected.length, actual.getEntryCount());
        for (int x = 0; x < expected.length; x++) {
            assertEquals(expected[x], actual.getEntries()[x]);
        }
    }

    public void testAvoidStartImportImporter() throws Exception {
        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        " <module name=\"AvoidStarImport\">\n" +
                        "</module>"
                )
        );

        assertEquals(999, codeStyleSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(999, codeStyleSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, codeStyleSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        " <module name=\"AvoidStarImport\">\n" +
                        "            <property name=\"allowClassImports\" value=\"true\"/>\n" +
                        "            <property name=\"allowStaticMemberImports\" value=\"true\"/>\n" +
                        "</module>"
                )
        );

        assertEquals(1, codeStyleSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(1, codeStyleSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, codeStyleSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        " <module name=\"AvoidStarImport\">\n" +
                        "            <property name=\"allowStaticMemberImports\" value=\"true\"/>\n" +
                        "</module>"
                )
        );

        assertEquals(999, codeStyleSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(1, codeStyleSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, codeStyleSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        codeStyleSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        " <module name=\"AvoidStarImport\">\n" +
                        "            <property name=\"allowClassImports\" value=\"true\"/>\n" +
                        "</module>"
                )
        );

        assertEquals(1, codeStyleSettings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(999, codeStyleSettings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND);
        assertEquals(0, codeStyleSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.getEntryCount());

        resetAvoidStarImportSettings(codeStyleSettings);
        importConfiguration(
                inTreeWalker(
                        " <module name=\"AvoidStarImport\">\n" +
                        "            <property name=\"excludes\" value=\"a.b.c,d.e.f\"/>\n" +
                        "</module>"
                )
        );

        PackageEntry[] expected = new PackageEntry[]{
                new PackageEntry(false, "a.b.c", false),
                new PackageEntry(false, "d.e.f", false),
                };

        comparePackageEntries(expected, codeStyleSettings.PACKAGES_TO_USE_IMPORT_ON_DEMAND);
    }

    private static void resetAvoidStarImportSettings(CodeStyleSettings settings) {
        settings.NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;
        settings.CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND = 1;
        settings.PACKAGES_TO_USE_IMPORT_ON_DEMAND.copyFrom(new PackageEntryTable());
    }

}
