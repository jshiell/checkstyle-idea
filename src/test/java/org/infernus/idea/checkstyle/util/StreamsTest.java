package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class StreamsTest {

    @Test
    void inMemoryCopyPreservesContent() throws IOException {
        byte[] original = "hello world".getBytes();
        InputStream copy = Streams.inMemoryCopyOf(new ByteArrayInputStream(original));
        assertThat(copy.readAllBytes(), is(original));
    }

    @Test
    void inMemoryCopyOfEmptyStreamIsEmpty() throws IOException {
        InputStream copy = Streams.inMemoryCopyOf(new ByteArrayInputStream(new byte[0]));
        assertThat(copy.readAllBytes().length, is(0));
    }

    @Test
    void readContentOfPreservesBytes() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        assertThat(Streams.readContentOf(new ByteArrayInputStream(data)), is(data));
    }

    @Test
    void readContentOfLargeStreamExceedingBufferSize() throws IOException {
        byte[] large = new byte[10_000];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i % 127);
        }
        assertThat(Streams.readContentOf(new ByteArrayInputStream(large)), is(large));
    }
}
