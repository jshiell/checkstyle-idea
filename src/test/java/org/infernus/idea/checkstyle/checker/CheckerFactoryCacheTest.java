package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CheckerFactoryCacheTest {

    @Mock
    private CheckStyleChecker checkStyleChecker;

    @Mock
    private Project project;

    private CheckerFactoryCache underTest;
    private ConfigurationLocation location;

    @BeforeEach
    void setUp() {
        underTest = new CheckerFactoryCache();
        location = new StringConfigurationLocation("<module/>", project);
    }

    @Test
    void getReturnsEmptyWhenNothingHasBeenPut() {
        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    @Test
    void getReturnsTheCachedCheckerAfterPut() {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);

        underTest.put(location, null, cachedChecker);

        assertThat(underTest.get(location, null), is(Optional.of(cachedChecker)));
    }

    @Test
    void getReturnsTheSameInstanceThatWasPut() {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);

        underTest.put(location, null, cachedChecker);

        assertThat(underTest.get(location, null).orElseThrow(), is(sameInstance(cachedChecker)));
    }

    @Test
    void getReturnsEmptyForAnExpiredEntry() throws Exception {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);
        backdateTimestamp(cachedChecker, 61_000);

        underTest.put(location, null, cachedChecker);

        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    @Test
    void getRemovesExpiredEntryFromCacheSoSubsequentGetAlsoReturnsEmpty() throws Exception {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);
        backdateTimestamp(cachedChecker, 61_000);
        underTest.put(location, null, cachedChecker);

        underTest.get(location, null); // first get triggers cleanup

        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    @Test
    void invalidateClearsAllEntries() {
        underTest.put(location, null, new CachedChecker(checkStyleChecker));

        underTest.invalidate();

        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    @Test
    void invalidateDestroysAllCachedCheckers() {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);
        underTest.put(location, null, cachedChecker);

        underTest.invalidate();

        verify(checkStyleChecker).destroy();
    }

    @Test
    void disposeCallsInvalidate() {
        CachedChecker cachedChecker = new CachedChecker(checkStyleChecker);
        underTest.put(location, null, cachedChecker);

        underTest.dispose();

        assertThat(underTest.get(location, null), is(Optional.empty()));
    }

    private void backdateTimestamp(CachedChecker cachedChecker, long millisInThePast) throws Exception {
        Field field = CachedChecker.class.getDeclaredField("timeStamp");
        field.setAccessible(true);
        field.set(cachedChecker, System.currentTimeMillis() - millisInThePast);
    }
}
