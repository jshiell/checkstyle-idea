package org.infernus.idea.checkstyle.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CachedCheckerTest {

    @Mock
    private CheckStyleChecker checkStyleChecker;

    @Test
    void aFreshCachedCheckerIsValid() {
        CachedChecker underTest = new CachedChecker(checkStyleChecker);

        assertThat(underTest.isValid(), is(true));
    }

    @Test
    void anExpiredCachedCheckerIsNotValid() throws Exception {
        CachedChecker underTest = new CachedChecker(checkStyleChecker);

        backdateTimestamp(underTest, 61_000);

        assertThat(underTest.isValid(), is(false));
    }

    @Test
    void getCheckStyleCheckerReturnsTheWrappedChecker() {
        CachedChecker underTest = new CachedChecker(checkStyleChecker);

        assertThat(underTest.getCheckStyleChecker(), is(sameInstance(checkStyleChecker)));
    }

    @Test
    void getCheckStyleCheckerRefreshesTheTimestamp() throws Exception {
        CachedChecker underTest = new CachedChecker(checkStyleChecker);

        backdateTimestamp(underTest, 61_000);
        underTest.getCheckStyleChecker();

        assertThat(underTest.isValid(), is(true));
    }

    @Test
    void destroyDelegatesToTheWrappedChecker() {
        CachedChecker underTest = new CachedChecker(checkStyleChecker);

        underTest.destroy();

        verify(checkStyleChecker).destroy();
    }

    private void backdateTimestamp(final CachedChecker cachedChecker, final long millisInThePast) throws Exception {
        Field field = CachedChecker.class.getDeclaredField("timeStamp");
        field.setAccessible(true);
        field.set(cachedChecker, System.currentTimeMillis() - millisInThePast);
    }
}
