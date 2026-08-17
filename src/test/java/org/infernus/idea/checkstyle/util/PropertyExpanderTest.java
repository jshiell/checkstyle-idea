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

    @Test
    void aReferenceToABuiltInWithABlankValueIsLeftVerbatim() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "${basedir}/suppressions.xml"),
                Map.of("basedir", "   "));

        assertThat(expanded.get("suppressions"), is("${basedir}/suppressions.xml"));
    }

    @Test
    void aKeyNamedAfterABuiltInResolvesToTheBuiltInValue() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("basedir", "${basedir}"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("basedir"), is("/a/module"));
    }

    @Test
    void keysAreNeverExpanded() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("${basedir}", "a value"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded, is(Map.of("${basedir}", "a value")));
    }

    @Test
    void substitutedTextIsNeverItselfExpanded() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "${basedir}/suppressions.xml"),
                Map.of("basedir", "${basedir}", "nested", "/should/not/appear"));

        assertThat(expanded.get("suppressions"), is("${basedir}/suppressions.xml"));
    }

    @Test
    void aValueWithNoReferencesIsUnchanged() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "/a/literal/path$/with-no-references"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("suppressions"), is("/a/literal/path$/with-no-references"));
    }

    @Test
    void anUnclosedReferenceIsLeftVerbatim() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "${basedir/suppressions.xml"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("suppressions"), is("${basedir/suppressions.xml"));
    }

    @Test
    void anEmptyReferenceNameIsLeftVerbatim() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "${}/suppressions.xml"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("suppressions"), is("${}/suppressions.xml"));
    }

    @Test
    void aNestedReferenceIsResolvedAtItsFirstClosingBrace() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("suppressions", "${a${basedir}}"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("suppressions"), is("${a${basedir}}"));
    }

    @Test
    void oneUserPropertyIsNotExpandedIntoAnother() {
        Map<String, String> expanded = PropertyExpander.expand(
                Map.of("root", "/a/module", "suppressions", "${root}/suppressions.xml"),
                Map.of("basedir", "/a/module"));

        assertThat(expanded.get("suppressions"), is("${root}/suppressions.xml"));
    }
}
