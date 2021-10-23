package com.infilos.utils.stream;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
public class CommonStreamTest {

    @Test
    public void splitChunk() {
        assertThat(CommonStream.splitChunk(list(), 3))
            .isEqualTo(Collections.emptyList());

        assertThat(CommonStream.splitChunk(list(1), 3))
            .isEqualTo(list(list(1)));

        assertThat(CommonStream.splitChunk(list(1, 2), 3))
            .isEqualTo(list(list(1, 2)));

        assertThat(CommonStream.splitChunk(list(1, 2, 3), 3))
            .isEqualTo(list(list(1, 2, 3)));

        assertThat(CommonStream.splitChunk(list(1, 2, 3, 4), 3))
            .isEqualTo(list(list(1, 2, 3), list(4)));

        assertThat(CommonStream.splitChunk(list(1, 2, 3, 4, 5), 3))
            .isEqualTo(list(list(1, 2, 3), list(4, 5)));

        assertThat(CommonStream.splitChunk(list(1, 2, 3, 4, 5, 6), 3))
            .isEqualTo(list(list(1, 2, 3), list(4, 5, 6)));

        assertThat(CommonStream.splitChunk(list(1, 2, 3, 4, 5, 6, 7), 3))
            .isEqualTo(list(list(1, 2, 3), list(4, 5, 6), list(7)));

        assertThat(CommonStream.splitChunk(list(1, 2, 3, 4, 5, 6, 7), 3, true))
            .isEqualTo(list(list(1, 2, 3), list(4, 5, 6, 7)));

        assertThat(CommonStream.splitChunk(list(1, 2, 3, 4, 5, 6, 7, 8), 3, true))
            .isEqualTo(list(list(1, 2, 3), list(4, 5, 6), list(7, 8)));
    }

    private static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }
}