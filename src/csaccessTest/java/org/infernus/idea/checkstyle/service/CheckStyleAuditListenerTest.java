package org.infernus.idea.checkstyle.service;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class CheckStyleAuditListenerTest {
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
        underTest.addException(createDummyEvent(SeverityLevel.ERROR),
                new IllegalArgumentException("Exception for unit testing only - not a real exception"));
    }

    @Test
    public void testWithoutLocalizedMessage() {
        final CheckStyleAuditListener underTest = new CheckStyleAuditListener(Collections.emptyMap(), false, 2,
                Optional.empty(), Collections.emptyList());
        assertThrows(NullPointerException.class,
                () -> underTest.addError(new AuditEvent("source", "filename.java")));
    }

    private AuditEvent createDummyEvent(@Nullable final SeverityLevel severityLevel) {
        try {
            return (AuditEvent) auditEvent().newInstance("source", "filename.java", createMessage(severityLevel));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dummy event", e);
        }
    }

    @NotNull
    private Object createMessage(@Nullable final SeverityLevel severityLevel) throws ReflectiveOperationException {
        Constructor<?> messageConstructor = messageClass().getDeclaredConstructor(int.class, int.class, String.class,
                String.class, Object[].class, SeverityLevel.class, String.class, Class.class, String.class);
        return messageConstructor.newInstance(42 + (counter++), 21, "bundle", "message text",
                null, severityLevel, "moduleId", CheckStyleAuditListenerTest.class, null);
    }

    private Class<?> messageClass() {
        for (String possibleClassName : asList("Violation", "LocalizedMessage")) {
            try {
                return Class.forName("com.puppycrawl.tools.checkstyle.api." + possibleClassName);
            } catch (ClassNotFoundException ignored) {
            }
        }

        throw new RuntimeException("Unable to find a message class for the version of Checkstyle on the classpath");
    }

    private Constructor<?> auditEvent() throws ClassNotFoundException, NoSuchMethodException {
        return Class.forName("com.puppycrawl.tools.checkstyle.api.AuditEvent")
                .getDeclaredConstructor(Object.class, String.class, messageClass());
    }
}
