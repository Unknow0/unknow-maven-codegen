package unknow.maven.codegen;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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

	public boolean hasChanged(String path, PathMatcher... includes) {
		return hasChanged(Paths.get(path), Arrays.asList(includes), Collections.emptyList());
	}

	public boolean hasChanged(Path path, PathMatcher... includes) {
		return hasChanged(path, Arrays.asList(includes), Collections.emptyList());
	}

	public boolean hasChanged(String path, Collection<PathMatcher> includes, Collection<PathMatcher> excludes) {
		return hasChanged(Paths.get(path), includes, excludes);
	}

	public boolean hasChanged(Path path, Collection<PathMatcher> includes, Collection<PathMatcher> excludes) {
		this.changed = false;
		try {
			FileScanner.scan(path, this, includes, excludes);
			return changed;
		} catch (IOException e) {
			logger.warn("Failed to check last change", e);
			return true;
		}
	}
}