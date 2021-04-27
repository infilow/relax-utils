package com.infilos.utils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public class ResourceTest {

    @Test
    public void test() throws URISyntaxException, IOException {
        assertNotNull(Resource.readAsFile("file.txt"));
        assertTrue(Resource.readAsFile("file.txt").isFile());
        assertNotNull(Resource.readAsStream("file.txt"));
        assertEquals(3, Resource.readAsLines("file.txt").size());
        assertEquals("a\nb\nc", Resource.readAsString("file.txt"));
    }
    
    @Test
    public void write() throws IOException {
        Resource.writeResource("alphabet.txt", "abcdefg".getBytes());
        assertEquals("abcdefg", Resource.readAsString("alphabet.txt"));
    }
}