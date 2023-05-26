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

public class FileMoveVisitor extends SimpleFileVisitor<Path> {

	private final Path source;
	private final Path target;
	private boolean isTargetCreated;
	private final CopyOption[] copyOptions;

	public FileMoveVisitor(Path source, Path target, CopyOption... copyOptions) {
		if(PathHelper.exists(target, false) && !PathHelper.isDirectory(target)){
			throw new IllegalArgumentException("Target must be a directory");
		}
		this.source = source;
		this.target = target;
		this.copyOptions = copyOptions;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			throws IOException {
		initTarget();
		final Path targetDir = target.resolve(source.relativize(dir));
		
		if(!Files.exists(targetDir)){
			Files.createDirectories(targetDir);
		} else if(!Files.isDirectory(targetDir)){
			throw new FileAlreadyExistsException(targetDir.toString());
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		initTarget();
		Files.move(file, target.resolve(source.relativize(file)), copyOptions);
		return FileVisitResult.CONTINUE;
	}
	
	private void initTarget() throws IOException {
		if(!this.isTargetCreated){
			PathHelper.mkdir(this.target);
			this.isTargetCreated = true;
		}
	}
}
