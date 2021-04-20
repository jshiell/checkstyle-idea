package org.infernus.idea.checkstyle.service;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Optional;


public class CheckStyleAuditListenerTest {
    private int counter = 0;

    @Test
    public void testSeverityLevels() throws Exception {
        final CheckStyleAuditListener underTest = new CheckStyleAuditListener(Collections.emptyMap(), false, 2,
                Optional.empty(), Collections.emptyList());
        underTest.addError(createDummyEvent(SeverityLevel.INFO));
        underTest.addError(createDummyEvent(SeverityLevel.WARNING));
        underTest.addError(createDummyEvent(SeverityLevel.ERROR));
        underTest.addError(createDummyEvent(SeverityLevel.IGNORE));
        underTest.addError(createDummyEvent(null));
    }

    @Test
    public void testAddException() throws Exception {
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

    private AuditEvent createDummyEvent(@Nullable final SeverityLevel pSeverityLevel) throws Exception {
        Constructor<?> auditEvent = getAuditEvent();
        Class<?> messageClass = getMessageClass();
        Class<?>[] Type = {int.class, int.class, String.class, String.class, Object[].class,
                SeverityLevel.class, String.class, Class.class, String.class};
        Constructor<?> cons = messageClass.getDeclaredConstructor(Type);

        Object message = cons.newInstance(42 + (counter++), 21, "bundle", "message text",
                null, pSeverityLevel, "moduleId", CheckStyleAuditListenerTest.class, null);
        return (AuditEvent) auditEvent.newInstance("source", "filename.java", message);
    }

    private Class<?> getMessageClass() throws NullPointerException, ClassNotFoundException {
        Class messageClass = null;
        try {
            messageClass = Class.forName("com.puppycrawl.tools.checkstyle.api.Violation");
        } catch (ClassNotFoundException e) {
            System.out.println("Violation class not found");
        }

        if (messageClass == null) {
            try {
                messageClass = Class.forName("com.puppycrawl.tools.checkstyle.api.LocalizedMessage");
            } catch (ClassNotFoundException e) {
                System.out.println("LocalizedClass class Not found");
                e.printStackTrace();
                throw e;
            }
        }
        return messageClass;
    }

    private Constructor<?> getAuditEvent() throws Exception {
        final Class<?> auditEventClass;
        final Constructor<?> auditEvent;
        try {
            auditEventClass = Class.forName("com.puppycrawl.tools.checkstyle.api.AuditEvent");
        } catch (ClassNotFoundException e) {
            System.out.println("AuditEvent class Not found");
            e.printStackTrace();
            throw e;
        }

        Class<?>[] type = { Object.class, String.class, getMessageClass()};
        auditEvent = auditEventClass.getDeclaredConstructor(type);
        return auditEvent;
    }
}
