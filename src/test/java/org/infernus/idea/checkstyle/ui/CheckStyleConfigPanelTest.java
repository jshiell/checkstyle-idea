package org.infernus.idea.checkstyle.ui;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;

public class CheckStyleConfigPanelTest extends LightPlatformTestCase {

    private PluginConfigurationManager configurationManager;
    private CheckStyleConfigPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configurationManager = getProject().getService(PluginConfigurationManager.class);
        configurationManager.setCurrent(PluginConfigurationBuilder.defaultConfiguration(getProject()).build(), false);
        panel = new CheckStyleConfigPanel(getProject());
    }

    public void testAnUntouchedCheckboxReturnsTheLiveScanBeforeCheckin() {
        panel.showPluginConfiguration(setScanBeforeCheckin(true));

        assertTrue("an untouched checkbox should return the live flag",
                panel.getPluginConfiguration().isScanBeforeCheckin());
    }

    public void testAnUntouchedCheckboxPicksUpAnEditMadeAfterThePanelWasShown() {
        panel.showPluginConfiguration(setScanBeforeCheckin(false));

        // the Commit settings page writes while our panel is open
        setScanBeforeCheckin(true);

        assertTrue("an untouched checkbox should not overwrite a later edit",
                panel.getPluginConfiguration().isScanBeforeCheckin());
    }

    public void testATickedCheckboxIsWrittenBack() {
        panel.showPluginConfiguration(setScanBeforeCheckin(false));

        panel.getScanBeforeCheckinCheckbox().setSelected(true);

        assertTrue("a checkbox the user ticked should be written back",
                panel.getPluginConfiguration().isScanBeforeCheckin());
    }

    public void testScrollToSourceIsCarriedThrough() {
        final PluginConfiguration configuration = PluginConfigurationBuilder
                .from(configurationManager.getCurrent())
                .withScrollToSource(true)
                .build();
        configurationManager.setCurrent(configuration, false);
        panel.showPluginConfiguration(configuration);

        assertTrue("a flag the panel has no widget for should not be reset",
                panel.getPluginConfiguration().isScrollToSource());
    }

    private PluginConfiguration setScanBeforeCheckin(final boolean scanBeforeCheckin) {
        final PluginConfiguration configuration = PluginConfigurationBuilder
                .from(configurationManager.getCurrent())
                .withScanBeforeCheckin(scanBeforeCheckin)
                .build();
        configurationManager.setCurrent(configuration, false);
        return configuration;
    }
}
