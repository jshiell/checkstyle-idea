package org.infernus.idea.checkstyle.exception;

import antlr.MismatchedTokenException;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.infernus.idea.checkstyle.exception.CheckStylePluginException.wrap;
import static org.junit.Assert.assertThat;

public class CheckStylePluginExceptionTest {

    private static final String A_TEST_EXCEPTION = "aTestException";

    @Test
    public void anExceptionThatIsNotACheckstyleExceptionIsWrappedInAPluginException() {
        assertThat(
                wrap(new NullPointerException(A_TEST_EXCEPTION)),
                is(instanceOf(CheckStylePluginException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfARecognitionExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException(A_TEST_EXCEPTION, new RecognitionException(A_TEST_EXCEPTION))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfASubclassOfRecognitionExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException(A_TEST_EXCEPTION, new MismatchedTokenException())),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfATokenStreamExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException(A_TEST_EXCEPTION, new TokenStreamException(A_TEST_EXCEPTION))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfANullPointerExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException(A_TEST_EXCEPTION, new NullPointerException(A_TEST_EXCEPTION))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnArrayIndexOutOfBoundsExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException(A_TEST_EXCEPTION, new ArrayIndexOutOfBoundsException(A_TEST_EXCEPTION))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnIllegalStateExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException(A_TEST_EXCEPTION, new IllegalStateException(A_TEST_EXCEPTION))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnyOtherExceptionIsWrappedInAPluginException() {
        assertThat(
                wrap(new CheckstyleException(A_TEST_EXCEPTION, new IllegalStateException(A_TEST_EXCEPTION))),
                is(instanceOf(CheckStylePluginException.class)));
    }

}
