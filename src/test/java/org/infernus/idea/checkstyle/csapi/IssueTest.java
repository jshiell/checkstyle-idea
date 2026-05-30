package org.infernus.idea.checkstyle.csapi;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class IssueTest {

    @Test
    void constructorStoresAllFields() {
        Issue issue = new Issue("MyFile.java", 10, 3, "Some error", SeverityLevel.Error, "com.example.FooCheck");

        assertThat(issue.fileName, is("MyFile.java"));
        assertThat(issue.lineNumber, is(10));
        assertThat(issue.columnNumber, is(3));
        assertThat(issue.message, is("Some error"));
        assertThat(issue.severityLevel, is(SeverityLevel.Error));
        assertThat(issue.sourceName, is("com.example.FooCheck"));
    }

    @Test
    void constructorAllowsNullFields() {
        Issue issue = new Issue(null, 0, 0, null, SeverityLevel.Ignore, null);

        assertThat(issue.fileName, is(org.hamcrest.Matchers.nullValue()));
        assertThat(issue.message, is(org.hamcrest.Matchers.nullValue()));
        assertThat(issue.sourceName, is(org.hamcrest.Matchers.nullValue()));
    }
}
