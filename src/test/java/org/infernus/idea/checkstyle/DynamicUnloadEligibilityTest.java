package org.infernus.idea.checkstyle;

import com.intellij.ide.plugins.DynamicPlugins;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.testFramework.LightPlatformTestCase;

/**
 * Calls the platform's own dynamic-unload gate, {@code DynamicPlugins.checkCanUnloadWithoutRestart},
 * against this plugin's real loaded descriptor. This is authoritative where the
 * {@link PluginDescriptorDynamicUnloadTripwireTest} XML checks and the manual EP-dynamism review in
 * plan.md are not: it is the platform's actual runtime decision, including extension-point dynamism
 * that lives in the platform's own descriptors rather than ours, which nothing else here can assert.
 * <p>
 * Confirmed by a throwaway spike (plan.md Step 7) that with {@code require-restart} temporarily
 * flipped to {@code false}, this returned {@code null} - the plugin was otherwise fully dynamic-unload
 * clean, meaning {@code require-restart} was the only remaining blocker. plan.md Step 8 has now removed
 * the attribute for real, so this asserts the platform agrees the plugin is fully unload-clean.
 */
public class DynamicUnloadEligibilityTest extends LightPlatformTestCase {

    public void testThePluginIsFullyDynamicUnloadClean() {
        final var descriptor = (IdeaPluginDescriptorImpl)
                PluginManagerCore.getPlugin(PluginId.getId(CheckStylePlugin.ID_PLUGIN));
        assertNotNull("plugin descriptor not found in test sandbox", descriptor);

        final String reason = DynamicPlugins.INSTANCE.checkCanUnloadWithoutRestart(descriptor);

        assertNull("expected no dynamic-unload blocker, but got: " + reason, reason);
    }
}
