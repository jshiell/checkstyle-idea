package org.infernus.idea.checkstyle.checker;

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
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.infernus.idea.checkstyle.checker.ConfigurationBuilder.checker;
import static org.infernus.idea.checkstyle.checker.ConfigurationBuilder.config;
import static org.infernus.idea.checkstyle.checker.ConfigurationMatcher.configEqualTo;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationsTest {

    @Mock
    private ConfigurationLocation configurationLocation;
    @Mock
    private Module module;
    @Mock
    private Notifications notifications;

    private Configurations underTest;

    @Before
    public void setUp() throws IOException {
        interceptApplicationNotifications();

        when(configurationLocation.resolveAssociatedFile("aFileToResolve", module))
                .thenReturn("aResolvedFile");
        when(configurationLocation.resolveAssociatedFile("triggersAnIoException", module))
                .thenThrow(new IOException("aTriggeredIoException"));

        underTest = new Configurations(configurationLocation, module);
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
    public void aDefaultTabWidthIsEightIsUsedWhenNoTabWidthPropertyIsPresent() {
        assertThat(
                underTest.tabWidth(checker().build()),
                is(equalTo(8)));
    }

    @Test
    public void tabWidthPropertyValueIsReturnedWhenPresent() {
        assertThat(
                underTest.tabWidth(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("property")
                                        .withAttribute("name", "tabWidth")
                                        .withAttribute("value", "7")))
                        .build()),
                is(equalTo(7)));
    }

    @Test
    public void aTabWidthPropertyWithANonIntegerValueReturnsTheDefault() {
        assertThat(
                underTest.tabWidth(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("property")
                                        .withAttribute("name", "tabWidth")
                                        .withAttribute("value", "dd")))
                        .build()),
                is(equalTo(8)));
    }

    @Test
    public void aTabWidthPropertyWithNoValueReturnsTheDefault() {
        assertThat(
                underTest.tabWidth(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("property")
                                        .withAttribute("name", "tabWidth")))
                        .build()),
                is(equalTo(8)));
    }

    @Test
    public void filePathsAreNotResolvedOnANonDefaultImplementationOfConfiguration() throws CheckstyleException {
        final Configuration config = mock(Configuration.class);
        assertThat(
                underTest.resolveFilePaths(config),
                is(equalTo(config)));
        verifyZeroInteractions(config);
    }

    @Test
    public void childrenOfUpdatedElementsArePreserved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "aFileToResolve")
                                .withChild(config("aChildModule")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "aResolvedFile")
                                .withChild(config("aChildModule")))
                        .build())));
    }

    @Test
    public void attributesOfUpdatedElementsArePreserved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("ImportControl")
                                        .withAttribute("file", "aFileToResolve")
                                        .withAttribute("anotherAttribute", "anotherValue")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("ImportControl")
                                        .withAttribute("file", "aResolvedFile")
                                        .withAttribute("anotherAttribute", "anotherValue")))
                        .build())));
    }

    @Test
    public void messagesOfUpdatedElementsArePreserved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "aFileToResolve")
                                .withMessage("messageKey", "messageValue")
                                .withMessage("anotherMessageKey", "anotherMessageValue"))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "aResolvedFile")
                                .withMessage("messageKey", "messageValue")
                                .withMessage("anotherMessageKey", "anotherMessageValue"))
                        .build())));
    }

    @Test
    public void multipleElementsAreUpdated() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "aFileToResolve"))
                        .withChild(config("TreeWalker")
                                .withChild(config("ImportControl")
                                        .withAttribute("file", "anUnresolvableFile"))
                                .withChild(config("RegexpHeader")
                                        .withAttribute("headerFile", "aFileToResolve")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "aResolvedFile"))
                        .withChild(config("TreeWalker")
                                .withChild(config("RegexpHeader")
                                        .withAttribute("headerFile", "aResolvedFile")))
                        .build())));
    }

    @Test
    public void removalOfElementsDoesNotEffectOtherElements() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "anUnresolvableFile"))
                        .withChild(config("TreeWalker")
                                .withChild(config("RegexpHeader")
                                        .withAttribute("headerFile", "anUnresolvableFile"))
                                .withChild(config("ConstantName")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("ConstantName")))
                        .build())));
    }

    @Test
    public void aModuleWithAFilenameThatRaisesAnIOExceptionOnResolutionDoesNotModifyTheConfiguration() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "triggersAnIoException"))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "triggersAnIoException"))
                        .build())));
    }

    @Test
    public void aModuleWithAFilenameThatRaisesAnIOExceptionOnResolutionTriggersAnErrorNotification() throws CheckstyleException {
        underTest.resolveFilePaths(checker()
                .withChild(config("SuppressionFilter")
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
        underTest.resolveFilePaths(checker()
                .withChild(config("TreeWalker")
                        .withChild(config("RegexpHeader")
                                .withAttribute("headerFile", "anUnresolvableFile")))
                .build());

        final Notification notification = sentNotification();
        assertThat(notification, is(not(nullValue())));
        assertThat(notification.getType(), is(equalTo(NotificationType.WARNING)));
        assertThat(notification.getContent(),
                is(equalTo(CheckStyleBundle.message("checkstyle.not-found.RegexpHeader"))));
    }

    @Test
    public void aModuleWithAFilenameThatIsBlankDoesNotHaveResolutionAttempted() throws CheckstyleException {
        underTest.resolveFilePaths(checker()
                .withChild(config("TreeWalker")
                        .withChild(config("ImportControl")
                                .withAttribute("file", "")))
                .build());

        verifyZeroInteractions(configurationLocation);
    }

    private Notification sentNotification() {
        final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).notify(notificationCaptor.capture());
        return notificationCaptor.getValue();
    }

    @Test
    public void aSuppressionFilterWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "aFileToResolve"))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "aResolvedFile"))
                        .build())));
    }

    @Test
    public void aSuppressionFilterWithAnUnresolvableFilenameHasTheModuleRemoved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("SuppressionFilter")
                                .withAttribute("file", "anUnresolvableFile"))
                        .build()),
                is(configEqualTo(checker().build())));
    }

    @Test
    public void aRegexpHeaderWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("RegexpHeader")
                                        .withAttribute("headerFile", "aFileToResolve")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("RegexpHeader")
                                        .withAttribute("headerFile", "aResolvedFile")))
                        .build())));
    }

    @Test
    public void aRegexpHeaderWithAnUnresolvableFilenameHasTheModuleReferenceRemoved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("RegexpHeader")
                                        .withAttribute("headerFile", "anUnresolvableFile")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("TreeWalker"))
                        .build())));
    }

    @Test
    public void aImportControlWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("ImportControl")
                                        .withAttribute("file", "aFileToResolve")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("ImportControl")
                                        .withAttribute("file", "aResolvedFile")))
                        .build())));
    }

    @Test
    public void anImportControlWithAnUnresolvableFilenameHasTheModuleReferenceRemoved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config("TreeWalker")
                                .withChild(config("ImportControl")
                                        .withAttribute("file", "anUnresolvableFile")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config("TreeWalker"))
                        .build())));
    }

}
