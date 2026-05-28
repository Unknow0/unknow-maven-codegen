package unknow.maven.codegen;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
import java.util.function.Function;

public final class FileScanner {
	private static final FileSystem FS = FileSystems.getDefault();
	private static final Function<String, String> NORM = "\\".equals(FS.getSeparator()) ? s -> s.replace('/', '\\') : s -> s.replace('\\', '/');

	private FileScanner() {
	}

	public static PathMatcher globSystem(String glob) {
		return FS.getPathMatcher("glob:" + NORM.apply(glob));
	}

	public static void scan(String path, FileHandler p, PathMatcher... includes) throws IOException {
		scan(Paths.get(path), p, Arrays.asList(includes), Collections.emptyList());
	}

	public static void scan(Path path, FileHandler p, PathMatcher... includes) throws IOException {
		scan(path, p, Arrays.asList(includes), Collections.emptyList());
	}

	public static void scan(Path path, FileHandler p, Collection<PathMatcher> includes, Collection<PathMatcher> excludes) throws IOException {
		if (path == null || p == null || includes == null || excludes == null)
			throw new NullPointerException();
		if (!Files.isDirectory(path))
			return;
		Files.walkFileTree(path, new Visitor(path, p, includes, excludes));
	}

	private static final class Visitor extends SimpleFileVisitor<Path> {
		private final Path root;
		private final FileHandler p;
		private final Collection<PathMatcher> includes;
		private final Collection<PathMatcher> excludes;

		Visitor(Path root, FileHandler p, Collection<PathMatcher> includes, Collection<PathMatcher> excludes) {
			this.root = root;
			this.p = p;
			this.includes = includes;
			this.excludes = excludes;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (!file.startsWith(root))
				return FileVisitResult.CONTINUE;
			Path relative = root.relativize(file);
			if (accept(relative) && !p.handle(file, relative, attrs))
				return FileVisitResult.TERMINATE;
			return FileVisitResult.CONTINUE;
		}

		private boolean accept(Path file) {
			for (PathMatcher p : excludes) {
				if (p.matches(file))
					return false;
			}
			if (includes.isEmpty())
				return true;
			for (PathMatcher p : includes) {
				if (p.matches(file))
					return true;
			}
			return false;
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
