package org.infernus.idea.checkstyle.model;

import org.infernus.idea.checkstyle.TestHelper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for credential redaction in HTTPURLConfigurationLocation log output.
 */
public class HTTPURLConfigurationLocationRedactionTest {

    @Test
    public void credentialsAreRemovedFromRedactedLocation() {
        final HTTPURLConfigurationLocation location = locationWithUrl("http://user:secret@example.com/path");
        assertThat(location.redactedLocation(), not(containsString("secret")));
        assertThat(location.redactedLocation(), not(containsString("user")));
    }

    @Test
    public void hostAndPathArePreservedAfterRedaction() {
        final HTTPURLConfigurationLocation location = locationWithUrl("http://user:secret@example.com/path");
        assertThat(location.redactedLocation(), containsString("example.com"));
        assertThat(location.redactedLocation(), containsString("/path"));
    }

    @Test
    public void urlWithoutCredentialsIsReturnedUnchanged() {
        final String url = "http://example.com/path";
        final HTTPURLConfigurationLocation location = locationWithUrl(url);
        assertThat(location.redactedLocation(), is(url));
    }

    @Test
    public void malformedUrlFallsBackToRawLocation() {
        final HTTPURLConfigurationLocation location = locationWithUrl("not a valid url");
        assertThat(location.redactedLocation(), is("not a valid url"));
    }

    private HTTPURLConfigurationLocation locationWithUrl(final String url) {
        final HTTPURLConfigurationLocation location =
                new HTTPURLConfigurationLocation(TestHelper.mockProject(), UUID.randomUUID().toString());
        location.setDescription("test");
        location.setLocation(url);
        return location;
    }
}
