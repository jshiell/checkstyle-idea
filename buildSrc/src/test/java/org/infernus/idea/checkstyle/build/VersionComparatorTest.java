package org.infernus.idea.checkstyle.build;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Unit tests of {@link VersionComparator}.
 *
 * @author Thomas Jensen
 */
public class VersionComparatorTest {
    @Test
    public void testComp() {
        VersionComparator testee = new VersionComparator();

        assertTrue(testee.compare("6.1", "6.1.2") < 0);
        assertTrue(testee.compare("6.1.2", "6.1") > 0);
        assertTrue(testee.compare("6.2", "6.1.2") > 0);
        assertTrue(testee.compare("6.1.2", "6.2") < 0);
        assertTrue(testee.compare("6.1", "7.1") < 0);
        assertTrue(testee.compare("7.1", "6.1") > 0);
        assertTrue(testee.compare("6.1.3", "6.1.2") > 0);
        assertTrue(testee.compare("6.1.2", "6.1.3") < 0);
        assertTrue(testee.compare("6.1", "foo") < 0);
        assertTrue(testee.compare("foo", "6.1") > 0);
        assertTrue(testee.compare("foo", "FOO") > 0);
        assertTrue(testee.compare("FOO", "foo") < 0);
        assertTrue(testee.compare("6.1", null) < 0);
        assertTrue(testee.compare(null, "6.1") > 0);
        assertEquals(0, testee.compare(null, null));
        assertEquals(0, testee.compare("6.1.2", "6.1.2"));
        assertEquals(0, testee.compare("6.1", "6.1"));
        assertEquals(0, testee.compare("foo", "foo"));
        assertTrue(testee.compare("6", "0815") > 0);  // lex. order b/c "6" does not match version pattern
        assertTrue(testee.compare("6.1", "6.x") < 0);
        assertTrue(testee.compare("6.x", "6.1") > 0);
        assertTrue(testee.compare("6.1.8", "6.1.x") < 0);
        assertTrue(testee.compare("6.1.x", "6.1.8") > 0);
    }
}
