package org.infernus.idea.checkstyle.service.cmd;

import com.google.common.collect.ImmutableMap;
import com.intellij.ide.plugins.PluginUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.service.CheckstyleActionsImpl;
import org.infernus.idea.checkstyle.service.ConfigurationBuilder;
import org.infernus.idea.checkstyle.service.FileUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.infernus.idea.checkstyle.service.ConfigurationMatcher.configEqualTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;


public class OpLoadConfigurationTest {
    private static final Project PROJECT = mock(Project.class);

    private final ConfigurationLocation configurationLocation = mock(ConfigurationLocation.class);
    private final Module module = mock(Module.class);
    private final Notifications notifications = mock(Notifications.class);

    private OpLoadConfiguration underTest;

    @Before
    public void setUp() throws IOException {
        interceptApplicationNotifications();

        final ClassLoader checkstyleClassloader = mock(ClassLoader.class);
        final CheckstyleProjectService checkstyleProjectService = mock(CheckstyleProjectService.class);
        when(checkstyleProjectService.underlyingClassLoader()).thenReturn(checkstyleClassloader);

        when(configurationLocation.resolveAssociatedFile("aFileToResolve", module, checkstyleClassloader))
                .thenReturn("aResolvedFile");
        when(configurationLocation.resolveAssociatedFile("triggersAnIoException", module, checkstyleClassloader))
                .thenThrow(new IOException("aTriggeredIoException"));

        underTest = new OpLoadConfiguration(configurationLocation, null, module, checkstyleProjectService);
    }

    private void interceptApplicationNotifications() {
        final MessageBus messageBus = mock(MessageBus.class);
        when(PROJECT.getMessageBus()).thenReturn(messageBus);
        when(messageBus.syncPublisher(Notifications.TOPIC)).thenReturn(notifications);

        final ExtensionsArea extensionArea = mock(ExtensionsArea.class);
        final PluginUtil pluginUtil = mock(PluginUtil.class);

        final Application application = mock(Application.class);
        when(application.isUnitTestMode()).thenReturn(true);
        when(application.getMessageBus()).thenReturn(messageBus);
        when(application.getExtensionArea()).thenReturn(extensionArea);
        when(application.getService(PluginUtil.class)).thenReturn(pluginUtil);
        ApplicationManager.setApplication(application, mock(Disposable.class));
    }

    @Test
    public void filePathsAreNotResolvedOnANonDefaultImplementationOfConfiguration() {
        final Configuration config = mock(Configuration.class);
        verifyZeroInteractions(config);
    }

    @Test
    public void childrenOfUpdatedElementsArePreserved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter").withAttribute("file", "aFileToResolve")
                        .withChild(ConfigurationBuilder.config("aChildModule")))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter").withAttribute("file", "aResolvedFile")
                        .withChild(ConfigurationBuilder.config("aChildModule")))
                .build())));
    }

    @Test
    public void attributesOfUpdatedElementsArePreserved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("ImportControl")
                                .withAttribute("file", "aFileToResolve")
                                .withAttribute("anotherAttribute", "anotherValue")))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("ImportControl")
                                .withAttribute("file", "aResolvedFile")
                                .withAttribute("anotherAttribute", "anotherValue")))
                .build())));
    }

    @Test
    public void messagesOfUpdatedElementsArePreserved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "aFileToResolve")
                        .withMessage("messageKey",
                                "messageValue").withMessage("anotherMessageKey", "anotherMessageValue"))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "aResolvedFile")
                        .withMessage("messageKey", "messageValue")
                        .withMessage("anotherMessageKey", "anotherMessageValue"))
                .build())));
    }

    @Test
    public void multipleElementsAreUpdated() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "aFileToResolve"))
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("ImportControl")
                                .withAttribute("file",
                                        "anUnresolvableFile"))
                        .withChild(ConfigurationBuilder.config("RegexpHeader")
                                .withAttribute("headerFile", "aFileToResolve")))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "aResolvedFile"))
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("RegexpHeader")
                                .withAttribute("headerFile", "aResolvedFile")))
                .build())));
    }

    @Test
    public void removalOfElementsDoesNotEffectOtherElements() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "anUnresolvableFile"))
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("RegexpHeader")
                                .withAttribute("headerFile", "anUnresolvableFile"))
                        .withChild(ConfigurationBuilder.config("ConstantName")))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("ConstantName")))
                .build())));
    }

    @Test
    public void aModuleWithAFilenameThatRaisesAnIOExceptionOnResolutionDoesNotModifyTheConfiguration() throws
            CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "triggersAnIoException"))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "triggersAnIoException"))
                .build())));
    }

    @Test
    public void aModuleWithAFilenameThatRaisesAnIOExceptionOnResolutionTriggersAnErrorNotification() throws
            CheckstyleException {
        underTest.resolveFilePaths(PROJECT, ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "triggersAnIoException"))
                .build());

        final Notification notification = sentNotification();
        assertThat(notification, is(not(nullValue())));
        assertThat(notification.getType(), is(equalTo(NotificationType.ERROR)));
        assertThat(notification.getContent(),
                is(equalTo(CheckStyleBundle.message("checkstyle.checker-failed", "aTriggeredIoException"))));
    }

    @Test
    public void aModuleWithAFilenameThatIsNotResolvesTriggersAWarningNotification() throws CheckstyleException {
        underTest.resolveFilePaths(PROJECT, ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("RegexpHeader")
                                .withAttribute("headerFile",
                                        "anUnresolvableFile")))
                .build());

        final Notification notification = sentNotification();
        assertThat(notification, is(not(nullValue())));
        assertThat(notification.getType(), is(equalTo(NotificationType.WARNING)));
        assertThat(notification.getContent(),
                is(equalTo(CheckStyleBundle.message("checkstyle.not-found.RegexpHeader"))));
    }

    @Test
    public void aModuleWithAFilenameThatIsBlankDoesNotHaveResolutionAttempted() throws CheckstyleException {
        underTest.resolveFilePaths(PROJECT, ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("ImportControl")
                                .withAttribute("file", ""))).build());
    }

    private Notification sentNotification() {
        final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).notify(notificationCaptor.capture());
        return notificationCaptor.getValue();
    }

    @Test
    public void aSuppressionFilterWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "aFileToResolve"))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "aResolvedFile"))
                .build())));
    }

    @Test
    public void aSuppressionFilterWithAnUnresolvableFilenameHasTheModuleRemoved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("SuppressionFilter")
                        .withAttribute("file", "anUnresolvableFile"))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker().build())));
    }

    @Test
    public void aRegexpHeaderWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("RegexpHeader")
                                .withAttribute("headerFile", "aFileToResolve")))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("RegexpHeader")
                                .withAttribute("headerFile", "aResolvedFile")))
                .build())));
    }

    @Test
    public void aRegexpHeaderWithAnUnresolvableFilenameHasTheModuleReferenceRemoved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config("TreeWalker")
                .withChild(ConfigurationBuilder.config("RegexpHeader").withAttribute("headerFile",
                        "anUnresolvableFile"))).build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker"))
                .build())));
    }

    @Test
    public void aImportControlWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("ImportControl")
                                .withAttribute("file", "aFileToResolve")))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("ImportControl")
                                .withAttribute("file", "aResolvedFile")))
                .build())));
    }

    @Test
    public void anImportControlWithAnUnresolvableFilenameHasTheModuleReferenceRemoved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker")
                        .withChild(ConfigurationBuilder.config("ImportControl")
                                .withAttribute("file", "anUnresolvableFile")))
                .build();
        underTest.resolveFilePaths(PROJECT, config);
        assertThat(config, is(configEqualTo(ConfigurationBuilder.checker()
                .withChild(ConfigurationBuilder.config("TreeWalker"))
                .build())));
    }


    @Test
    public void testNoConfiguration() {
        CheckstyleProjectService projectService = mock(CheckstyleProjectService.class);
        when(projectService.underlyingClassLoader()).thenReturn(getClass().getClassLoader());
        OpLoadConfiguration testee = new OpLoadConfiguration(configurationLocation, null, module, projectService) {
            @Override
            Configuration callLoadConfiguration(final InputStream inputStream) {
                return null;
            }
        };
        try {
            testee.execute(PROJECT);
        } catch (CheckstyleException e) {
            assertTrue(e.getMessage().contains("Couldn't find root module"));
        }
    }


    @Test
    public void testWrongConfigurationClass() throws CheckstyleException {
        Configuration config = new Configuration() {
            @Override
            public String[] getAttributeNames() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAttribute(final String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Configuration[] getChildren() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getName() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ImmutableMap<String, String> getMessages() {
                throw new UnsupportedOperationException();
            }
        };
        underTest.resolveFilePaths(PROJECT, config);  // just log a warning and do nothing ... well ...
    }


    @Test
    public void testConstructors() {
        final CheckstyleProjectService checkstyleProjectService = mock(CheckstyleProjectService.class);

        new OpLoadConfiguration(configurationLocation, checkstyleProjectService);
        new OpLoadConfiguration(configurationLocation, null, checkstyleProjectService);
        new OpLoadConfiguration(configurationLocation, null, module, checkstyleProjectService);
        VirtualFile virtualFile = mock(VirtualFile.class);
        new OpLoadConfiguration(virtualFile, checkstyleProjectService);
        new OpLoadConfiguration(virtualFile, null, checkstyleProjectService);
        new OpLoadConfiguration("doesn't matter", checkstyleProjectService);
    }


    @Test
    public void testLoadFromString() throws IOException, URISyntaxException {
        CheckstyleProjectService projectService = mock(CheckstyleProjectService.class);
        when(projectService.underlyingClassLoader()).thenReturn(getClass().getClassLoader());
        final String configXml = FileUtil.readFile("cmd/config-ok.xml");
        CheckstyleInternalObject csConfig = new CheckstyleActionsImpl(PROJECT, projectService).loadConfiguration(configXml);
        Assert.assertNotNull(csConfig);
    }


    @Test
    public void testLoadBundledSunChecks() {
        final ConfigurationLocationFactory clf = new ConfigurationLocationFactory();
        CheckstyleProjectService projectService = mock(CheckstyleProjectService.class);
        when(projectService.underlyingClassLoader()).thenReturn(getClass().getClassLoader());
        CheckstyleInternalObject csConfig = new CheckstyleActionsImpl(PROJECT, projectService)
                .loadConfiguration(clf.create(BundledConfig.SUN_CHECKS, mock(Project.class)), true, null);
        Assert.assertNotNull(csConfig);
    }


    @Test
    public void testLoadBundledGoogleChecks() {
        final ConfigurationLocationFactory clf = new ConfigurationLocationFactory();
        CheckstyleProjectService projectService = mock(CheckstyleProjectService.class);
        when(projectService.underlyingClassLoader()).thenReturn(getClass().getClassLoader());
        CheckstyleInternalObject csConfig = new CheckstyleActionsImpl(PROJECT, projectService)
                .loadConfiguration(clf.create(BundledConfig.GOOGLE_CHECKS, mock(Project.class)), true, null);
        Assert.assertNotNull(csConfig);
    }
}
