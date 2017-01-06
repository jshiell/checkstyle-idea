package org.infernus.idea.checkstyle.service;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class SimpleResolverTest
{
    private Map<String, String> props = null;


    @Before
    public void beforeTest() {
        props = new HashMap<>();
        props.put("key1", "value1");
        props.put("key2", "value2");
    }


    @Test
    public void testResolve1() {
        Assert.assertEquals("value1", new SimpleResolver(props).resolve("key1"));
        Assert.assertEquals("value2", new SimpleResolver(props).resolve("key2"));
    }


    @Test
    public void testNotFound() {
        Assert.assertNull(new SimpleResolver(props).resolve("unknownKey"));
        Assert.assertNull(new SimpleResolver(props).resolve(null));
    }


    @SuppressWarnings("ConstantConditions")
    @Test
    public void testNoProps() {
        Assert.assertNull(new SimpleResolver(null).resolve("key1"));
        Assert.assertNull(new SimpleResolver(null).resolve(null));
    }
}
