package com.infilos.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FunctionsTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void function3_andThen() {
        final Function3<String, String, String, Integer> f = (a, b, c) -> 1;
        assertThat(f.andThen(i -> i + 1).apply("", "", ""), is(2));
    }

    @Test
    public void function3_andThenNull() {
        final Function3<String, String, String, Integer> f = (a, b, c) -> 1;
        exception.expect(NullPointerException.class);
        f.andThen(null);
    }

    @Test
    public void function4_andThen() {
        final Function4<String, String, String, String, Integer> f = (a, b, c, d) -> 1;
        assertThat(f.andThen(i -> i + 1).apply("", "", "", ""), is(2));
    }

    @Test
    public void function4_andThenNull() {
        final Function4<String, String, String, String, Integer> f = (a, b, c, d) -> 1;
        exception.expect(NullPointerException.class);
        f.andThen(null);
    }

    @Test
    public void function5_andThen() {
        final Function5<String, String, String, String, String, Integer> f = (a, b, c, d, e) -> 1;
        assertThat(f.andThen(i -> i + 1).apply("", "", "", "", ""), is(2));
    }

    @Test
    public void function5_andThenNull() {
        final Function5<String, String, String, String, String, Integer> f = (a, b, c, d, e) -> 1;
        exception.expect(NullPointerException.class);
        f.andThen(null);
    }

    @Test
    public void function6_andThen() {
        final Function6<String, String, String, String, String, String, Integer> ff = (a, b, c, d, e, f) -> 1;
        assertThat(ff.andThen(i -> i + 1).apply("", "", "", "", "", ""), is(2));
    }

    @Test
    public void function6_andThenNull() {
        final Function6<String, String, String, String, String, String, Integer> ff = (a, b, c, d, e, f) -> 1;
        exception.expect(NullPointerException.class);
        ff.andThen(null);
    }
}