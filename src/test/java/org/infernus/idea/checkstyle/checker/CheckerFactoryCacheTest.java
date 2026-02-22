package org.infernus.idea.checkstyle.checker;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CheckerFactoryCacheTest extends LightPlatformTestCase {

    private CheckStyleChecker checkStyleChecker;

    private CheckerFactoryCache underTest;
    private ConfigurationLocation location;

    public void setUp() throws Exception {
        super.setUp();

        checkStyleChecker = mock(CheckStyleChecker.class);

        underTest = new CheckerFactoryCache();
        location = new StringConfigurationLocation("<module/>", getProject());
    }

    public void testGetReturnsEmptyWhenNothingHasBeenPut() {
        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    public void testGetReturnsTheCachedCheckerAfterPut() {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);

        underTest.put(location, null, cachedChecker);

        assertThat(underTest.get(location, null), is(Optional.of(cachedChecker)));
    }

    public void testGetReturnsTheSameInstanceThatWasPut() {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);

        underTest.put(location, null, cachedChecker);

        assertThat(underTest.get(location, null).orElseThrow(), is(sameInstance(cachedChecker)));
    }

    public void testGetReturnsEmptyForAnExpiredEntry() throws Exception {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);
        backdateTimestamp(cachedChecker, 61_000);

        underTest.put(location, null, cachedChecker);

        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    public void testGetRemovesExpiredEntryFromCacheSoSubsequentGetAlsoReturnsEmpty() throws Exception {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);
        backdateTimestamp(cachedChecker, 61_000);
        underTest.put(location, null, cachedChecker);

        underTest.get(location, null); // first get triggers cleanup

        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    public void testInvalidateClearsAllEntries() {
        underTest.put(location, null, new CachedChecker(checkStyleChecker));

        underTest.invalidate();

        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    public void testInvalidateDestroysAllCachedCheckers() {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);
        underTest.put(location, null, cachedChecker);

        underTest.invalidate();

        verify(checkStyleChecker).destroy();
    }

    public void testDisposeCallsInvalidate() {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);
        underTest.put(location, null, cachedChecker);

        underTest.dispose();

        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    private void backdateTimestamp(final CachedChecker cachedChecker, final long millisInThePast) throws Exception {
        Field field = CachedChecker.class.getDeclaredField("timeStamp");
        field.setAccessible(true);
        field.set(cachedChecker, System.currentTimeMillis() - millisInThePast);
    }
}
