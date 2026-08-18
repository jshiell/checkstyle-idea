package org.infernus.idea.checkstyle.ui;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;

public class CheckStyleConfigPanelTest extends LightPlatformTestCase {

    private CheckStyleConfigPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new CheckStyleConfigPanel(getProject());
    }

    public void testScanBeforeCheckinSurvivesARoundTrip() {
        panel.showPluginConfiguration(configurationWithScanBeforeCheckin(true));

        assertTrue("the panel should return the flag it was shown",
                panel.getPluginConfiguration().isScanBeforeCheckin());
    }

    private PluginConfiguration configurationWithScanBeforeCheckin(final boolean scanBeforeCheckin) {
        return PluginConfigurationBuilder.defaultConfiguration(getProject())
                .withScanBeforeCheckin(scanBeforeCheckin)
                .build();
    }
}
