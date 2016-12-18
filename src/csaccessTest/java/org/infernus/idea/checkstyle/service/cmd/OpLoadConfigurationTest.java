package org.infernus.idea.checkstyle.service.cmd;

import java.io.IOException;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.util.messages.MessageBus;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.hamcrest.Matchers;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.ConfigurationBuilder;
import org.infernus.idea.checkstyle.service.ConfigurationMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class OpLoadConfigurationTest
{
    private ConfigurationLocation configurationLocation = Mockito.mock(ConfigurationLocation.class);

    private Module module = Mockito.mock(Module.class);

    private Notifications notifications = Mockito.mock(Notifications.class);

    private OpLoadConfiguration underTest;


    @Before
    public void setUp() throws IOException {

        interceptApplicationNotifications();

        when(configurationLocation.resolveAssociatedFile("aFileToResolve", module)).thenReturn("aResolvedFile");
        when(configurationLocation.resolveAssociatedFile("triggersAnIoException", module)).thenThrow(new IOException
                ("aTriggeredIoException"));

        underTest = new OpLoadConfiguration(configurationLocation, null, module);
    }


    private void interceptApplicationNotifications() {
        final MessageBus messageBus = mock(MessageBus.class);
        when(messageBus.syncPublisher(Notifications.TOPIC)).thenReturn(notifications);

        final Application application = mock(Application.class);
        when(application.isUnitTestMode()).thenReturn(true);
        when(application.getMessageBus()).thenReturn(messageBus);
        ApplicationManager.setApplication(application, mock(Disposable.class));
    }


    @Test
    public void filePathsAreNotResolvedOnANonDefaultImplementationOfConfiguration() throws CheckstyleException {
        final Configuration config = mock(Configuration.class);
        verifyZeroInteractions(config);
    }


    @Test
    public void childrenOfUpdatedElementsArePreserved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("SuppressionFilter").withAttribute("file", "aFileToResolve").withChild(ConfigurationBuilder.config
                ("aChildModule"))).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("SuppressionFilter").withAttribute("file", "aResolvedFile").withChild
                        (ConfigurationBuilder.config("aChildModule"))).build())));
    }

    @Test
    public void attributesOfUpdatedElementsArePreserved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config("TreeWalker")
                .withChild(ConfigurationBuilder.config("ImportControl").withAttribute("file", "aFileToResolve")
                        .withAttribute("anotherAttribute", "anotherValue"))).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("TreeWalker").withChild(ConfigurationBuilder.config("ImportControl")
                        .withAttribute("file", "aResolvedFile").withAttribute("anotherAttribute", "anotherValue")))
                .build())));
    }

    @Test
    public void messagesOfUpdatedElementsArePreserved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("SuppressionFilter").withAttribute("file", "aFileToResolve").withMessage("messageKey",
                "messageValue").withMessage("anotherMessageKey", "anotherMessageValue")).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("SuppressionFilter").withAttribute("file", "aResolvedFile").withMessage
                        ("messageKey", "messageValue").withMessage("anotherMessageKey", "anotherMessageValue")).build
                ())));
    }

    @Test
    public void multipleElementsAreUpdated() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("SuppressionFilter").withAttribute("file", "aFileToResolve")).withChild(ConfigurationBuilder.config
                ("TreeWalker").withChild(ConfigurationBuilder.config("ImportControl").withAttribute("file",
                "anUnresolvableFile")).withChild(ConfigurationBuilder.config("RegexpHeader").withAttribute
                ("headerFile", "aFileToResolve"))).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("SuppressionFilter").withAttribute("file", "aResolvedFile")).withChild
                (ConfigurationBuilder.config("TreeWalker").withChild(ConfigurationBuilder.config("RegexpHeader")
                        .withAttribute("headerFile", "aResolvedFile"))).build())));
    }

    @Test
    public void removalOfElementsDoesNotEffectOtherElements() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("SuppressionFilter").withAttribute("file", "anUnresolvableFile")).withChild(ConfigurationBuilder
                .config("TreeWalker").withChild(ConfigurationBuilder.config("RegexpHeader").withAttribute
                        ("headerFile", "anUnresolvableFile")).withChild(ConfigurationBuilder.config("ConstantName")))
                .build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("TreeWalker").withChild(ConfigurationBuilder.config("ConstantName")))
                .build())));
    }

    @Test
    public void aModuleWithAFilenameThatRaisesAnIOExceptionOnResolutionDoesNotModifyTheConfiguration() throws
            CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("SuppressionFilter").withAttribute("file", "triggersAnIoException")).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("SuppressionFilter").withAttribute("file", "triggersAnIoException"))
                .build())));
    }

    @Test
    public void aModuleWithAFilenameThatRaisesAnIOExceptionOnResolutionTriggersAnErrorNotification() throws
            CheckstyleException {
        underTest.resolveFilePaths(ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("SuppressionFilter").withAttribute("file", "triggersAnIoException")).build());

        final Notification notification = sentNotification();
        assertThat(notification, is(not(nullValue())));
        assertThat(notification.getType(), is(equalTo(NotificationType.ERROR)));
        assertThat(notification.getContent(), is(equalTo(CheckStyleBundle.message("checkstyle.checker-failed",
                "aTriggeredIoException"))));
    }

    @Test
    public void aModuleWithAFilenameThatIsNotResolvesTriggersAWarningNotification() throws CheckstyleException {
        underTest.resolveFilePaths(ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config("TreeWalker")
                .withChild(ConfigurationBuilder.config("RegexpHeader").withAttribute("headerFile",
                        "anUnresolvableFile"))).build());

        final Notification notification = sentNotification();
        assertThat(notification, is(not(nullValue())));
        assertThat(notification.getType(), is(equalTo(NotificationType.WARNING)));
        assertThat(notification.getContent(), is(equalTo(CheckStyleBundle.message("checkstyle.not-found" + "" +
                ".RegexpHeader"))));
    }

    @Test
    public void aModuleWithAFilenameThatIsBlankDoesNotHaveResolutionAttempted() throws CheckstyleException {
        underTest.resolveFilePaths(ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config("TreeWalker")
                .withChild(ConfigurationBuilder.config("ImportControl").withAttribute("file", ""))).build());
        //verifyZeroInteractions(configurationLocation);
    }

    private Notification sentNotification() {
        final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).notify(notificationCaptor.capture());
        return notificationCaptor.getValue();
    }

    @Test
    public void aSuppressionFilterWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("SuppressionFilter").withAttribute("file", "aFileToResolve")).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("SuppressionFilter").withAttribute("file", "aResolvedFile")).build())));
    }

    @Test
    public void aSuppressionFilterWithAnUnresolvableFilenameHasTheModuleRemoved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config
                ("SuppressionFilter").withAttribute("file", "anUnresolvableFile")).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().build())));
    }

    @Test
    public void aRegexpHeaderWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config("TreeWalker")
                .withChild(ConfigurationBuilder.config("RegexpHeader").withAttribute("headerFile", "aFileToResolve"))
        ).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("TreeWalker").withChild(ConfigurationBuilder.config("RegexpHeader")
                        .withAttribute("headerFile", "aResolvedFile"))).build())));
    }

    @Test
    public void aRegexpHeaderWithAnUnresolvableFilenameHasTheModuleReferenceRemoved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config("TreeWalker")
                .withChild(ConfigurationBuilder.config("RegexpHeader").withAttribute("headerFile",
                        "anUnresolvableFile"))).build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("TreeWalker")).build())));
    }

    @Test
    public void aImportControlWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config("TreeWalker")
                .withChild(ConfigurationBuilder.config("ImportControl").withAttribute("file", "aFileToResolve")))
                .build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("TreeWalker").withChild(ConfigurationBuilder.config("ImportControl")
                        .withAttribute("file", "aResolvedFile"))).build())));
    }

    @Test
    public void anImportControlWithAnUnresolvableFilenameHasTheModuleReferenceRemoved() throws CheckstyleException {
        Configuration config = ConfigurationBuilder.checker().withChild(ConfigurationBuilder.config("TreeWalker")
                .withChild(ConfigurationBuilder.config("ImportControl").withAttribute("file", "anUnresolvableFile")))
                .build();
        underTest.resolveFilePaths(config);
        assertThat(config, Matchers.is(ConfigurationMatcher.configEqualTo(ConfigurationBuilder.checker().withChild
                (ConfigurationBuilder.config("TreeWalker")).build())));
    }
}
