package com.infilos.utils.io;

import com.infilos.utils.PathHelper;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileCopyVisitor extends SimpleFileVisitor<Path> {

	private final Path source;
	private final Path target;
	private final CopyOption[] copyOptions;
	private boolean isTargetCreated;

	public FileCopyVisitor(Path source, Path target, CopyOption... copyOptions) {
		if (PathHelper.exists(target, false) && !PathHelper.isDirectory(target)) {
			throw new IllegalArgumentException("Target must be a directory");
		}
		this.source = source;
		this.target = target;
		this.copyOptions = copyOptions;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		initTargetDir();
		final Path targetDir = buildRelativeTarget(dir);

		try {
			Files.copy(dir, targetDir, copyOptions);
		} catch (FileAlreadyExistsException e) {
			if (!Files.isDirectory(targetDir)) {
				throw e;
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		initTargetDir();
		Files.copy(file, buildRelativeTarget(file), copyOptions);
		
		return FileVisitResult.CONTINUE;
	}

	private Path buildRelativeTarget(Path file) {
		return target.resolve(source.relativize(file));
	}

	private void initTargetDir() throws IOException {
		if (!this.isTargetCreated) {
			PathHelper.mkdir(this.target);
			this.isTargetCreated = true;
		}
	}
}
