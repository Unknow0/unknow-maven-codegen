package unknow.maven.codegen;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public final class FileScanner {
	private FileScanner() {
	}

	/**
	 * scan folder
	 * @param path root path
	 * @param p handler notified for each file found
	 * @param includes includes only file matching these glob (null or empty for all)
	 * @throws IOException in case of error
	 */
	public static void scan(String path, FileHandler p, String... includes) throws IOException {
		scan(Paths.get(path), p, Arrays.asList(includes), Collections.emptyList());
	}

	/**
	 * scan folder
	 * @param path root path
	 * @param p handler notified for each file found
	 * @param includes includes only file matching these glob (null or empty for all)
	 * @throws IOException in case of error
	 */
	public static void scan(Path path, FileHandler p, String... includes) throws IOException {
		scan(path, p, Arrays.asList(includes), Collections.emptyList());
	}

	/**
	 * scan folder
	 * @param path root path
	 * @param p handler notified for each file found
	 * @param includes includes only file matching these globs (null or empty for all)
	 * @param excludes exclude all file matching one globs (null or empty to include all)
	 * @throws IOException in case of error
	 */
	public static void scan(String path, FileHandler p, Collection<String> includes, Collection<String> excludes) throws IOException {
		scan(Paths.get(path), p, PathMatchers.includes(includes, excludes));
	}

	/**
	 * scan folder
	 * @param path root path
	 * @param p handler notified for each file found
	 * @param includes includes only file matching these globs (null or empty for all)
	 * @param excludes exclude all file matching one globs (null or empty to include all)
	 * @throws IOException in case of error
	 */
	public static void scan(Path path, FileHandler p, Collection<String> includes, Collection<String> excludes) throws IOException {
		scan(path, p, PathMatchers.includes(includes, excludes));
	}

	/**
	 * scan folder
	 * @param path root path
	 * @param p handler notified for each file found
	 * @param matcher includes only file matching these globs (null or empty for all)
	 * @throws IOException in case of error
	 */
	public static void scan(Path path, FileHandler p, PathMatcher matcher) throws IOException {
		if (path == null || p == null || matcher == null)
			throw new NullPointerException();
		if (!Files.isDirectory(path))
			return;
		Files.walkFileTree(path, new Visitor(path, p, matcher));
	}

	private static final class Visitor extends SimpleFileVisitor<Path> {
		private final Path root;
		private final FileHandler h;
		private final PathMatcher matcher;

		Visitor(Path root, FileHandler h, PathMatcher matcher) {
			this.root = root;
			this.h = h;
			this.matcher = matcher;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (!file.startsWith(root))
				return FileVisitResult.CONTINUE;
			Path relative = root.relativize(file);
			if (matcher.matches(relative) && !h.handle(file, relative, attrs))
				return FileVisitResult.TERMINATE;
			return FileVisitResult.CONTINUE;
		}

	}

	public interface FileHandler {
		/**
		 * handle a file
		 * @param file file found
		 * @param relative file relative to root
		 * @param attrs attribute of the file
		 * @return false to stop the scanning
		 * @throws IOException in case of error
		 */
		boolean handle(Path file, Path relative, BasicFileAttributes attrs) throws IOException;
	}
}
