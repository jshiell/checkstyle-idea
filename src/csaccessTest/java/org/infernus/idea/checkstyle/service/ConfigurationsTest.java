package org.infernus.idea.checkstyle.service;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ConfigurationsTest {
    private static final int CODE_STYLE_TAB_SIZE = 3;

    private TabWidthAndBaseDirProvider createClassUnderTest(final Configuration pConfig) throws IOException {

        final Module module = mock(Module.class);
        final ConfigurationLocation configurationLocation = mock(ConfigurationLocation.class);
        final CodeStyleSettings codeStyleSettings = mock(CodeStyleSettings.class);

        when(configurationLocation.resolveAssociatedFile(eq("aFileToResolve"), eq(module), any(ClassLoader.class))).thenReturn("aResolvedFile");
        when(configurationLocation.resolveAssociatedFile(eq("triggersAnIoException"), eq(module), any(ClassLoader.class))).thenThrow(new
                IOException("aTriggeredIoException"));
        when(codeStyleSettings.getTabSize(JavaFileType.INSTANCE)).thenReturn(CODE_STYLE_TAB_SIZE);

        return new Configurations(module, pConfig) {
            @NotNull
            @Override
            CodeStyleSettings currentCodeStyleSettings() {
                return codeStyleSettings;
            }
        };
    }


    @Test
    public void aDefaultTabWidthIsEightIsUsedWhenNoTabWidthPropertyIsPresent() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().build();
        assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void testNoTreeWalker_useDefault() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("FileTabCharacter")).build();
        assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void tabWidthPropertyValueIsReturnedWhenPresent() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", "7")).build();
        assertEquals(7, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void aTabWidthPropertyWithANonIntegerValueReturnsTheDefault() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", "dd")).build();
        assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void aTabWidthPropertyWithNoValueReturnsTheDefault() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", "")).build();
        assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void tabWidthNullAttribute() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", null)).build();
        assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void testBaseDir() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", "7")).build();
        assertEquals(Optional.empty(), createClassUnderTest(config).baseDir());
    }


    @Test
    public void testBaseDirFromAttribute() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withAttribute("foo", "bar") //
                .withAttribute("basedir", "/some/dir").build();
        assertEquals(Optional.of("/some/dir"), createClassUnderTest(config).baseDir());
    }
}
