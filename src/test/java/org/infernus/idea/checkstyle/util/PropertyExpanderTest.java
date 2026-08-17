package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.Test;

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
}
