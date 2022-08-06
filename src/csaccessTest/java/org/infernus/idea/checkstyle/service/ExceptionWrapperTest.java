package org.infernus.idea.checkstyle.service;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;


public class ExceptionWrapperTest {
    @Test
    public void anExceptionThatIsNotACheckstyleExceptionIsWrappedInAPluginException() {
        assertThat(
                new ExceptionWrapper().wrap(null, new NullPointerException("aTestException")),
                is(instanceOf(CheckStylePluginException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnAntlrV2RecognitionExceptionIsWrappedInAPluginParseException() {
        assumeTrue(classExists("antlr.RecognitionException"));

        assertThat(
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", instantiateExceptionByNameWithMessage("antlr.RecognitionException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfASubclassOfRecognitionExceptionIsWrappedInAPluginParseException() {
        assumeTrue(classExists("antlr.MismatchedTokenException"));

        assertThat(
                new ExceptionWrapper().wrap(null, new CheckstyleException("aTestException", instantiateExceptionByName("antlr.MismatchedTokenException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfATokenStreamExceptionIsWrappedInAPluginParseException() {
        assumeTrue(classExists("antlr.TokenStreamException"));

        assertThat(
                new ExceptionWrapper()
                        .wrap(null, new CheckstyleException("aTestException", instantiateExceptionByNameWithMessage("antlr.TokenStreamException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfANullPointerExceptionIsWrappedInAPluginParseException() {
        assertThat(
                new ExceptionWrapper()
                        .wrap(null, new CheckstyleException("aTestException", new NullPointerException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnArrayIndexOutOfBoundsExceptionIsWrappedInAPluginParseException() {
        assertThat(
                new ExceptionWrapper()
                        .wrap(null, new CheckstyleException("aTestException", new ArrayIndexOutOfBoundsException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnIllegalStateExceptionIsWrappedInAPluginParseException() {
        assertThat(
                new ExceptionWrapper()
                        .wrap(null, new CheckstyleException("aTestException", new IllegalStateException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAStringIndexOutOfBoundsExceptionIsWrappedInAPluginParseException() {
        assertThat(
                new ExceptionWrapper()
                        .wrap(null, new CheckstyleException("aTestException", new StringIndexOutOfBoundsException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAClassCastExceptionIsWrappedInAPluginParseException() {
        assertThat(
                new ExceptionWrapper()
                        .wrap(null, new CheckstyleException("aTestException", new ClassCastException("aTestException"))),
                is(instanceOf(CheckStylePluginParseException.class)));
    }

    @Test
    public void aCheckstyleExceptionThatHasACauseOfAnyOtherExceptionIsWrappedInAPluginException() {
        assertThat(
                new ExceptionWrapper()
                        .wrap(null, new CheckstyleException("aTestException", new IllegalStateException("aTestException"))),
                is(instanceOf(CheckStylePluginException.class)));
    }

    @NotNull
    private Throwable instantiateExceptionByName(String s) {
        try {
            return (Throwable) Class.forName(s).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Test exception instantiation failed for " + s, e);
        }
    }

    @NotNull
    private Throwable instantiateExceptionByNameWithMessage(String s) {
        try {
            return (Throwable) Class.forName(s).getDeclaredConstructor(String.class).newInstance("aTestException");
        } catch (Exception e) {
            throw new RuntimeException("Test exception instantiation failed for " + s, e);
        }
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
