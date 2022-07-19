package com.infilos.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipHelper {
    private ZipHelper() {
    }

    public static List<String> listFileNames(ZipFile zipFile, String dir) {
        String theDir = dir;
        if (Strings.isNotBlank(theDir)) {
            theDir = Strings.addSuffixIfMissing(theDir, "/");
        }

        String name;
        final List<String> fileNames = new ArrayList<>();

        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            name = entry.getName();
            if (Strings.isEmpty(theDir) || name.startsWith(theDir)) {
                final String nameSuffix = Strings.removePrefix(name, dir);
                if (Strings.isNotEmpty(nameSuffix) && !nameSuffix.contains("/")) {
                    fileNames.add(nameSuffix);
                }
            }
        }

        return fileNames;
    }
}
