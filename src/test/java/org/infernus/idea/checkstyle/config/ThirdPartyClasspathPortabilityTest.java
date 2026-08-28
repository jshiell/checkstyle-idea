package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.testFramework.LightPlatformTestCase;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class ThirdPartyClasspathPortabilityTest extends LightPlatformTestCase {

    public void testPathsUnderTheUsersHomeDirectoryArePortablyCollapsedAndExpanded() {
        PathMacroManager macroManager = PathMacroManager.getInstance(getProject());
        String pathUnderHome = normalise(
                new File(System.getProperty("user.home"), "libs/sevntu-checks.jar").getAbsolutePath());

        String collapsed = macroManager.collapsePath(pathUnderHome);
        // Semantic property first: some *portable* token was substituted. Don't over-assert which
        // one - $PROJECT_DIR$'s ancestor-hierarchy replacement can plausibly win over $USER_HOME$
        // if the light test project happens to be rooted under the home directory. Either is fine;
        // only a failure to substitute anything at all is a real problem.
        assertThat(collapsed, is(not(equalTo(pathUnderHome))));
        assertThat(normalise(macroManager.expandPath(collapsed)), is(equalTo(pathUnderHome)));

        // Softer, informational check of which token was actually used - not a hard requirement.
        // If this specific assertion is the *only* one failing, that means portability still works
        // via a different macro; relax this line rather than treating it as a real defect.
        assertThat(collapsed, containsString("$USER_HOME$"));
    }

    public void testProjectDirTokenExpandsOnLoad() {
        // Closes the "collapse observed, expand not observed" gap: proves the load-direction half
        // that the .idea/checkstyle-idea.xml evidence alone does not.
        PathMacroManager macroManager = PathMacroManager.getInstance(getProject());
        String expanded = macroManager.expandPath("$PROJECT_DIR$/some.jar");
        assertThat(expanded, is(not(equalTo("$PROJECT_DIR$/some.jar"))));
        assertThat(new File(expanded).isAbsolute(), is(true));
    }

    public void testThirdPartyClasspathStateOptsIntoPathMacroSubstitution() {
        // The one assertion in this file that can actually be broken by a future change to *this*
        // codebase, as opposed to the platform - the real regression guard.
        // usePathMacroManager() lives on @Storage (not @State) as of IC-251.29188.72 - verified by
        // decompiling com.intellij.openapi.components.Storage against this project's target build.
        State state = ProjectConfigurationState.class.getAnnotation(State.class);
        Storage storage = state.storages()[0];
        assertThat(storage.usePathMacroManager(), is(true));
    }

    private static String normalise(String path) {
        return path.replace(File.separatorChar, '/');
    }

}
