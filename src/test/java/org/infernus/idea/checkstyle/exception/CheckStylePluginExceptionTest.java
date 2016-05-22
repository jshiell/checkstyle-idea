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

    @Test
    public void anExceptionThatIsNotACheckstyleExceptionIsWrappedInAPluginException() {
        assertThat(
                wrap(new NullPointerException("aTestException")),
                is(instanceOf(CheckStylePluginException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfARecognitionExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException("aTestException", new RecognitionException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfASubclassOfRecognitionExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException("aTestException", new MismatchedTokenException())),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfATokenStreamExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException("aTestException", new TokenStreamException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfANullPointerExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException("aTestException", new NullPointerException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnArrayIndexOutOfBoundsExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException("aTestException", new ArrayIndexOutOfBoundsException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnIllegalStateExceptionIsWrappedInAPluginParseException() {
        assertThat(
                wrap(new CheckstyleException("aTestException", new IllegalStateException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnyOtherExceptionIsWrappedInAPluginException() {
        assertThat(
                wrap(new CheckstyleException("aTestException", new IllegalStateException("aTestException"))),
                is(instanceOf(CheckStylePluginException.class)));
    }

}
