package com.infilos.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static com.infilos.utils.DataSize.DataUnit;

public class DataSizeTest {

    private void assertDataSize(long size, String expect) {
        assertEquals(DataSize.ofSuccinct(size).toString(), expect);
    }

    @Test
    public void testDataSize() {
        assertDataSize(123, "123B");
        assertDataSize(Double.valueOf(5.5 * 1024).longValue(), "5.50kB");
        assertDataSize(3L * 1024 * 1024, "3MB");
        assertDataSize(3L * 1024 * 1024 * 1024, "3GB");
        assertDataSize(3L * 1024 * 1024 * 1024 * 1024, "3TB");
        assertDataSize(3L * 1024 * 1024 * 1024 * 1024 * 1024, "3PB");
    }

    @Test
    public void testParseDataSize() {
        DataSize size1 = DataSize.of(1234);
        DataSize size2 = DataSize.parse("1234");
        DataSize size3 = DataSize.parse("1234B");
        DataSize size4 = DataSize.parse("1234kB");
        DataSize size5 = DataSize.parse("1234MB");
        DataSize size6 = DataSize.parse("1234GB");
        DataSize size7 = DataSize.parse("1234TB");
        DataSize size8 = DataSize.parse("1234PB");
        assertTrue(size1.value()==1234 && size1.unit()==DataUnit.BYTE);
        assertTrue(size2.value()==1234 && size2.unit()==DataUnit.BYTE);
        assertTrue(size3.value()==1234 && size3.unit()==DataUnit.BYTE);
        assertTrue(size4.value()==1234 && size4.unit()==DataUnit.KILOBYTE);
        assertTrue(size5.value()==1234 && size5.unit()==DataUnit.MEGABYTE);
        assertTrue(size6.value()==1234 && size6.unit()==DataUnit.GIGABYTE);
        assertTrue(size7.value()==1234 && size7.unit()==DataUnit.TERABYTE);
        assertTrue(size8.value()==1234 && size8.unit()==DataUnit.PETABYTE);

        DataSize size9 = DataSize.parse("1234.56B");
        DataSize size10 = DataSize.parse("1234.56kB");
        DataSize size11 = DataSize.parse("1234.56MB");
        DataSize size12 = DataSize.parse("1234.56GB");
        DataSize size13 = DataSize.parse("1234.56TB");
        DataSize size14 = DataSize.parse("1234.56PB");
        assertTrue(size9.value()==1234.56 && size9.unit()==DataUnit.BYTE);
        assertTrue(size10.value()==1234.56 && size10.unit()==DataUnit.KILOBYTE);
        assertTrue(size11.value()==1234.56 && size11.unit()==DataUnit.MEGABYTE);
        assertTrue(size12.value()==1234.56 && size12.unit()==DataUnit.GIGABYTE);
        assertTrue(size13.value()==1234.56 && size13.unit()==DataUnit.TERABYTE);
        assertTrue(size14.value()==1234.56 && size14.unit()==DataUnit.PETABYTE);
    }

    @Test
    public void testConvert() {
        DataSize dataSize = DataSize.parse("10GB");
        assertEquals(dataSize.convertTo(DataUnit.BYTE).toString(), "10737418240B");
        assertEquals(dataSize.convertTo(DataUnit.KILOBYTE).toString(), "10485760kB");
        assertEquals(dataSize.convertTo(DataUnit.MEGABYTE).toString(), "10240MB");
        assertEquals(dataSize.convertTo(DataUnit.GIGABYTE).toString(), "10GB");
        assertEquals(dataSize.convertTo(DataUnit.TERABYTE).toString(), "0.01TB");
        assertEquals(dataSize.convertTo(DataUnit.PETABYTE).toString(), "0.00PB");
    }

    @Test
    public void testRound() {
        assertTrue(DataSize.of(10000).roundTo(DataUnit.KILOBYTE)==10);
        assertTrue(DataSize.of(10000000).roundTo(DataUnit.MEGABYTE)==10);
    }

    @Test
    public void testCompare() {
        List<DataSize> sizes = Arrays.asList(
            DataSize.parse("1GB"),
            DataSize.parse("8kB"),
            DataSize.parse("2048MB"),
            DataSize.parse("10B"),
            DataSize.parse("20MB"),
            DataSize.parse("10PB"),
            DataSize.parse("20PB")
        );
        Collections.sort(sizes);

        List<DataSize> expect = Arrays.asList(
            DataSize.parse("10B"),
            DataSize.parse("8kB"),
            DataSize.parse("20MB"),
            DataSize.parse("1GB"),
            DataSize.parse("2048MB"),
            DataSize.parse("10PB"),
            DataSize.parse("20PB")
        );

        for (int idx = 0; idx < expect.size(); idx++) {
            assertEquals(sizes.get(idx).toString(), expect.get(idx).toString());
        }
    }

    @Test
    public void testToBytes() {
        assertEquals(DataSize.parse("1").toBytes(), 1);
        assertEquals(DataSize.parse("1B").toBytes(), 1);
        assertEquals(DataSize.parse("1kB").toBytes(), 1L * 1024);
        assertEquals(DataSize.parse("1MB").toBytes(), 1L * 1024 * 1024);
        assertEquals(DataSize.parse("1GB").toBytes(), 1L * 1024 * 1024 * 1024);
        assertEquals(DataSize.parse("1TB").toBytes(), 1L * 1024 * 1024 * 1024 * 1024);
        assertEquals(DataSize.parse("1PB").toBytes(), 1L * 1024 * 1024 * 1024 * 1024 * 1024);
    }
}