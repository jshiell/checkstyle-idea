package org.infernus.idea.checkstyle;

import org.junit.Assert;
import org.junit.Test;


/**
 * Unit tests of {@link VersionComparator}.
 */
public class VersionComparatorTest {
    @Test
    public void testComp() {
        VersionComparator testee = new VersionComparator();

        Assert.assertTrue(testee.compare("8.1", "8.1.2") < 0);
        Assert.assertTrue(testee.compare("8.1.2", "8.1") > 0);
        Assert.assertTrue(testee.compare("8.2", "8.1.2") > 0);
        Assert.assertTrue(testee.compare("8.1.2", "8.2") < 0);
        Assert.assertTrue(testee.compare("8.1", "9.1") < 0);
        Assert.assertTrue(testee.compare("9.1", "8.1") > 0);
        Assert.assertTrue(testee.compare("8.1.3", "8.1.2") > 0);
        Assert.assertTrue(testee.compare("8.1.2", "8.1.3") < 0);
        Assert.assertTrue(testee.compare("8.1", "foo") < 0);
        Assert.assertTrue(testee.compare("foo", "8.1") > 0);
        Assert.assertTrue(testee.compare("foo", "FOO") > 0);
        Assert.assertTrue(testee.compare("FOO", "foo") < 0);
        Assert.assertTrue(testee.compare("8.1", null) < 0);
        Assert.assertTrue(testee.compare(null, "8.1") > 0);
        Assert.assertTrue(testee.compare(null, null) == 0);
        Assert.assertTrue(testee.compare("8.1.2", "8.1.2") == 0);
        Assert.assertTrue(testee.compare("8.1", "8.1") == 0);
        Assert.assertTrue(testee.compare("foo", "foo") == 0);
        Assert.assertTrue(testee.compare("8", "0815") > 0);  // lex. order b/c "6" does not match version pattern
        Assert.assertTrue(testee.compare("8.1", "8.x") < 0);
        Assert.assertTrue(testee.compare("8.x", "8.1") > 0);
        Assert.assertTrue(testee.compare("8.1.8", "8.1.x") < 0);
        Assert.assertTrue(testee.compare("8.1.x", "8.1.8") > 0);
    }
}
