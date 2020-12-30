package com.infilos.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public class EncoderTest {

    @Test
    public void test() {
        String b62 = Encoder.decimalToNumeric(1561947570614L, 62);
        System.out.println(b62);
        System.out.println(b62.length());

        long str = Encoder.numericToDecimal(b62, 62);
        assertEquals(1561947570614L, str);

        assertNotNull(Encoder.md5("1561947570614"));
        assertNotNull(Encoder.asBase64("1561947570614"));
        assertNotNull(Encoder.ofBase64("MTU2MTk0NzU3MDYxNA=="));
    }
}