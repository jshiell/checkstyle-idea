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
 * flipped to {@code false}, this returns {@code null} - the plugin is otherwise fully dynamic-unload
 * clean. {@code require-restart="true"} is deliberately still in {@code plugin.xml} (plan.md Step 8
 * ships in a later release, after the leak fixes in this one bake in the field), so today this
 * asserts that it is the *only* remaining blocker. When Step 8 removes the attribute, flip this
 * assertion to expect {@code null}.
 */
public class DynamicUnloadEligibilityTest extends LightPlatformTestCase {

    public void testTheOnlyDynamicUnloadBlockerIsTheDeclaredRequireRestartAttribute() {
        final var descriptor = (IdeaPluginDescriptorImpl)
                PluginManagerCore.getPlugin(PluginId.getId(CheckStylePlugin.ID_PLUGIN));
        assertNotNull("plugin descriptor not found in test sandbox", descriptor);

        final String reason = DynamicPlugins.INSTANCE.checkCanUnloadWithoutRestart(descriptor);

        assertEquals("Plugin CheckStyle-IDEA is explicitly marked as requiring restart", reason);
    }
}
