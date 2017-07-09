package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckStyleChecker;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.TabWidthAndBaseDirProvider;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.CheckstyleActionsImpl;
import org.infernus.idea.checkstyle.service.FileUtil;
import org.infernus.idea.checkstyle.service.StringConfigurationLocation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class OpCreateCheckerTest {
    private static final Project PROJECT = mock(Project.class);
    private static final String CONFIG_FILE = "config-ok.xml";

    private static Module moduleMock;
    private static TabWidthAndBaseDirProvider configurationsMock;

    @BeforeClass
    public static void setUp() throws URISyntaxException {
        moduleMock = mock(Module.class);
        when(moduleMock.getProject()).thenReturn(PROJECT);

        configurationsMock = mock(TabWidthAndBaseDirProvider.class);
        when(configurationsMock.tabWidth()).thenReturn(2);
        when(configurationsMock.baseDir()).thenReturn(  //
                Optional.of(new File(OpCreateCheckerTest.class.getResource(CONFIG_FILE).toURI()).getParent()));

        CheckstyleProjectService csServiceMock = mock(CheckstyleProjectService.class);
        when(csServiceMock.getCheckstyleInstance()).thenReturn(mock(CheckstyleActions.class));
        CheckstyleProjectService.activateMock4UnitTesting(csServiceMock);
    }

    @AfterClass
    public static void tearDown() {
        CheckstyleProjectService.activateMock4UnitTesting(null);
    }

    @Test
    public void testCreateCheckerWithConfigsMock() throws IOException, URISyntaxException {

        final ConfigurationLocation configLoc = new StringConfigurationLocation( //
                FileUtil.readFile("cmd/" + CONFIG_FILE));

        final CheckStyleChecker checker = new CheckstyleActionsImpl(PROJECT).createChecker(moduleMock, configLoc,
                emptyMap(), configurationsMock, getClass().getClassLoader());
        assertNotNull(checker);
    }


    @Test(expected = CheckStylePluginException.class)
    public void testCreateChecker_noConfigLoc() throws IOException, URISyntaxException {

        //noinspection ConstantConditions
        new CheckstyleActionsImpl(PROJECT).createChecker(moduleMock, null, emptyMap(),
                configurationsMock, getClass().getClassLoader());
        fail("expected exception was not thrown");
    }


    @Test
    public void testCreateChecker_noModule() throws IOException, URISyntaxException {

        final ConfigurationLocation configLoc = new StringConfigurationLocation( //
                FileUtil.readFile("cmd/" + CONFIG_FILE));

        //noinspection ConstantConditions
        CheckStyleChecker checker = new CheckstyleActionsImpl(PROJECT).createChecker(null, configLoc,
                emptyMap(), configurationsMock, getClass().getClassLoader());
        assertNotNull(checker);
    }
}
