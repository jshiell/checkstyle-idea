package org.infernus.idea.checkstyle;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;

public class CheckStyleConfigurableTest extends LightPlatformTestCase {

    private PluginConfigurationManager configurationManager;
    private CheckStyleConfigurable configurable;
    private CheckStyleConfigPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configurationManager = getProject().getService(PluginConfigurationManager.class);
        configurationManager.setCurrent(PluginConfigurationBuilder.defaultConfiguration(getProject()).build(), false);
        configurable = new CheckStyleConfigurable(getProject());
        panel = (CheckStyleConfigPanel) configurable.createComponent();
    }

    public void testApplyPersistsAToggleOfTheCheckbox() {
        panel.getScanBeforeCheckinCheckbox().setSelected(true);

        configurable.apply();

        assertTrue("a checkbox the user toggled should be written back",
                configurationManager.getCurrent().isScanBeforeCheckin());
    }

    public void testApplyPersistsAnUntickOfTheCheckbox() {
        setScanBeforeCheckin(true);
        configurable.reset();

        panel.getScanBeforeCheckinCheckbox().setSelected(false);

        configurable.apply();

        assertFalse("a checkbox the user unticked should be written back",
                configurationManager.getCurrent().isScanBeforeCheckin());
    }

    public void testApplyKeepsAnEditMadeOnTheCommitPage() {
        // the Commit settings page is applied before ours, so it writes while our panel is open
        setScanBeforeCheckin(true);

        configurable.apply();

        assertTrue("an untouched checkbox should not overwrite the Commit page's edit",
                configurationManager.getCurrent().isScanBeforeCheckin());
    }

    public void testApplyDoesNotDisableScrollToSource() {
        setScrollToSource(true);

        configurable.apply();

        assertTrue("applying the settings should not reset a flag the panel does not own",
                configurationManager.getCurrent().isScrollToSource());
    }

    public void testScrollToSourceAloneIsNotAModification() {
        configurable.reset();

        setScrollToSource(true);

        assertFalse("a flag the panel does not show should not mark the page as modified",
                configurable.isModified());
    }

    private void setScrollToSource(final boolean scrollToSource) {
        configurationManager.setCurrent(PluginConfigurationBuilder
                .from(configurationManager.getCurrent())
                .withScrollToSource(scrollToSource)
                .build(), false);
    }

    private void setScanBeforeCheckin(final boolean scanBeforeCheckin) {
        configurationManager.setCurrent(PluginConfigurationBuilder
                .from(configurationManager.getCurrent())
                .withScanBeforeCheckin(scanBeforeCheckin)
                .build(), false);
    }
}
