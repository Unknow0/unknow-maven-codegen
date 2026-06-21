package unknow.maven.codegen;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.maven.codegen.FileScanner.FileHandler;

public class ChangeWalker implements FileHandler {
	private static final Logger logger = LoggerFactory.getLogger(ChangeWalker.class);

	private final long lastRun;
	private boolean changed;

	public ChangeWalker(long last) {
		this.lastRun = last;
	}

	@Override
	public boolean handle(Path file, Path relative, BasicFileAttributes attrs) {
		long last = attrs.lastModifiedTime().toMillis();
		if (last > lastRun) {
			logger.info("Detected file {} changed at {}", file, Instant.ofEpochMilli(last));
			changed = true;
			return false;
		}
		return true;
	}

	public boolean hasChanged(String path, PathMatcher matcher) {
		return hasChanged(Paths.get(path), matcher);
	}

	public boolean hasChanged(String path, Collection<String> includes, Collection<String> excludes) {
		return hasChanged(Paths.get(path), PathMatchers.includes(includes, excludes));
	}

	public boolean hasChanged(Path path, PathMatcher matcher) {
		this.changed = false;
		try {
			FileScanner.scan(path, this, matcher);
			return changed;
		} catch (IOException e) {
			logger.warn("Failed to check last change", e);
			return true;
		}
	}
}