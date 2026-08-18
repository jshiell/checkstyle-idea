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
        setScanBeforeCheckin(false);
        configurable = new CheckStyleConfigurable(getProject());
        panel = (CheckStyleConfigPanel) configurable.createComponent();
    }

    public void testApplyPersistsAToggleOfTheCheckbox() {
        panel.getScanBeforeCheckinCheckbox().setSelected(true);

        configurable.apply();

        assertTrue("a checkbox the user toggled should be written back",
                configurationManager.getCurrent().isScanBeforeCheckin());
    }

    public void testApplyKeepsAnEditMadeOnTheCommitPage() {
        // the Commit settings page is applied before ours, so it writes while our panel is open
        setScanBeforeCheckin(true);

        configurable.apply();

        assertTrue("an untouched checkbox should not overwrite the Commit page's edit",
                configurationManager.getCurrent().isScanBeforeCheckin());
    }

    private void setScanBeforeCheckin(final boolean scanBeforeCheckin) {
        configurationManager.setCurrent(PluginConfigurationBuilder
                .from(configurationManager.getCurrent())
                .withScanBeforeCheckin(scanBeforeCheckin)
                .build(), false);
    }
}
