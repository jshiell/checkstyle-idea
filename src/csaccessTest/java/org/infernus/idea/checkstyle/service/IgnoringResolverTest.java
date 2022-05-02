package org.infernus.idea.checkstyle.service;

import org.junit.Assert;
import org.junit.Test;


public class IgnoringResolverTest {
    @Test
    public void testResolve1() {
        Assert.assertEquals("", new IgnoringResolver().resolve("key"));
    }

    @Test
    public void testResolveNull() {
        Assert.assertEquals("", new IgnoringResolver().resolve(null));
    }
}
