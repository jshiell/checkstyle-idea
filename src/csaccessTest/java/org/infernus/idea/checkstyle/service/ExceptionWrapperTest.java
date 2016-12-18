package org.infernus.idea.checkstyle.service;

import antlr.MismatchedTokenException;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.junit.Test;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class ExceptionWrapperTest
{
    @Test
    public void anExceptionThatIsNotACheckstyleExceptionIsWrappedInAPluginException() {
        assertThat( //
                new ExceptionWrapper().wrap(null, new NullPointerException("aTestException")), //
                is(instanceOf(CheckStylePluginException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfARecognitionExceptionIsWrappedInAPluginParseException() {
        assertThat( //
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", new RecognitionException
                        ("aTestException"))), //
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfASubclassOfRecognitionExceptionIsWrappedInAPluginParseException() {
        assertThat( //
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", new
                        MismatchedTokenException())), //
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfATokenStreamExceptionIsWrappedInAPluginParseException() {
        assertThat( //
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", new TokenStreamException
                        ("aTestException"))), //
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfANullPointerExceptionIsWrappedInAPluginParseException() {
        assertThat( //
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", new NullPointerException
                        ("aTestException"))), //
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnArrayIndexOutOfBoundsExceptionIsWrappedInAPluginParseException() {
        assertThat( //
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", new
                        ArrayIndexOutOfBoundsException("aTestException"))), //
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnIllegalStateExceptionIsWrappedInAPluginParseException() {
        assertThat( //
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", new IllegalStateException
                        ("aTestException"))), //
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnyOtherExceptionIsWrappedInAPluginException() {
        assertThat( //
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", new IllegalStateException
                        ("aTestException"))), //
                is(instanceOf(CheckStylePluginException.class)));
    }
}
