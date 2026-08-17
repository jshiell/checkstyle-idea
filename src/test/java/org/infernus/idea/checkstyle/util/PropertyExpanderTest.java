package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PropertyExpanderTest {

    @Test
    void aValueThatIsASingleReferenceBecomesTheBuiltInValue() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("baseDir", "${basedir}"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("baseDir"), is("/a/module"));
    }

    @Test
    void aReferenceIsExpandedWhereItSitsWithinALongerValue() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "${basedir}/gradle/checkstyle-exclude.xml"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("suppressions"), is("/a/module/gradle/checkstyle-exclude.xml"));
    }

    @Test
    void everyReferenceInAValueIsExpanded() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("paths", "${basedir}:${config_loc}:${basedir}"),
                Map.of("basedir", "/a/module", "config_loc", "/a/rules"));

        assertThat(expanded.get("paths"), is("/a/module:/a/rules:/a/module"));
    }

    @Test
    void aReferenceToAnUnknownNameIsLeftVerbatim() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "${nosuchthing}/suppressions.xml"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("suppressions"), is("${nosuchthing}/suppressions.xml"));
    }

    @Test
    void aReferenceToABuiltInWithANullValueIsLeftVerbatim() {
        Map<String, String> builtIns = new HashMap<>();
        builtIns.put("basedir", null);

        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "${basedir}/suppressions.xml"), builtIns);

        assertThat(expanded.get("suppressions"), is("${basedir}/suppressions.xml"));
    }
}
