package org.infernus.idea.checkstyle.service;

import java.util.Collections;
import java.util.Optional;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;


public class CheckStyleAuditListenerTest
{
    private int counter = 0;


    @Test
    public void testSeverityLevels() {
        final CheckStyleAuditListener underTest = new CheckStyleAuditListener(Collections.emptyMap(), false, 2,
                Optional.empty(), Collections.emptyList());
        underTest.addError(createDummyEvent(SeverityLevel.INFO));
        underTest.addError(createDummyEvent(SeverityLevel.WARNING));
        underTest.addError(createDummyEvent(SeverityLevel.ERROR));
        underTest.addError(createDummyEvent(SeverityLevel.IGNORE));
        underTest.addError(createDummyEvent(null));
    }


    @Test
    public void testAddException() {
        final CheckStyleAuditListener underTest = new CheckStyleAuditListener(Collections.emptyMap(), false, 2,
                Optional.empty(), Collections.emptyList());
        underTest.addException(createDummyEvent(SeverityLevel.ERROR), //
                new IllegalArgumentException("Exception for unit testing only - not a real exception"));
    }


    @Test
    public void testWithoutLocalizedMessage() {
        final CheckStyleAuditListener underTest = new CheckStyleAuditListener(Collections.emptyMap(), false, 2,
                Optional.empty(), Collections.emptyList());
        try {
            underTest.addError(new AuditEvent("source", "filename.java"));  // quite unlikely to happen in real life
            Assert.fail("expected exception was not thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }


    private AuditEvent createDummyEvent(@Nullable final SeverityLevel pSeverityLevel) {
        LocalizedMessage localizedMessage = new LocalizedMessage(42 + (counter++), 21, "bundle", "message text",
                null, pSeverityLevel, "moduleId", CheckStyleAuditListenerTest.class, null);
        return new AuditEvent("source", "filename.java", localizedMessage);
    }
}
