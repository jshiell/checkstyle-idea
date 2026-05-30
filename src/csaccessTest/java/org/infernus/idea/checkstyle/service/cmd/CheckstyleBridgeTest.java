package org.infernus.idea.checkstyle.service.cmd;

import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckstyleBridgeTest {

    @Test
    void messagesFromReturnsMessagesMapFromConfiguration() throws Exception {
        Configuration config = mock(Configuration.class);
        Map<String, String> expectedMessages = Map.of("key1", "value1", "key2", "value2");
        when(config.getMessages()).thenReturn(expectedMessages);

        Map<String, String> result = CheckstyleBridge.messagesFrom(config);

        assertThat(result, is(expectedMessages));
    }

    @Test
    void messagesFromReturnsEmptyMapWhenConfigurationHasNoMessages() throws Exception {
        Configuration config = mock(Configuration.class);
        when(config.getMessages()).thenReturn(Map.of());

        Map<String, String> result = CheckstyleBridge.messagesFrom(config);

        assertThat(result.size(), is(0));
    }

    @Test
    void messagesFromThrowsRuntimeExceptionWhenConfigurationThrows() throws Exception {
        Configuration config = mock(Configuration.class);
        when(config.getMessages()).thenThrow(new RuntimeException("unexpected error"));

        assertThrows(RuntimeException.class, () -> CheckstyleBridge.messagesFrom(config));
    }
}
