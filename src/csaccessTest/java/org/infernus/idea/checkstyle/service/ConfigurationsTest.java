package org.infernus.idea.checkstyle.service;

import java.io.IOException;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationsTest
{
    private static final int CODE_STYLE_TAB_SIZE = 3;

    private Configurations underTest;


    @Before
    public void setUp() throws IOException {

        final Module module = Mockito.mock(Module.class);
        final ConfigurationLocation configurationLocation = Mockito.mock(ConfigurationLocation.class);
        final CodeStyleSettings codeStyleSettings = Mockito.mock(CodeStyleSettings.class);

        when(configurationLocation.resolveAssociatedFile("aFileToResolve", module)).thenReturn("aResolvedFile");
        when(configurationLocation.resolveAssociatedFile("triggersAnIoException", module)).thenThrow(new IOException
                ("aTriggeredIoException"));
        when(codeStyleSettings.getTabSize(JavaFileType.INSTANCE)).thenReturn(CODE_STYLE_TAB_SIZE);

        underTest = new Configurations(module)
        {
            @NotNull
            @Override
            CodeStyleSettings currentCodeStyleSettings() {
                return codeStyleSettings;
            }
        };
    }


    @Test
    public void aDefaultTabWidthIsEightIsUsedWhenNoTabWidthPropertyIsPresent() {
        assertThat(underTest.tabWidth(ConfigurationBuilder.checker().build()), is(equalTo(CODE_STYLE_TAB_SIZE)));
    }


    @Test
    public void tabWidthPropertyValueIsReturnedWhenPresent() {
        assertThat(underTest.tabWidth(ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("TreeWalker").withAttribute("tabWidth", "7")).build()), is(equalTo(7)));
    }


    @Test
    public void aTabWidthPropertyWithANonIntegerValueReturnsTheDefault() {
        assertThat(underTest.tabWidth(ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("TreeWalker").withAttribute("tabWidth", "dd")).build()), is(equalTo(CODE_STYLE_TAB_SIZE)));
    }


    @Test
    public void aTabWidthPropertyWithNoValueReturnsTheDefault() {
        assertThat(underTest.tabWidth(ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("TreeWalker").withAttribute("tabWidth", "")).build()), is(equalTo(CODE_STYLE_TAB_SIZE)));
    }
}
