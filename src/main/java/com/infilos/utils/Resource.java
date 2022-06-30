package com.infilos.utils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * @author zhiguang.zhang on 2020-12-09.
 *
 * Read files locate at 'src/main/resources'.
 */

public final class Resource {
    private Resource() {
    }

    public static URL readAsUrl(String pathOfResource) {
        return Resource.class.getResource(fixPathOfResource(pathOfResource));
    }

    public static File readAsFile(String pathOfResource) throws URISyntaxException {
        return new File(Resource.class.getResource(fixPathOfResource(pathOfResource)).toURI());
    }

    public static InputStream readAsStream(String pathOfResource) {
        return Resource.class.getResourceAsStream(fixPathOfResource(pathOfResource));
    }

    public static List<String> readAsLines(String pathOfResource, int limit) throws IOException {
        InputStream stream = readAsStream(pathOfResource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<String> lines = new ArrayList<>();

        while (reader.ready()) {
            lines.add(reader.readLine());
            
            if(limit >= 0 && lines.size() >= limit) {
                break;
            }
        }

        return lines;
    }

    public static List<String> readAsLines(String pathOfResource) throws IOException {
        return readAsLines(pathOfResource, -1);
    }

    public static String readAsString(String pathOfResource) throws IOException {
        return String.join("\n", readAsLines(pathOfResource));
    }

    public static byte[] readAsBytes(String pathOfResource) throws URISyntaxException, IOException {
        File file = Resource.readAsFile(pathOfResource);
        byte[] bytes = new byte[(int) file.length()];

        try (DataInputStream stream = new DataInputStream(Files.newInputStream(file.toPath()))) {
            stream.readFully(bytes);
        }

        return bytes;
    }

    private static String fixPathOfResource(String pathOfResource) {
        return pathOfResource.startsWith("/") ? pathOfResource : "/" + pathOfResource;
    }

    /**
     * File would appear in "/target/classes" or "/target/test-classes".
     */
    public static File writeResource(String filename, byte[] bytes) throws IOException {
        URL resourceUrl = Resource.class.getResource("/");
        if(Objects.isNull(resourceUrl)) {
            throw new IllegalArgumentException("Cannot find resource folder with '/'.");
        }
        
        Path resource = Paths.get(resourceUrl.getPath());
        File file = new File(resource.toAbsolutePath() + fixPathOfResource(filename));

        if (!file.exists()) {
            file.createNewFile();
        }
        Files.write(file.toPath(), bytes);

        return file;
    }

    public static File writeResource(String filename, String string) throws IOException {
        return writeResource(filename, string.getBytes());
    }
}
