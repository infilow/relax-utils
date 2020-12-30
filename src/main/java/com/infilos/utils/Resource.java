package com.infilos.utils;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhiguang.zhang on 2020-12-09.
 *
 * Read files locate at 'src/main/resources'.
 */

public final class Resource {
    private Resource() {
    }

    public static File readAsFile(String pathOfResource) throws URISyntaxException {
        return new File(Resource.class.getResource(fixPathOfResource(pathOfResource)).toURI());
    }

    public static InputStream readAsStream(String pathOfResource) {
        return Resource.class.getResourceAsStream(fixPathOfResource(pathOfResource));
    }

    public static List<String> readAsLines(String pathOfResource) throws IOException {
        InputStream stream = readAsStream(pathOfResource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<String> lines = new ArrayList<>();

        while (reader.ready()) {
            lines.add(reader.readLine());
        }

        return lines;
    }

    public static String readAsString(String pathOfResource) throws IOException {
        return String.join("\n", readAsLines(pathOfResource));
    }
    
    private static String fixPathOfResource(String pathOfResource) {
        return pathOfResource.startsWith("/") ? pathOfResource:"/" + pathOfResource;
    }
}
