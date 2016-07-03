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

    private static final String A_FILE_TO_RESOLVE = "aFileToResolve";
    private static final String A_RESOLVED_FILE = "aResolvedFile";
    private static final String TRIGGERS_AN_IO_EXCEPTION = "triggersAnIoException";
    private static final String TREE_WALKER = "TreeWalker";
    private static final String PROPERTY = "property";
    private static final String TAB_WIDTH = "tabWidth";
    private static final String SUPPRESSION_FILTER = "SuppressionFilter";
    private static final String IMPORT_CONTROL = "ImportControl";
    private static final String AN_UNRESOLVABLE_FILE = "anUnresolvableFile";
    private static final String REGEXP_HEADER = "RegexpHeader";
    private static final String HEADER_FILE = "headerFile";
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

        when(configurationLocation.resolveAssociatedFile(A_FILE_TO_RESOLVE, module))
                .thenReturn(A_RESOLVED_FILE);
        when(configurationLocation.resolveAssociatedFile(TRIGGERS_AN_IO_EXCEPTION, module))
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
                        .withChild(config(TREE_WALKER)
                                .withChild(config(PROPERTY)
                                        .withAttribute("name", TAB_WIDTH)
                                        .withAttribute("value", "7")))
                        .build()),
                is(equalTo(7)));
    }

    @Test
    public void aTabWidthPropertyWithANonIntegerValueReturnsTheDefault() {
        assertThat(
                underTest.tabWidth(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(PROPERTY)
                                        .withAttribute("name", TAB_WIDTH)
                                        .withAttribute("value", "dd")))
                        .build()),
                is(equalTo(8)));
    }

    @Test
    public void aTabWidthPropertyWithNoValueReturnsTheDefault() {
        assertThat(
                underTest.tabWidth(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(PROPERTY)
                                        .withAttribute("name", TAB_WIDTH)))
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
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", A_FILE_TO_RESOLVE)
                                .withChild(config("aChildModule")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", A_RESOLVED_FILE)
                                .withChild(config("aChildModule")))
                        .build())));
    }

    @Test
    public void attributesOfUpdatedElementsArePreserved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(IMPORT_CONTROL)
                                        .withAttribute("file", A_FILE_TO_RESOLVE)
                                        .withAttribute("anotherAttribute", "anotherValue")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(IMPORT_CONTROL)
                                        .withAttribute("file", A_RESOLVED_FILE)
                                        .withAttribute("anotherAttribute", "anotherValue")))
                        .build())));
    }

    @Test
    public void messagesOfUpdatedElementsArePreserved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", A_FILE_TO_RESOLVE)
                                .withMessage("messageKey", "messageValue")
                                .withMessage("anotherMessageKey", "anotherMessageValue"))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", A_RESOLVED_FILE)
                                .withMessage("messageKey", "messageValue")
                                .withMessage("anotherMessageKey", "anotherMessageValue"))
                        .build())));
    }

    @Test
    public void multipleElementsAreUpdated() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", A_FILE_TO_RESOLVE))
                        .withChild(config(TREE_WALKER)
                                .withChild(config(IMPORT_CONTROL)
                                        .withAttribute("file", AN_UNRESOLVABLE_FILE))
                                .withChild(config(REGEXP_HEADER)
                                        .withAttribute(HEADER_FILE, A_FILE_TO_RESOLVE)))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", A_RESOLVED_FILE))
                        .withChild(config(TREE_WALKER)
                                .withChild(config(REGEXP_HEADER)
                                        .withAttribute(HEADER_FILE, A_RESOLVED_FILE)))
                        .build())));
    }

    @Test
    public void removalOfElementsDoesNotEffectOtherElements() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", AN_UNRESOLVABLE_FILE))
                        .withChild(config(TREE_WALKER)
                                .withChild(config(REGEXP_HEADER)
                                        .withAttribute(HEADER_FILE, AN_UNRESOLVABLE_FILE))
                                .withChild(config("ConstantName")))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config("ConstantName")))
                        .build())));
    }

    @Test
    public void aModuleWithAFilenameThatRaisesAnIOExceptionOnResolutionDoesNotModifyTheConfiguration() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", TRIGGERS_AN_IO_EXCEPTION))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", TRIGGERS_AN_IO_EXCEPTION))
                        .build())));
    }

    @Test
    public void aModuleWithAFilenameThatRaisesAnIOExceptionOnResolutionTriggersAnErrorNotification() throws CheckstyleException {
        underTest.resolveFilePaths(checker()
                .withChild(config(SUPPRESSION_FILTER)
                        .withAttribute("file", TRIGGERS_AN_IO_EXCEPTION))
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
                .withChild(config(TREE_WALKER)
                        .withChild(config(REGEXP_HEADER)
                                .withAttribute(HEADER_FILE, AN_UNRESOLVABLE_FILE)))
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
                .withChild(config(TREE_WALKER)
                        .withChild(config(IMPORT_CONTROL)
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
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", A_FILE_TO_RESOLVE))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", A_RESOLVED_FILE))
                        .build())));
    }

    @Test
    public void aSuppressionFilterWithAnUnresolvableFilenameHasTheModuleRemoved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(SUPPRESSION_FILTER)
                                .withAttribute("file", AN_UNRESOLVABLE_FILE))
                        .build()),
                is(configEqualTo(checker().build())));
    }

    @Test
    public void aRegexpHeaderWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(REGEXP_HEADER)
                                        .withAttribute(HEADER_FILE, A_FILE_TO_RESOLVE)))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(REGEXP_HEADER)
                                        .withAttribute(HEADER_FILE, A_RESOLVED_FILE)))
                        .build())));
    }

    @Test
    public void aRegexpHeaderWithAnUnresolvableFilenameHasTheModuleReferenceRemoved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(REGEXP_HEADER)
                                        .withAttribute(HEADER_FILE, AN_UNRESOLVABLE_FILE)))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(TREE_WALKER))
                        .build())));
    }

    @Test
    public void aImportControlWithAResolvableFilenameHasTheModuleUpdated() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(IMPORT_CONTROL)
                                        .withAttribute("file", A_FILE_TO_RESOLVE)))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(IMPORT_CONTROL)
                                        .withAttribute("file", A_RESOLVED_FILE)))
                        .build())));
    }

    @Test
    public void anImportControlWithAnUnresolvableFilenameHasTheModuleReferenceRemoved() throws CheckstyleException {
        assertThat(
                underTest.resolveFilePaths(checker()
                        .withChild(config(TREE_WALKER)
                                .withChild(config(IMPORT_CONTROL)
                                        .withAttribute("file", AN_UNRESOLVABLE_FILE)))
                        .build()),
                is(configEqualTo(checker()
                        .withChild(config(TREE_WALKER))
                        .build())));
    }

}
