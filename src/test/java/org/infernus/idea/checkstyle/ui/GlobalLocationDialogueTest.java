package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState.GlobalConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GlobalLocationDialogueTest extends LightPlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestDialogManager.setTestDialog(TestDialog.OK, getTestRootDisposable());
    }

    public void testSelectingAFileWithPropertiesMovesToThePropertiesStepInsteadOfClosing() throws IOException {
        final Path rulesFile = Files.createTempFile("global-location-test", ".xml");
        try {
            Files.writeString(rulesFile, """
                    <module name="Checker">
                    <module name="TestFilter">
                      <property name="file" value="${my-property}/a-file.xml"/>
                    </module>
                    </module>""");

            final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null, getProject());
            dialogue.fileLocationField().setText(rulesFile.toAbsolutePath().toString());
            dialogue.descriptionField().setText("My Rules");

            dialogue.commitButton().doClick();

            assertFalse("dialogue should not have closed yet", dialogue.isOK());
            assertTrue("Previous should now be enabled on the properties step", dialogue.previousButton().isEnabled());
            assertEquals(CheckStyleBundle.message("config.file.okay.text"), dialogue.commitButton().getText());

            dialogue.commitButton().doClick();
            assertTrue(dialogue.isOK());
        } finally {
            Files.deleteIfExists(rulesFile);
        }
    }

    public void testSelectingAFileWithNoPropertiesFinishesImmediately() throws IOException {
        final Path rulesFile = Files.createTempFile("global-location-test", ".xml");
        try {
            Files.writeString(rulesFile, "<module name=\"Checker\"/>");

            final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null, getProject());
            dialogue.fileLocationField().setText(rulesFile.toAbsolutePath().toString());
            dialogue.descriptionField().setText("My Rules");

            dialogue.commitButton().doClick();

            assertTrue(dialogue.isOK());
        } finally {
            Files.deleteIfExists(rulesFile);
        }
    }

    public void testPreviousFromThePropertiesStepReturnsToSelectWithNextRelabelled() throws IOException {
        final Path rulesFile = Files.createTempFile("global-location-test", ".xml");
        try {
            Files.writeString(rulesFile, """
                    <module name="Checker">
                    <module name="TestFilter">
                      <property name="file" value="${my-property}/a-file.xml"/>
                    </module>
                    </module>""");

            final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null, getProject());
            try {
                dialogue.fileLocationField().setText(rulesFile.toAbsolutePath().toString());
                dialogue.descriptionField().setText("My Rules");
                dialogue.commitButton().doClick();

                dialogue.previousButton().doClick();

                assertFalse(dialogue.previousButton().isEnabled());
                assertEquals(CheckStyleBundle.message("config.file.next.text"), dialogue.commitButton().getText());
            } finally {
                dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
            }
        } finally {
            Files.deleteIfExists(rulesFile);
        }
    }

    public void testPreviousButtonIsDisabledOnTheSelectStep() {
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null);
        try {
            assertFalse(dialogue.previousButton().isEnabled());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testCommitButtonIsLabelledNextOnTheSelectStep() {
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null);
        try {
            assertEquals(CheckStyleBundle.message("config.file.next.text"), dialogue.commitButton().getText());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testCommittingWithAValidLocationAndDescriptionClosesTheDialogueWithOk() throws IOException {
        final Path rulesFile = Files.createTempFile("global-location-test", ".xml");
        try {
            Files.writeString(rulesFile, "<module name=\"Checker\"/>");

            final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null, getProject());
            dialogue.fileLocationField().setText(rulesFile.toAbsolutePath().toString());
            dialogue.descriptionField().setText("My Rules");

            dialogue.commitButton().doClick();

            assertTrue(dialogue.isOK());
            final GlobalConfigurationLocation location = dialogue.getGlobalConfigurationLocation();
            assertEquals(rulesFile.toAbsolutePath().toString(), location.location);
            assertEquals("My Rules", location.description);
        } finally {
            Files.deleteIfExists(rulesFile);
        }
    }

    public void testSelectingClasspathFinishesImmediatelyWithoutResolvingIt() {
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null, getProject());
        dialogue.classpathLocationRadio().doClick();
        dialogue.classpathLocationField().setText("this/classpath/resource/does-not-exist.xml");
        dialogue.descriptionField().setText("My Rules");

        dialogue.commitButton().doClick();

        assertTrue("a classpath location must never be resolved from this dialogue", dialogue.isOK());
        final GlobalConfigurationLocation location = dialogue.getGlobalConfigurationLocation();
        assertEquals(ConfigurationType.PLUGIN_CLASSPATH.name(), location.type);
        assertEquals("this/classpath/resource/does-not-exist.xml", location.location);
    }

    public void testFinishingFromThePropertiesStepCommitsItsValuesIntoTheDto() throws IOException {
        final Path rulesFile = Files.createTempFile("global-location-test", ".xml");
        try {
            Files.writeString(rulesFile, """
                    <module name="Checker">
                    <module name="TestFilter">
                      <property name="file" value="${my-property}/a-file.xml" default="a-default-value"/>
                    </module>
                    </module>""");

            final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null, getProject());
            dialogue.fileLocationField().setText(rulesFile.toAbsolutePath().toString());
            dialogue.descriptionField().setText("My Rules");
            dialogue.commitButton().doClick();

            dialogue.commitButton().doClick();

            assertTrue(dialogue.isOK());
            final GlobalConfigurationLocation location = dialogue.getGlobalConfigurationLocation();
            assertEquals("a-default-value", location.properties.get("my-property"));
        } finally {
            Files.deleteIfExists(rulesFile);
        }
    }

    public void testFinishingWithNoPropertiesFoundLeavesThePropertiesMapNull() throws IOException {
        final Path rulesFile = Files.createTempFile("global-location-test", ".xml");
        try {
            Files.writeString(rulesFile, "<module name=\"Checker\"/>");

            final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null, getProject());
            dialogue.fileLocationField().setText(rulesFile.toAbsolutePath().toString());
            dialogue.descriptionField().setText("My Rules");
            dialogue.commitButton().doClick();

            assertNull(dialogue.getGlobalConfigurationLocation().properties);
        } finally {
            Files.deleteIfExists(rulesFile);
        }
    }

    public void testCommittingWithABlankLocationDoesNotCloseTheDialogue() {
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null);
        try {
            dialogue.descriptionField().setText("My Rules");

            dialogue.commitButton().doClick();

            assertFalse(dialogue.isOK());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testCommittingWithABlankDescriptionDoesNotCloseTheDialogue() {
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null);
        try {
            dialogue.fileLocationField().setText("/path/to/rules.xml");

            dialogue.commitButton().doClick();

            assertFalse(dialogue.isOK());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testFileRadioIsSelectedByDefaultForANewLocation() {
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null);
        try {
            assertTrue(dialogue.fileLocationRadio().isSelected());
            assertTrue(dialogue.fileLocationField().isEnabled());
            assertFalse(dialogue.urlLocationField().isEnabled());
            assertFalse(dialogue.classpathLocationField().isEnabled());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testSelectingUrlRadioEnablesTheUrlFieldAndDisablesTheOthers() {
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null);
        try {
            dialogue.urlLocationRadio().doClick();

            assertTrue(dialogue.urlLocationField().isEnabled());
            assertTrue(dialogue.insecureHttpCheckbox().isEnabled());
            assertFalse(dialogue.fileLocationField().isEnabled());
            assertFalse(dialogue.classpathLocationField().isEnabled());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testSelectingClasspathRadioEnablesTheClasspathFieldAndDisablesTheOthers() {
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null);
        try {
            dialogue.classpathLocationRadio().doClick();

            assertTrue(dialogue.classpathLocationField().isEnabled());
            assertFalse(dialogue.fileLocationField().isEnabled());
            assertFalse(dialogue.urlLocationField().isEnabled());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testEditModeSeedsTheFileRadioAndFieldFromAnExistingLocalFileLocation() {
        final GlobalConfigurationLocation existing = new GlobalConfigurationLocation(
                "an-id", ConfigurationType.LOCAL_FILE.name(), "/path/to/rules.xml", "My Rules");
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(existing);
        try {
            assertTrue(dialogue.fileLocationRadio().isSelected());
            assertEquals("/path/to/rules.xml", dialogue.fileLocationField().getText());
            assertEquals("My Rules", dialogue.descriptionField().getText());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testEditModeSeedsTheUrlRadioAndFieldFromAnExistingHttpUrlLocation() {
        final GlobalConfigurationLocation existing = new GlobalConfigurationLocation(
                "an-id", ConfigurationType.HTTP_URL.name(), "https://example.com/rules.xml", "My Rules");
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(existing);
        try {
            assertTrue(dialogue.urlLocationRadio().isSelected());
            assertEquals("https://example.com/rules.xml", dialogue.urlLocationField().getText());
            assertFalse(dialogue.insecureHttpCheckbox().isSelected());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testEditModeSeedsTheUrlRadioAndInsecureCheckboxFromAnExistingInsecureHttpUrlLocation() {
        final GlobalConfigurationLocation existing = new GlobalConfigurationLocation(
                "an-id", ConfigurationType.INSECURE_HTTP_URL.name(), "https://example.com/rules.xml", "My Rules");
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(existing);
        try {
            assertTrue(dialogue.urlLocationRadio().isSelected());
            assertTrue(dialogue.insecureHttpCheckbox().isSelected());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }

    public void testEditModeSeedsTheClasspathRadioAndFieldFromAnExistingClasspathLocation() {
        final GlobalConfigurationLocation existing = new GlobalConfigurationLocation(
                "an-id", ConfigurationType.PLUGIN_CLASSPATH.name(), "checkstyle/my-rules.xml", "My Rules");
        final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(existing);
        try {
            assertTrue(dialogue.classpathLocationRadio().isSelected());
            assertEquals("checkstyle/my-rules.xml", dialogue.classpathLocationField().getText());
        } finally {
            dialogue.close(DialogWrapper.CANCEL_EXIT_CODE);
        }
    }
}
