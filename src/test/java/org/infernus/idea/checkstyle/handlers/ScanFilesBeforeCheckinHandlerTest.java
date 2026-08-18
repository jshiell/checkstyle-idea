package org.infernus.idea.checkstyle.handlers;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;

import javax.swing.JCheckBox;
import java.awt.Component;
import java.awt.Container;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanFilesBeforeCheckinHandlerTest extends LightPlatformTestCase {

    private PluginConfigurationManager configurationManager;
    private ScanFilesBeforeCheckinHandler handler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configurationManager = getProject().getService(PluginConfigurationManager.class);
        setScanBeforeCheckin(false);

        final CheckinProjectPanel checkinPanel = mock(CheckinProjectPanel.class);
        when(checkinPanel.getProject()).thenReturn(getProject());
        handler = new ScanFilesBeforeCheckinHandler(checkinPanel);
    }

    public void testTheCommitOptionWritesTheCheckboxBackToTheConfiguration() {
        final RefreshableOnComponent option = handler.getBeforeCheckinConfigurationPanel();
        assertNotNull(option);

        option.restoreState();
        final JCheckBox checkBox = firstCheckBoxIn(option.getComponent());
        assertNotNull("the commit option should present a checkbox", checkBox);
        assertFalse("the checkbox should reflect the stored value", checkBox.isSelected());

        checkBox.setSelected(true);
        option.saveState();

        assertTrue("saving the commit option should store the checkbox value",
                configurationManager.getCurrent().isScanBeforeCheckin());
    }

    private JCheckBox firstCheckBoxIn(final Component component) {
        if (component instanceof JCheckBox checkBox) {
            return checkBox;
        }
        if (component instanceof Container container) {
            for (final Component child : container.getComponents()) {
                final JCheckBox found = firstCheckBoxIn(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void setScanBeforeCheckin(final boolean scanBeforeCheckin) {
        configurationManager.setCurrent(PluginConfigurationBuilder
                .from(configurationManager.getCurrent())
                .withScanBeforeCheckin(scanBeforeCheckin)
                .build(), false);
    }
}
