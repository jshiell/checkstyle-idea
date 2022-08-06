package org.infernus.idea.checkstyle.service;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class SimpleResolverTest {
    private Map<String, String> props;

    @Before
    public void beforeTest() {
        props = new HashMap<>();
        props.put("key1", "value1");
        props.put("key2", "value2");
    }

    @Test
    public void testResolve1() {
        assertEquals("value1", new SimpleResolver(props).resolve("key1"));
        assertEquals("value2", new SimpleResolver(props).resolve("key2"));
    }

    @Test
    public void testNotFound() {
        assertNull(new SimpleResolver(props).resolve("unknownKey"));
        assertNull(new SimpleResolver(props).resolve(null));
    }

    @Test
    public void testNoProps() {
        assertNull(new SimpleResolver(emptyMap()).resolve("key1"));
        assertNull(new SimpleResolver(emptyMap()).resolve(null));
    }
}
