package org.infernus.idea.checkstyle.model;

import org.infernus.idea.checkstyle.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLConnection;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InsecureHTTPURLConfigurationLocationTest {

    private InsecureHTTPURLConfigurationLocation underTest;

    @BeforeEach
    void setUp() {
        underTest = new InsecureHTTPURLConfigurationLocation(TestHelper.mockProject(), UUID.randomUUID().toString());
        underTest.setDescription("test-insecure-location");
    }

    @Test
    void typeIsInsecureHttpUrl() {
        assertThat(underTest.getType(), instanceOf(ConfigurationType.class));
        assertThat(underTest.getType().toString(), org.hamcrest.Matchers.is(ConfigurationType.INSECURE_HTTP_URL.toString()));
    }

    @Test
    void cloneReturnsNewInstanceOfSameType() {
        underTest.setLocation("http://example.com/checkstyle.xml");
        Object cloned = underTest.clone();
        assertThat(cloned, instanceOf(InsecureHTTPURLConfigurationLocation.class));
        assertThat(cloned, not(sameInstance(underTest)));
    }

    @Test
    void connectionToPlainHttpUrlDoesNotThrow() throws IOException {
        underTest.setLocation("http://example.com/checkstyle.xml");
        // openConnection() on a plain http URL should succeed (no SSL setup needed)
        URLConnection conn = underTest.connectionTo("http://example.com/checkstyle.xml");
        assertNotNull(conn);
    }
}
