package com.infilos.utils;

import com.infilos.utils.io.FileReader;
import com.infilos.utils.io.FileWriter;
import com.infilos.utils.io.*;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.jar.JarFile;

@SuppressWarnings("unused")
public final class FileHelper {
    private FileHelper() {
    }

    /**
     * Clean temporary files after 2 minutes.
     */
    private static final ExpiringMap<String, File> TEMPORARY_CLEANUP_REGISTRY = ExpiringMap.builder()
            .expiration(2, TimeUnit.MINUTES)
            .expirationListener((ExpirationListener<String, File>) (name, file) -> cleanTemporary(file))
            .build();

    public static final String CLASS_EXT = ".java";
    public static final String JAR_FILE_EXT = ".jar";
    public static final String JAR_PATH_EXT = ".jar!";
    public static final String PATH_FILE_PRE = "file:";
    // Unix is '/', Windows is '\'
    public static final String FILE_SEPARATOR = File.separator;
    // Unix is ':', Windows is ';'
    public static final String PATH_SEPARATOR = File.pathSeparator;

    public static boolean isWindows() {
        return '\\' == File.separatorChar;
    }

    public static File[] ls(String path) {
        if (path == null) {
            return null;
        }

        File file = of(path);
        if (file.isDirectory()) {
            return file.listFiles();
        }
        throw Throws.runtime("Path [%s] is not directory!", path);
    }

    public static boolean isEmpty(File file) {
        if ((null == file) || !file.exists()) {
            return true;
        }

        if (file.isDirectory()) {
            String[] subFiles = file.list();
            return Arrays.isEmpty(subFiles);
        } else if (file.isFile()) {
            return file.length() <= 0;
        }

        return false;
    }

    public static boolean isNotEmpty(File file) {
        return !isEmpty(file);
    }

    public static boolean isDirEmpty(File dir) {
        return PathHelper.isDirEmpty(dir.toPath());
    }

    public static List<File> loopFiles(String path, FileFilter fileFilter) {
        return loopFiles(of(path), fileFilter);
    }

    public static List<File> loopFiles(File file, FileFilter fileFilter) {
        return loopFiles(file, -1, fileFilter);
    }

    public static void walkFiles(File file, Consumer<File> consumer) {
        if (file.isDirectory()) {
            final File[] subFiles = file.listFiles();
            if (Arrays.isNotEmpty(subFiles)) {
                for (File tmp : subFiles) {
                    walkFiles(tmp, consumer);
                }
            }
        } else {
            consumer.accept(file);
        }
    }

    public static List<File> loopFiles(File file, int maxDepth, FileFilter fileFilter) {
        return PathHelper.loopFiles(file.toPath(), maxDepth, fileFilter);
    }

    public static List<File> loopFiles(String path) {
        return loopFiles(of(path));
    }

    public static List<File> loopFiles(File file) {
        return loopFiles(file, null);
    }

    public static List<String> listFileNames(String path) throws IOException {
        if (path == null) {
            return new ArrayList<>(0);
        }
        int index = path.lastIndexOf(JAR_PATH_EXT);
        if (index < 0) {
            // common dir
            final List<String> paths = new ArrayList<>();
            final File[] files = ls(path);
            for (File file : files) {
                if (file.isFile()) {
                    paths.add(file.getName());
                }
            }
            return paths;
        }

        // jar file
        path = getAbsolutePath(path);
        // jar file path
        index = index + JAR_FILE_EXT.length();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(path.substring(0, index));
            return ZipHelper.listFileNames(jarFile, Strings.removePrefix(path.substring(index + 1), "/"));
        } catch (IOException e) {
            throw new IOException(String.format("Can not read file path of [%s]", path), e);
        } finally {
            IOStreams.closeQuietly(jarFile);
        }
    }

    public static File newFile(String path) {
        return new File(path);
    }

    public static File of(String path) {
        if (null == path) {
            return null;
        }
        return new File(getAbsolutePath(path));
    }

    public static File of(String parent, String path) {
        return of(new File(parent), path);
    }

    public static File of(File parent, String path) {
        if (Strings.isBlank(path)) {
            throw new NullPointerException("File path is blank!");
        }
        return checkSlip(parent, buildFile(parent, path));
    }

    public static File of(File directory, String... names) {
        Require.checkNotNull(directory, "directory must not be null");
        if (Arrays.isEmpty(names)) {
            return directory;
        }

        File file = directory;
        for (String name : names) {
            if (null != name) {
                file = of(file, name);
            }
        }
        return file;
    }

    public static File of(String... names) {
        if (Arrays.isEmpty(names)) {
            return null;
        }

        File file = null;
        for (String name : names) {
            if (file == null) {
                file = of(name);
            } else {
                file = of(file, name);
            }
        }
        return file;
    }

    public static File of(URI uri) {
        if (uri == null) {
            throw new NullPointerException("File uri is null!");
        }
        return new File(uri);
    }

    public static File of(URL url) throws URISyntaxException {
        return new File(new URI(url.toString()));
    }

    public static String getTmpDirPath() {
        return System.getProperty("java.io.tmpdir");
    }

    public static File getTmpDir() {
        return of(getTmpDirPath());
    }

    public static String getUserHomePath() {
        return System.getProperty("user.home");
    }

    public static File getUserHomeDir() {
        return of(getUserHomePath());
    }

    public static boolean exist(String path) {
        return (null != path) && of(path).exists();
    }

    public static boolean exist(File file) {
        return (null != file) && file.exists();
    }

    public static boolean exist(String directory, String regexp) {
        final File file = new File(directory);
        if (!file.exists()) {
            return false;
        }

        final String[] fileList = file.list();
        if (fileList == null) {
            return false;
        }

        for (String fileName : fileList) {
            if (fileName.matches(regexp)) {
                return true;
            }

        }
        return false;
    }

    public static Date lastModifiedTime(File file) {
        if (!exist(file)) {
            return null;
        }

        return new Date(file.lastModified());
    }

    public static Date lastModifiedTime(String path) {
        return lastModifiedTime(new File(path));
    }

    public static long size(File file) {
        if (null == file || !file.exists() || isSymlink(file)) {
            return 0;
        }

        if (file.isDirectory()) {
            long size = 0L;
            File[] subFiles = file.listFiles();
            if (Arrays.isEmpty(subFiles)) {
                return 0L;// empty directory
            }
            for (File subFile : subFiles) {
                size += size(subFile);
            }
            return size;
        } else {
            return file.length();
        }
    }

    public static boolean newerThan(File file, File reference) {
        if (null == reference || !reference.exists()) {
            return true; // newer than not exists file
        }
        return newerThan(file, reference.lastModified());
    }

    public static boolean newerThan(File file, long timeMillis) {
        if (null == file || !file.exists()) {
            return false; // older than already exists file
        }
        return file.lastModified() > timeMillis;
    }

    public static File touch(String fullFilePath) throws IOException {
        if (fullFilePath == null) {
            return null;
        }
        return touch(of(fullFilePath));
    }

    public static File touch(File file) throws IOException {
        if (null == file) {
            return null;
        }
        if (!file.exists()) {
            mkParentDirs(file);
            try {
                // noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return file;
    }

    public static File touch(File parent, String path) throws IOException {
        return touch(of(parent, path));
    }

    public static File touch(String parent, String path) throws IOException {
        return touch(of(parent, path));
    }

    public static File mkParentDirs(File file) {
        if (null == file) {
            return null;
        }
        return mkdir(file.getParentFile());
    }

    public static File mkParentDirs(String path) {
        if (path == null) {
            return null;
        }
        return mkParentDirs(of(path));
    }

    public static boolean del(String fullFileOrDirPath) throws IOException {
        return del(of(fullFileOrDirPath));
    }

    public static boolean del(File file) throws IOException {
        if (file == null || !file.exists()) {
            return true;
        }

        if (file.isDirectory()) {
            boolean isOk = clean(file);
            if (!isOk) {
                return false;
            }
        }

        final Path path = file.toPath();
        try {
            PathHelper.delFile(path);
        } catch (DirectoryNotEmptyException e) {
            PathHelper.del(path);
        } catch (IOException e) {
            throw new IOException(e);
        }

        return true;
    }

    public static boolean clean(String dirPath) throws IOException {
        return clean(of(dirPath));
    }

    public static boolean clean(File directory) throws IOException {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return true;
        }

        final File[] files = directory.listFiles();
        if (null != files) {
            for (File childFile : files) {
                if (!del(childFile)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean cleanEmpty(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return true;
        }

        final File[] files = directory.listFiles();
        if (Arrays.isEmpty(files)) {
            return directory.delete();
        }

        for (File childFile : files) {
            cleanEmpty(childFile);
        }

        return true;
    }

    public static File mkdir(String dirPath) {
        if (dirPath == null) {
            return null;
        }
        final File dir = of(dirPath);

        return mkdir(dir);
    }

    public static File mkdir(File dir) {
        if (dir == null) {
            return null;
        }
        if (!dir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static File createTempFile(File dir) throws IOException {
        return createTempFile("", null, dir, true);
    }

    public static File createTempFile(File dir, boolean isReCreat) throws IOException {
        return createTempFile("", null, dir, isReCreat);
    }

    /**
     * prefix[Random].suffix
     */
    public static File createTempFile(String prefix, String suffix, File dir, boolean isReCreat) throws IOException {
        int exceptionsCount = 0;
        while (true) {
            try {
                File file = File.createTempFile(prefix, suffix, mkdir(dir)).getCanonicalFile();
                if (isReCreat) {
                    // noinspection ResultOfMethodCallIgnored
                    file.delete();
                    // noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                }
                return file;
            } catch (IOException ioex) { // fixes java.io.WinNTFileSystem.createFileExclusively access denied
                if (++exceptionsCount >= 50) {
                    throw new IOException(ioex);
                }
            }
        }
    }

    private static void registerTemporaryCleanup(File file) {
        file.deleteOnExit();
        TEMPORARY_CLEANUP_REGISTRY.put(file.getName(), file, ExpirationPolicy.CREATED);
    }

    /**
     * Create temporary directory
     */
    public static File createTemporaryDirectory() throws IOException {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";
        for (int counter = 0; counter < 100; counter++) {
            Path path = Files.createTempDirectory(baseDir.toPath(), baseName + counter);
            File file = path.toFile();
            if (file.exists() && file.isDirectory()) {
                return file;
            }
        }
        throw new IllegalStateException("Failed to create temporary directory within 100 attempts.");
    }

    /**
     * Create terporary file
     */
    public static File createTemporary(String fileName) {
        try {
            File temporary = new File(createTemporaryDirectory(), fileName);
            registerTemporaryCleanup(temporary);
            return temporary;
        } catch (IOException e) {
            throw new RuntimeException("创建临时目录异常", e);
        }
    }

    /**
     * Clean temproary
     */
    public static void cleanTemporary(File temporary) {
        try {
            if (temporary != null && temporary.isFile() && temporary.exists()) {
                temporary.delete();
            }
        } catch (Throwable ignore) {
        }
    }

    /**
     * Clean temporary list
     */
    public static void cleanTemporarys(Collection<File> temporarys) {
        for (File temporary : temporarys) {
            cleanTemporary(temporary);
        }
    }

    /**
     * Split temporary file
     *
     * @param maxChunkSize is byte size, 1kb=1024bytes
     */
    public static List<File> splitTemporaryBySize(File temporary, int maxChunkSize) throws IOException {
        List<File> results = new ArrayList<>();
        try (InputStream in = Files.newInputStream(temporary.toPath())) {
            final byte[] buffer = new byte[maxChunkSize];
            int dataRead = in.read(buffer);
            while (dataRead > -1) {
                File fileChunk = writeSplitTemporary(temporary.getName(), results.size(), buffer, dataRead);
                results.add(fileChunk);
                dataRead = in.read(buffer);
            }
        }

        return results;
    }

    private static File writeSplitTemporary(String filename, int fileIndex, byte[] buffer, int length) throws IOException {
        File outputFile = createTemporary(filename + "_split_" + fileIndex);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(buffer, 0, length);
        }

        return outputFile;
    }

    /**
     * Split file by file count
     */
    public List<File> splitTemporaryByCount(File largeFile, int maxFileCount) throws IOException {
        return splitTemporaryBySize(largeFile, computeSplitFileSize(largeFile.length(), maxFileCount));
    }

    private int computeSplitFileSize(long totalBytes, int numberOfFiles) {
        if (totalBytes % numberOfFiles != 0) {
            totalBytes = ((totalBytes / numberOfFiles) + 1) * numberOfFiles;
        }
        long x = totalBytes / numberOfFiles;
        if (x > Integer.MAX_VALUE) {
            throw new NumberFormatException("Compute split file size byte chunk too large: " + x);
        }

        return (int) x;
    }

    /**
     * Merge file
     */
    public static File mergeTemporary(String filename, List<File> list) throws IOException {
        File outputFile = createTemporary(filename);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (File file : list) {
                Files.copy(file.toPath(), fos);
            }
        }

        return outputFile;
    }

    public static File copyFile(String src, String dest, StandardCopyOption... options) throws IOException {
        Require.checkNotNull(src, "Source File path is blank !");
        Require.checkNotNull(dest, "Destination File path is blank !");

        return PathHelper.copyFile(Paths.get(src), Paths.get(dest), options).toFile();
    }

    public static File copyFile(File src, File dest, StandardCopyOption... options) throws IOException {
        // check
        Require.checkNotNull(src, "Source File is null !");
        if (!src.exists()) {
            throw new IOException("File not exist: " + src);
        }
        Require.checkNotNull(dest, "Destination File or directiory is null !");
        if (src.equals(dest)) {
            throw Throws.runtime("Files '%s' and '%s' are equal", src, dest);
        }
        return PathHelper.copyFile(src.toPath(), dest.toPath(), options).toFile();
    }

    public static File copy(String srcPath, String destPath, boolean isOverride) throws IOException {
        return copy(of(srcPath), of(destPath), isOverride);
    }

    public static File copy(File src, File dest, boolean isOverride) throws IOException {
        return FileCopier.create(src, dest).setOverride(isOverride).copy();
    }

    public static File copyContent(File src, File dest, boolean isOverride) {
        return FileCopier.create(src, dest).setCopyContentIfDir(true).setOverride(isOverride).copy();
    }

    public static File copyFilesFromDir(File src, File dest, boolean isOverride) {
        return FileCopier.create(src, dest).setCopyContentIfDir(true).setOnlyCopyFile(true).setOverride(isOverride).copy();
    }

    public static void move(File src, File target, boolean isOverride) throws IOException {
        Require.checkNotNull(src, "Src file must be not null!");
        Require.checkNotNull(target, "target file must be not null!");

        PathHelper.move(src.toPath(), target.toPath(), isOverride);
    }

    public static void moveContent(File src, File target, boolean isOverride) throws IOException {
        Require.checkNotNull(src, "Src file must be not null!");
        Require.checkNotNull(target, "target file must be not null!");

        PathHelper.moveContent(src.toPath(), target.toPath(), isOverride);
    }

    public static File rename(File file, String newName, boolean isOverride) throws IOException {
        return rename(file, newName, false, isOverride);
    }

    public static File rename(File file, String newName, boolean isRetainExt, boolean isOverride) throws IOException {
        if (isRetainExt) {
            final String extName = getExtensionName(file);
            if (Strings.isNotBlank(extName)) {
                newName = newName.concat(".").concat(extName);
            }
        }
        return PathHelper.rename(file.toPath(), newName, isOverride).toFile();
    }

    public static String getExtensionName(File file) {
        if (null == file) {
            return null;
        }
        if (file.isDirectory()) {
            return null;
        }
        return getExtensionName(file.getName());
    }

    public static String getExtensionName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        } else {
            String ext = fileName.substring(index + 1);
            return ext.contains("/") || ext.contains("\\") ? "" : ext;
        }
    }

    public static String getCanonicalPath(File file) throws IOException {
        if (null == file) {
            return null;
        }

        return file.getCanonicalPath();
    }

    public static String getAbsolutePath(File file) {
        if (file == null) {
            return null;
        }

        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    public static String getAbsolutePath(String path) {
        if (Strings.isBlank(path)) {
            return null;
        }

        File file = Paths.get(path).toFile();
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    public static boolean isAbsolutePath(String path) {
        if (Strings.isEmpty(path)) {
            return false;
        }

        return '/' == path.charAt(0) || path.matches("^[a-zA-Z]:([/\\\\].*)?");
    }

    public static boolean isDirectory(String path) {
        return (null != path) && of(path).isDirectory();
    }

    public static boolean isDirectory(File file) {
        return (null != file) && file.isDirectory();
    }

    public static boolean isFile(String path) {
        return (null != path) && of(path).isFile();
    }

    public static boolean isFile(File file) {
        return (null != file) && file.isFile();
    }

    public static boolean equals(File file1, File file2) throws IOException {
        Require.checkNotNull(file1);
        Require.checkNotNull(file2);
        if (!file1.exists() || !file2.exists()) {
            return !file1.exists() && !file2.exists() && pathEquals(file1, file2);
        }
        return PathHelper.equals(file1.toPath(), file2.toPath());
    }

    public static boolean pathEquals(File file1, File file2) {
        if (isWindows()) {
            try {
                if (Strings.equalsIgnoreCase(file1.getCanonicalPath(), file2.getCanonicalPath())) {
                    return true;
                }
            } catch (Exception e) {
                if (Strings.equalsIgnoreCase(file1.getAbsolutePath(), file2.getAbsolutePath())) {
                    return true;
                }
            }
        } else {
            try {
                if (Strings.equals(file1.getCanonicalPath(), file2.getCanonicalPath())) {
                    return true;
                }
            } catch (Exception e) {
                if (Strings.equals(file1.getAbsolutePath(), file2.getAbsolutePath())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int lastIndexOfSeparator(String filePath) {
        if (Strings.isNotEmpty(filePath)) {
            int i = filePath.length();
            char c;
            while (--i >= 0) {
                c = filePath.charAt(i);
                if (Chars.isFileSeparator(c)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static boolean isModified(File file, long lastModifyTime) {
        if (null == file || !file.exists()) {
            return true;
        }
        return file.lastModified() != lastModifyTime;
    }

    public static byte[] readBytes(File file) {
        return FileReader.create(file).readBytes();
    }

    public static byte[] readBytes(String filePath) {
        return readBytes(of(filePath));
    }

    public static String readUtf8String(File file) {
        return readString(file, Charsets.UTF_8);
    }

    public static String readUtf8String(String path) {
        return readString(path, Charsets.UTF_8);
    }

    public static String readString(File file, Charset charset) {
        return FileReader.create(file, charset).readString();
    }

    public static String readString(String path, Charset charset) {
        return readString(of(path), charset);
    }

    public static String readString(URL url, Charset charset) {
        if (url == null) {
            throw new NullPointerException("Empty url provided!");
        }

        InputStream in = null;
        try {
            in = url.openStream();
            return IOStreams.read(in, charset);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            IOStreams.closeQuietly(in);
        }
    }

    public static <T extends Collection<String>> T readUtf8Lines(String path, T collection) {
        return readLines(path, Charsets.UTF_8, collection);
    }

    public static <T extends Collection<String>> T readLines(String path, String charset, T collection) {
        return readLines(of(path), charset, collection);
    }

    public static <T extends Collection<String>> T readLines(String path, Charset charset, T collection) {
        return readLines(of(path), charset, collection);
    }

    public static <T extends Collection<String>> T readUtf8Lines(File file, T collection) {
        return readLines(file, Charsets.UTF_8, collection);
    }

    public static <T extends Collection<String>> T readLines(File file, String charset, T collection) {
        return FileReader.create(file, Charsets.charset(charset)).readLines(collection);
    }

    public static <T extends Collection<String>> T readLines(File file, Charset charset, T collection) {
        return FileReader.create(file, charset).readLines(collection);
    }

    public static <T extends Collection<String>> T readUtf8Lines(URL url, T collection) throws IOException {
        return readLines(url, Charsets.UTF_8, collection);
    }

    public static <T extends Collection<String>> T readLines(URL url, Charset charset, T collection) throws IOException {
        InputStream in = null;
        try {
            in = url.openStream();
            return IOStreams.readLines(in, charset, collection);
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            IOStreams.closeQuietly(in);
        }
    }

    public static List<String> readUtf8Lines(URL url) throws IOException {
        return readLines(url, Charsets.UTF_8);
    }

    public static List<String> readLines(URL url, Charset charset) throws IOException {
        return readLines(url, charset, new ArrayList<>());
    }

    public static List<String> readUtf8Lines(String path) {
        return readLines(path, Charsets.UTF_8);
    }

    public static List<String> readLines(String path, String charset) {
        return readLines(path, charset, new ArrayList<>());
    }

    public static List<String> readLines(String path, Charset charset) {
        return readLines(path, charset, new ArrayList<>());
    }

    public static List<String> readUtf8Lines(File file) {
        return readLines(file, Charsets.UTF_8);
    }

    public static List<String> readLines(File file, String charset) {
        return readLines(file, charset, new ArrayList<>());
    }

    public static List<String> readLines(File file, Charset charset) {
        return readLines(file, charset, new ArrayList<>());
    }

    public static void readUtf8Lines(File file, Consumer<String> lineHandler) throws IOException {
        readLines(file, Charsets.UTF_8, lineHandler);
    }

    public static void readLines(File file, Charset charset, Consumer<String> lineHandler) throws IOException {
        FileReader.create(file, charset).readLines(lineHandler);
    }

    public static void readLines(RandomAccessFile file, Charset charset, Consumer<String> lineHandler) throws IOException {
        String line;
        while ((line = file.readLine()) != null) {
            lineHandler.accept(Charsets.convert(line, Charsets.ISO_8859_1, charset));
        }
    }

    public static void readLine(RandomAccessFile file, Charset charset, Consumer<String> lineHandler) throws IOException {
        final String line = readLine(file, charset);
        if (null != line) {
            lineHandler.accept(line);
        }
    }

    public static String readLine(RandomAccessFile file, Charset charset) throws IOException {
        String line = file.readLine();
        if (null != line) {
            return Charsets.convert(line, Charsets.ISO_8859_1, charset);
        }

        return null;
    }

    public static <T> T loadUtf8(String path, FileReader.ReaderHandler<T> readerHandler) {
        return load(path, Charsets.UTF_8, readerHandler);
    }

    public static <T> T load(String path, String charset, FileReader.ReaderHandler<T> readerHandler) {
        return FileReader.create(of(path), Charsets.charset(charset)).read(readerHandler);
    }

    public static <T> T load(String path, Charset charset, FileReader.ReaderHandler<T> readerHandler) {
        return FileReader.create(of(path), charset).read(readerHandler);
    }

    public static <T> T loadUtf8(File file, FileReader.ReaderHandler<T> readerHandler) {
        return load(file, Charsets.UTF_8, readerHandler);
    }

    public static <T> T load(File file, Charset charset, FileReader.ReaderHandler<T> readerHandler) {
        return FileReader.create(file, charset).read(readerHandler);
    }

    public static BufferedOutputStream getOutputStream(File file) throws IOException {
        final OutputStream out;
        try {
            out = Files.newOutputStream(touch(file).toPath());
        } catch (IOException e) {
            throw new IOException(e);
        }
        return IOStreams.createBuffered(out);
    }

    public static BufferedOutputStream getOutputStream(String path) throws IOException {
        return getOutputStream(touch(path));
    }

    public static BufferedWriter getWriter(String path, Charset charset, boolean isAppend) throws IOException {
        return getWriter(touch(path), charset, isAppend);
    }

    public static BufferedWriter getWriter(File file, Charset charset, boolean isAppend) {
        return FileWriter.create(file, charset).getWriter(isAppend);
    }

    public static PrintWriter getPrintWriter(String path, String charset, boolean isAppend) throws IOException {
        return new PrintWriter(getWriter(path, Charsets.charset(charset), isAppend));
    }

    public static PrintWriter getPrintWriter(String path, Charset charset, boolean isAppend) throws IOException {
        return new PrintWriter(getWriter(path, charset, isAppend));
    }

    public static PrintWriter getPrintWriter(File file, String charset, boolean isAppend) {
        return new PrintWriter(getWriter(file, Charsets.charset(charset), isAppend));
    }

    public static PrintWriter getPrintWriter(File file, Charset charset, boolean isAppend) {
        return new PrintWriter(getWriter(file, charset, isAppend));
    }

    public static String getLineSeparator() {
        return System.lineSeparator();
    }

    public static File writeUtf8String(String content, String path) throws IOException {
        return writeString(content, path, Charsets.UTF_8);
    }

    public static File writeUtf8String(String content, File file) {
        return writeString(content, file, Charsets.UTF_8);
    }

    public static File writeString(String content, String path, String charset) throws IOException {
        return writeString(content, touch(path), charset);
    }

    public static File writeString(String content, String path, Charset charset) throws IOException {
        return writeString(content, touch(path), charset);
    }

    public static File writeString(String content, File file, String charset) {
        return FileWriter.create(file, Charsets.charset(charset)).write(content);
    }

    public static File writeString(String content, File file, Charset charset) {
        return FileWriter.create(file, charset).write(content);
    }

    public static File appendUtf8String(String content, String path) throws IOException {
        return appendString(content, path, Charsets.UTF_8);
    }

    public static File appendString(String content, String path, String charset) throws IOException {
        return appendString(content, touch(path), charset);
    }

    public static File appendString(String content, String path, Charset charset) throws IOException {
        return appendString(content, touch(path), charset);
    }

    public static File appendUtf8String(String content, File file) {
        return appendString(content, file, Charsets.UTF_8);
    }

    public static File appendString(String content, File file, String charset) {
        return FileWriter.create(file, Charsets.charset(charset)).append(content);
    }

    public static File appendString(String content, File file, Charset charset) {
        return FileWriter.create(file, charset).append(content);
    }

    public static <T> File writeUtf8Lines(Collection<T> list, String path) {
        return writeLines(list, path, Charsets.UTF_8);
    }

    public static <T> File writeUtf8Lines(Collection<T> list, File file) {
        return writeLines(list, file, Charsets.UTF_8);
    }

    public static <T> File writeLines(Collection<T> list, String path, String charset) {
        return writeLines(list, path, charset, false);
    }

    public static <T> File writeLines(Collection<T> list, String path, Charset charset) {
        return writeLines(list, path, charset, false);
    }

    public static <T> File writeLines(Collection<T> list, File file, String charset) {
        return writeLines(list, file, charset, false);
    }

    public static <T> File writeLines(Collection<T> list, File file, Charset charset) {
        return writeLines(list, file, charset, false);
    }

    public static <T> File appendUtf8Lines(Collection<T> list, File file) {
        return appendLines(list, file, Charsets.UTF_8);
    }

    public static <T> File appendUtf8Lines(Collection<T> list, String path) {
        return appendLines(list, path, Charsets.UTF_8);
    }

    public static <T> File appendLines(Collection<T> list, String path, String charset) {
        return writeLines(list, path, charset, true);
    }

    public static <T> File appendLines(Collection<T> list, File file, String charset) {
        return writeLines(list, file, charset, true);
    }

    public static <T> File appendLines(Collection<T> list, String path, Charset charset) {
        return writeLines(list, path, charset, true);
    }

    public static <T> File appendLines(Collection<T> list, File file, Charset charset) {
        return writeLines(list, file, charset, true);
    }

    public static <T> File writeLines(Collection<T> list, String path, String charset, boolean isAppend) {
        return writeLines(list, of(path), charset, isAppend);
    }

    public static <T> File writeLines(Collection<T> list, String path, Charset charset, boolean isAppend) {
        return writeLines(list, of(path), charset, isAppend);
    }

    public static <T> File writeLines(Collection<T> list, File file, String charset, boolean isAppend) {
        return FileWriter.create(file, Charsets.charset(charset)).writeLines(list, isAppend);
    }

    public static <T> File writeLines(Collection<T> list, File file, Charset charset, boolean isAppend) {
        return FileWriter.create(file, charset).writeLines(list, isAppend);
    }

    public static File writeUtf8Map(Map<?, ?> map, File file, String kvSeparator, boolean isAppend) {
        return FileWriter.create(file, Charsets.UTF_8).writeMap(map, kvSeparator, isAppend);
    }

    public static File writeMap(Map<?, ?> map, File file, Charset charset, String kvSeparator, boolean isAppend) {
        return FileWriter.create(file, charset).writeMap(map, kvSeparator, isAppend);
    }

    public static File writeBytes(byte[] data, String path) throws IOException {
        return writeBytes(data, touch(path));
    }

    public static File writeBytes(byte[] data, File dest) {
        return writeBytes(data, dest, 0, data.length, false);
    }

    public static File writeBytes(byte[] data, File dest, int off, int len, boolean isAppend) {
        return FileWriter.create(dest).write(data, off, len, isAppend);
    }

    public static File writeFromStream(InputStream in, File dest) {
        return writeFromStream(in, dest, true);
    }

    public static File writeFromStream(InputStream in, File dest, boolean isCloseIn) {
        return FileWriter.create(dest).writeFromStream(in, isCloseIn);
    }

    public static File writeFromStream(InputStream in, String fullFilePath) throws IOException {
        return writeFromStream(in, touch(fullFilePath));
    }

    public static long writeToStream(File file, OutputStream out) {
        return FileReader.create(file).writeToStream(out);
    }

    public static long writeToStream(String fullFilePath, OutputStream out) throws IOException {
        return writeToStream(touch(fullFilePath), out);
    }

    /**
     * <pre>
     * getParent("d:/aaa/bbb/cc/ddd", 0) -> "d:/aaa/bbb/cc/ddd"
     * getParent("d:/aaa/bbb/cc/ddd", 2) -> "d:/aaa/bbb"
     * getParent("d:/aaa/bbb/cc/ddd", 4) -> "d:/"
     * getParent("d:/aaa/bbb/cc/ddd", 5) -> null
     * </pre>
     */
    public static String getParent(String filePath, int level) throws IOException {
        final File parent = getParent(of(filePath), level);
        return null == parent ? null : parent.getCanonicalPath();
    }

    /**
     * <pre>
     * getParent(file("d:/aaa/bbb/cc/ddd", 0)) -> "d:/aaa/bbb/cc/ddd"
     * getParent(file("d:/aaa/bbb/cc/ddd", 2)) -> "d:/aaa/bbb"
     * getParent(file("d:/aaa/bbb/cc/ddd", 4)) -> "d:/"
     * getParent(file("d:/aaa/bbb/cc/ddd", 5)) -> null
     * </pre>
     */
    public static File getParent(File file, int level) throws IOException {
        if (level < 1 || null == file) {
            return file;
        }

        File parentFile = file.getCanonicalFile().getParentFile();
        if (1 == level) {
            return parentFile;
        }
        return getParent(parentFile, level - 1);
    }

    public static File checkSlip(File parentFile, File file) throws IllegalArgumentException {
        if (null != parentFile && null != file) {
            String parentCanonicalPath;
            String canonicalPath;
            try {
                parentCanonicalPath = parentFile.getCanonicalPath();
                canonicalPath = file.getCanonicalPath();
            } catch (IOException e) {
                parentCanonicalPath = parentFile.getAbsolutePath();
                canonicalPath = file.getAbsolutePath();
            }
            if (!canonicalPath.startsWith(parentCanonicalPath)) {
                throw new IllegalArgumentException("New file is outside of the parent dir: " + file.getName());
            }
        }
        return file;
    }

    public static String getMimeType(String filePath) throws IOException {
        String contentType = URLConnection.getFileNameMap().getContentTypeFor(filePath);
        if (null == contentType) {
            if (filePath.endsWith(".css")) {
                contentType = "text/css";
            } else if (filePath.endsWith(".js")) {
                contentType = "application/x-javascript";
            }
        }

        if (null == contentType) {
            contentType = PathHelper.getMimeType(Paths.get(filePath));
        }

        return contentType;
    }

    public static boolean isSymlink(File file) {
        return PathHelper.isSymlink(file.toPath());
    }

    public static boolean isChild(File parent, File child) {
        Require.checkNotNull(parent);
        Require.checkNotNull(child);
        return PathHelper.isChild(parent.toPath(), child.toPath());
    }

    public static RandomAccessFile createRandomAccessFile(Path path, FileMode mode) {
        return createRandomAccessFile(path.toFile(), mode);
    }

    public static RandomAccessFile createRandomAccessFile(File file, FileMode mode) {
        try {
            return new RandomAccessFile(file, mode.name());
        } catch (FileNotFoundException e) {
            throw new IORuntimeException(e);
        }
    }

    private static File buildFile(File outFile, String fileName) {
        fileName = fileName.replace('\\', '/');
        if (!isWindows() && fileName.lastIndexOf("/", fileName.length() - 2) > 0) {
            final List<String> pathParts = java.util.Arrays.asList(Strings.split(fileName, "/"));
            final int lastPartIndex = pathParts.size() - 1;
            for (int i = 0; i < lastPartIndex; i++) {
                outFile = new File(outFile, pathParts.get(i));
            }
            // noinspection ResultOfMethodCallIgnored
            outFile.mkdirs();
            fileName = pathParts.get(lastPartIndex);
        }

        return new File(outFile, fileName);
    }
}
