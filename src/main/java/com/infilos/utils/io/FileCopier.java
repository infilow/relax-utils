package com.infilos.utils.io;

import com.infilos.utils.Arrays;
import com.infilos.utils.FileHelper;
import com.infilos.utils.Require;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.function.Predicate;

public class FileCopier {
    protected File src;
    protected File dest;
    private Predicate<File> copyFilter;
    private boolean isOverride;
    private boolean isCopyAttributes;
    private boolean isCopyContentIfDir;
    private boolean isOnlyCopyFile;

    public static FileCopier create(String srcPath, String destPath) {
        return new FileCopier(FileHelper.of(srcPath), FileHelper.of(destPath));
    }

    public static FileCopier create(File src, File dest) {
        return new FileCopier(src, dest);
    }

    public FileCopier(File src, File dest) {
        this.src = src;
        this.dest = dest;
    }

    public File getSrc() {
        return src;
    }

    public FileCopier setSrc(File src) {
        this.src = src;
        return this;
    }

    public File getDest() {
        return dest;
    }

    public FileCopier setDest(File dest) {
        this.dest = dest;
        return this;
    }

    public Predicate<File> getCopyFilter() {
        return copyFilter;
    }

    public FileCopier setCopyFilter(Predicate<File> copyFilter) {
        this.copyFilter = copyFilter;
        return this;
    }

    public boolean isOverride() {
        return isOverride;
    }

    public FileCopier setOverride(boolean isOverride) {
        this.isOverride = isOverride;
        return this;
    }

    public boolean isCopyAttributes() {
        return isCopyAttributes;
    }

    public FileCopier setCopyAttributes(boolean isCopyAttributes) {
        this.isCopyAttributes = isCopyAttributes;
        return this;
    }

    public boolean isCopyContentIfDir() {
        return isCopyContentIfDir;
    }

    public FileCopier setCopyContentIfDir(boolean isCopyContentIfDir) {
        this.isCopyContentIfDir = isCopyContentIfDir;
        return this;
    }

    public boolean isOnlyCopyFile() {
        return isOnlyCopyFile;
    }

    public FileCopier setOnlyCopyFile(boolean isOnlyCopyFile) {
        this.isOnlyCopyFile = isOnlyCopyFile;
        return this;
    }

    /**
     * 执行拷贝<br>
     * 拷贝规则为：
     * <pre>
     * 1、源为文件，目标为已存在目录，则拷贝到目录下，文件名不变
     * 2、源为文件，目标为不存在路径，则目标以文件对待（自动创建父级目录）比如：/dest/aaa，如果aaa不存在，则aaa被当作文件名
     * 3、源为文件，目标是一个已存在的文件，则当{@link #setOverride(boolean)}设为true时会被覆盖，默认不覆盖
     * 4、源为目录，目标为已存在目录，当{@link #setCopyContentIfDir(boolean)}为true时，只拷贝目录中的内容到目标目录中，否则整个源目录连同其目录拷贝到目标目录中
     * 5、源为目录，目标为不存在路径，则自动创建目标为新目录，然后按照规则4复制
     * 6、源为目录，目标为文件，抛出IO异常
     * 7、源路径和目标路径相同时，抛出IO异常
     * </pre>
     *
     * @return 拷贝后目标的文件或目录
     * @throws IORuntimeException IO异常
     */
    public File copy() throws IORuntimeException {
        final File src = this.src;
        final File dest = this.dest;
        Require.checkNotNull(src, "Source File is null !");
        Require.checkNotNull(dest, "Destination File or directiory is null !");

        if (!src.exists()) {
            throw new IORuntimeException("File not exist: " + src);
        }

        if (src.equals(dest)) {
            throw new IORuntimeException("Files '{}' and '{}' are equal", src, dest);
        }

        if (src.isDirectory()) {
            if (dest.exists() && !dest.isDirectory()) {
                throw new IORuntimeException("Src is a directory but dest is a file!");
            }
            if (FileHelper.isChild(src, dest)) {
                throw new IORuntimeException("Dest is a sub directory of src !");
            }

            final File subTarget = isCopyContentIfDir ? dest : FileHelper.mkdir(FileHelper.of(dest, src.getName()));
            internalCopyDirContent(src, subTarget);
        } else {
            internalCopyFile(src, dest);
        }
        return dest;
    }

    private void internalCopyDirContent(File src, File dest) throws IORuntimeException {
        if (null != copyFilter && !copyFilter.test(src)) {
            return;
        }

        if (!dest.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dest.mkdirs();
        } else if (!dest.isDirectory()) {
            throw new IORuntimeException(String.format("Src [%s] is a directory but dest [%s] is a file!", src.getPath(), dest.getPath()));
        }

        final String[] files = src.list();
        if (Arrays.isNotEmpty(files)) {
            File srcFile;
            File destFile;
            for (String file : files) {
                srcFile = new File(src, file);
                destFile = this.isOnlyCopyFile ? dest : new File(dest, file);
                if (srcFile.isDirectory()) {
                    internalCopyDirContent(srcFile, destFile);
                } else {
                    internalCopyFile(srcFile, destFile);
                }
            }
        }
    }

    private void internalCopyFile(File src, File dest) throws IORuntimeException {
        if (null != copyFilter && !copyFilter.test(src)) {
            return;
        }

        if (dest.exists()) {
            if (dest.isDirectory()) {
                dest = new File(dest, src.getName());
            }

            if (dest.exists() && !isOverride) {
                return;
            }
        } else {
            //noinspection ResultOfMethodCallIgnored
            dest.getParentFile().mkdirs();
        }

        final ArrayList<CopyOption> optionList = new ArrayList<>(2);
        if (isOverride) {
            optionList.add(StandardCopyOption.REPLACE_EXISTING);
        }
        if (isCopyAttributes) {
            optionList.add(StandardCopyOption.COPY_ATTRIBUTES);
        }

        try {
            Files.copy(src.toPath(), dest.toPath(), optionList.toArray(new CopyOption[0]));
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }
}