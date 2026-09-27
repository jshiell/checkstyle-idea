package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState.GlobalConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;

public class GlobalLocationDialogueTest extends LightPlatformTestCase {

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
