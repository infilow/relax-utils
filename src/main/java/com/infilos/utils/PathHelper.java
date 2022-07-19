package com.infilos.utils;

import com.infilos.utils.io.FileCopyVisitor;
import com.infilos.utils.io.FileDelVisitor;
import com.infilos.utils.io.FileMoveVisitor;
import com.infilos.utils.io.IORuntimeException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@SuppressWarnings("unused")
public final class PathHelper {
    private PathHelper() {
    }

    public static boolean isDirEmpty(Path dirPath) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dirPath)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> loopFiles(Path path, FileFilter fileFilter) {
        return loopFiles(path, -1, fileFilter);
    }

    public static List<File> loopFiles(Path path, int maxDepth, FileFilter fileFilter) {
        final List<File> fileList = new ArrayList<>();

        if (null == path || !Files.exists(path)) {
            return fileList;
        } else if (!isDirectory(path)) {
            final File file = path.toFile();
            if (null == fileFilter || fileFilter.accept(file)) {
                fileList.add(file);
            }
            return fileList;
        }

        walkFiles(path, maxDepth, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                final File file = path.toFile();
                if (null == fileFilter || fileFilter.accept(file)) {
                    fileList.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return fileList;
    }

    public static void walkFiles(Path start, FileVisitor<? super Path> visitor) {
        walkFiles(start, -1, visitor);
    }

    public static void walkFiles(Path start, int maxDepth, FileVisitor<? super Path> visitor) {
        if (maxDepth < 0) {
            maxDepth = Integer.MAX_VALUE;
        }

        try {
            Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), maxDepth, visitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean del(Path path) throws IOException {
        if (Files.notExists(path)) {
            return true;
        }

        if (isDirectory(path)) {
            Files.walkFileTree(path, FileDelVisitor.INSTANCE);
        } else {
            delFile(path);
        }

        return true;
    }

    public static Path copyFile(Path src, Path dest, StandardCopyOption... options) throws IOException {
        return copyFile(src, dest, (CopyOption[]) options);
    }

    public static Path copyFile(Path src, Path target, CopyOption... options) throws IOException {
        Require.checkNotNull(src, "Source File is null");
        Require.checkNotNull(target, "Destination File or directory is null");

        final Path targetPath = isDirectory(target) ? target.resolve(src.getFileName()) : target;
        mkParentDirs(targetPath);

        return Files.copy(src, targetPath, options);
    }

    public static Path copy(Path src, Path target, CopyOption... options) throws IOException {
        Require.checkNotNull(src, "Src path must be not null");
        Require.checkNotNull(target, "Target path must be not null");

        if (isDirectory(src)) {
            return copyContent(src, target.resolve(src.getFileName()), options);
        }
        return copyFile(src, target, options);
    }

    public static Path copyContent(Path src, Path target, CopyOption... options) throws IOException {
        Require.checkNotNull(src, "Src path must be not null !");
        Require.checkNotNull(target, "Target path must be not null !");

        Files.walkFileTree(src, new FileCopyVisitor(src, target, options));

        return target;
    }

    public static boolean isDirectory(Path path) {
        return isDirectory(path, false);
    }

    public static boolean isDirectory(Path path, boolean isFollowLinks) {
        if (null == path) {
            return false;
        }
        final LinkOption[] options = isFollowLinks ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        return Files.isDirectory(path, options);
    }

    public static Path getPathElement(Path path, int index) {
        return subPath(path, index, index == -1 ? path.getNameCount() : index + 1);
    }

    public static Path getLastPathElement(Path path) {
        return getPathElement(path, path.getNameCount() - 1);
    }

    public static Path subPath(Path path, int fromIndex, int toIndex) {
        if (null == path) {
            return null;
        }
        final int len = path.getNameCount();

        if (fromIndex < 0) {
            fromIndex = len + fromIndex;
            if (fromIndex < 0) {
                fromIndex = 0;
            }
        } else if (fromIndex > len) {
            fromIndex = len;
        }

        if (toIndex < 0) {
            toIndex = len + toIndex;
            if (toIndex < 0) {
                toIndex = len;
            }
        } else if (toIndex > len) {
            toIndex = len;
        }

        if (toIndex < fromIndex) {
            int tmp = fromIndex;
            fromIndex = toIndex;
            toIndex = tmp;
        }

        if (fromIndex == toIndex) {
            return null;
        }
        return path.subpath(fromIndex, toIndex);
    }

    public static BasicFileAttributes getAttributes(Path path, boolean isFollowLinks) throws IOException {
        if (null == path) {
            return null;
        }

        final LinkOption[] options = isFollowLinks ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        return Files.readAttributes(path, BasicFileAttributes.class, options);
    }

    public static BufferedInputStream getInputStream(Path path) {
        try {
            final InputStream in = Files.newInputStream(path);
            return IOStreams.createBuffered(in);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static BufferedReader getReader(Path path) throws IOException {
        return getReader(path, Charsets.UTF_8);
    }

    public static BufferedReader getReader(Path path, Charset charset) throws IOException {
        return IOStreams.createReader(getInputStream(path), charset);
    }

    public static byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public static BufferedOutputStream getOutputStream(Path path) throws IOException {
        final OutputStream in = Files.newOutputStream(path);
        return IOStreams.createBuffered(in);
    }

    public static Path rename(Path path, String newName, boolean isOverride) throws IOException {
        return move(path, path.resolveSibling(newName), isOverride);
    }

    public static Path move(Path src, Path target, boolean isOverride) throws IOException {
        Require.checkNotNull(src, "Src path must be not null !");
        Require.checkNotNull(target, "Target path must be not null !");

        if (isDirectory(target)) {
            target = target.resolve(src.getFileName());
        }

        return moveContent(src, target, isOverride);
    }

    public static Path moveContent(Path src, Path target, boolean isOverride) throws IOException {
        Require.checkNotNull(src, "Src path must be not null !");
        Require.checkNotNull(target, "Target path must be not null !");
        final CopyOption[] options = isOverride ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[]{};

        mkParentDirs(target);
        try {
            return Files.move(src, target, options);
        } catch (IOException e) {
            try {
                Files.walkFileTree(src, new FileMoveVisitor(src, target, options));
                del(src);
            } catch (IOException e2) {
                throw new IOException(e2);
            }
            return target;
        }
    }

    public static boolean equals(Path file1, Path file2) throws IOException {
        try {
            return Files.isSameFile(file1, file2);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    public static boolean isFile(Path path, boolean isFollowLinks) {
        if (null == path) {
            return false;
        }
        final LinkOption[] options = isFollowLinks ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        return Files.isRegularFile(path, options);
    }

    public static boolean isSymlink(Path path) {
        return Files.isSymbolicLink(path);
    }

    public static boolean exists(Path path, boolean isFollowLinks) {
        final LinkOption[] options = isFollowLinks ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        return Files.exists(path, options);
    }

    public static boolean isChild(Path parent, Path child) {
        return toAbsoluteNormal(child).startsWith(toAbsoluteNormal(parent));
    }

    public static Path toAbsoluteNormal(Path path) {
        Require.checkNotNull(path);
        return path.toAbsolutePath().normalize();
    }

    public static String getMimeType(Path file) throws IOException {
        return Files.probeContentType(file);
    }

    public static Path mkdir(Path dir) throws IOException {
        if (null != dir && !exists(dir, false)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    public static Path mkParentDirs(Path path) throws IOException {
        return mkdir(path.getParent());
    }

    public static String getName(Path path) {
        if (null == path) {
            return null;
        }
        return path.getFileName().toString();
    }

    public static void delFile(Path path) throws IOException {
        try {
            Files.delete(path);
        } catch (AccessDeniedException e) {
            if (!path.toFile().delete()) { // read only file use file.delete
                throw e;
            }
        }
    }
}
