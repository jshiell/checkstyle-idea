package org.infernus.idea.checkstyle.service;

import java.io.IOException;
import java.util.Optional;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;


public class ConfigurationsTest
{
    private static final int CODE_STYLE_TAB_SIZE = 3;


    private TabWidthAndBaseDirProvider createClassUnderTest(final Configuration pConfig) throws IOException {

        final Module module = Mockito.mock(Module.class);
        final ConfigurationLocation configurationLocation = Mockito.mock(ConfigurationLocation.class);
        final CodeStyleSettings codeStyleSettings = Mockito.mock(CodeStyleSettings.class);

        Mockito.when(configurationLocation.resolveAssociatedFile("aFileToResolve", module)).thenReturn("aResolvedFile");
        Mockito.when(configurationLocation.resolveAssociatedFile("triggersAnIoException", module)).thenThrow(new
                IOException("aTriggeredIoException"));
        Mockito.when(codeStyleSettings.getTabSize(JavaFileType.INSTANCE)).thenReturn(CODE_STYLE_TAB_SIZE);

        return new Configurations(module, pConfig)
        {
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
        Assert.assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void testNoTreeWalker_useDefault() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("FileTabCharacter")).build();
        Assert.assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void tabWidthPropertyValueIsReturnedWhenPresent() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", "7")).build();
        Assert.assertEquals(7, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void aTabWidthPropertyWithANonIntegerValueReturnsTheDefault() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", "dd")).build();
        Assert.assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void aTabWidthPropertyWithNoValueReturnsTheDefault() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", "")).build();
        Assert.assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void tabWidthNullAttribute() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", null)).build();
        Assert.assertEquals(CODE_STYLE_TAB_SIZE, createClassUnderTest(config).tabWidth());
    }


    @Test
    public void testBaseDir() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withChild( //
                ConfigurationBuilder.config("TreeWalker").withAttribute("tabWidth", "7")).build();
        Assert.assertEquals(Optional.empty(), createClassUnderTest(config).baseDir());
    }


    @Test
    public void testBaseDirFromAttribute() throws IOException {
        final Configuration config = ConfigurationBuilder.checker().withAttribute("foo", "bar") //
                .withAttribute("basedir", "/some/dir").build();
        Assert.assertEquals(Optional.of("/some/dir"), createClassUnderTest(config).baseDir());
    }
}
