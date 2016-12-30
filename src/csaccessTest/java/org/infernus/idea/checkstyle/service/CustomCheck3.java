package org.infernus.idea.checkstyle.service;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;


/**
 * A custom check that uses a part of the API that was broken for a time. We check that the error occurs with certain
 * versions of Checkstyle, and not with others. This is a helper class for one particular unit test, and it is only
 * one example out of many of how the API gets broken.
 * <p>The method {@link FileContents#getFileName()} was called {@code getFilename()} (lowercase 'n') until Checkstyle
 * 6.5. Then it was called {@code getFileName()} (uppercase 'N') in Checkstyle 6.6 and 6.7. Starting with Checkstyle
 * 6.8, both versions are present for backwards compatibility (Checkstyle issue <a target="_blank"
 * href="https://github.com/checkstyle/checkstyle/issues/1205">#1205</a>). The lowercase variant is deprecated, so it
 * may disappear in the future. In Checkstyle 7.1.1, it is still present.</p>
 * <p>This check tries to call the lowercase {@code getFilename()}, which is expected to fail with Checkstyle 6.6 and
 * 6.7, and work with all other versions at least up to 7.1.1.</p>
 *
 * @see ServiceLayerBasicTest#testConfig3()
 */
@SuppressWarnings("deprecation") // We can't extend AbstractCheck because then it would not work with earlier runtimes.
public class CustomCheck3
        extends com.puppycrawl.tools.checkstyle.api.Check   // do not "import" so that the warning can be suppressed
{
    private static Throwable errorOccurred = null;


    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.OBJBLOCK};
    }


    @Override
    @SuppressWarnings("deprecation") // FileContents.getFilename() is deprecated in CS 6.8+
    public void beginTree(final DetailAST pRootAst) {
        super.beginTree(pRootAst);
        try {
            final String fileName = getFileContents().getFilename();
            // NoSuchMethodError will be thrown from the line above, but subsequently caught and swallowed by
            // TreeWalker.processFiltered() (at least in versions 6.6 and 6.7; this was later fixed).
            Assert.assertNotNull(fileName);
        } catch (Throwable t) {
            errorOccurred = t;
        }
    }


    @Nullable
    static Throwable popErrorOccurred4UnitTest() {
        Throwable result = errorOccurred;
        errorOccurred = null;
        return result;
    }
}
