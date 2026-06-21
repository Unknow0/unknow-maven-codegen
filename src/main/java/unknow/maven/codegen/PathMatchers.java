package unknow.maven.codegen;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public final class PathMatchers {
	private static final PathMatcher[] EMPTY = {};
	private static final FileSystem FS = FileSystems.getDefault();
	private static final Function<String, String> NORM = "\\".equals(FS.getSeparator()) ? s -> s.replace("/", "\\\\") : s -> s.replace('\\', '/');
	private static final PathMatcher ALL = (path) -> true;
	private static final PathMatcher NONE = (path) -> false;

	private PathMatchers() {
	}

	/**
	 * match all path
	 * @return path matcher that match all
	 */
	public static PathMatcher all() {
		return ALL;
	}

	/**
	 * never match
	 * @return path matcher that match nothing
	 */
	public static PathMatcher none() {
		return NONE;
	}

	/**
	 * path matcher for glob (path separator is '/')
	 * @param glob the glob
	 * @return the glob matcher
	 * @see FileSystem#getPathMatcher
	 */
	public static PathMatcher glob(String glob) {
		if (glob == null)
			throw new NullPointerException("glob can't be null");
		return FS.getPathMatcher("glob:" + NORM.apply(glob));
	}

	/**
	 * convert all globs to PathMatcher, if globs is null return an empty array
	 * @param globs globs so convert
	 * @return the path matchers
	 */
	public static PathMatcher[] globs(Collection<String> globs) {
		if (globs == null || globs.isEmpty())
			return EMPTY;
		PathMatcher[] m = new PathMatcher[globs.size()];
		int i = 0;
		for (String s : globs)
			m[i++] = glob(s);
		return m;
	}

	public static PathMatcher oneof(Collection<PathMatcher> matchers) {
		if (matchers == null)
			throw new NullPointerException("matchers can't be null");
		if (matchers.isEmpty())
			return NONE;
		for (PathMatcher m : matchers) {
			if (m == null)
				throw new NullPointerException("None of the pathMatcher should be null");
		}
		if (matchers.size() == 1)
			return matchers.iterator().next();
		return new OneOf(matchers.toArray(EMPTY));
	}

	public static PathMatcher oneof(PathMatcher... matchers) {
		if (matchers == null)
			throw new NullPointerException("matchers can't be null");
		for (int i = 0; i < matchers.length; i++) {
			if (matchers[i] == null)
				throw new NullPointerException("None of the pathMatcher should be null");
		}
		if (matchers.length == 1)
			return matchers[0];
		return new OneOf(Arrays.copyOf(matchers, matchers.length));
	}

	public static PathMatcher oneofGlob(Collection<String> globs) {
		if (globs == null)
			throw new NullPointerException("matchers can't be null");
		return new OneOf(globs(globs));
	}

	public static PathMatcher noneof(PathMatcher... matchers) {
		if (matchers == null)
			throw new NullPointerException("matchers can't be null");
		for (int i = 0; i < matchers.length; i++) {
			if (matchers[i] == null)
				throw new NullPointerException("None of the pathMatcher should be null");
		}
		return new NoneOf(Arrays.copyOf(matchers, matchers.length));
	}

	public static PathMatcher noneof(Collection<PathMatcher> matchers) {
		if (matchers == null)
			throw new NullPointerException("matchers can't be null");
		for (PathMatcher m : matchers) {
			if (m == null)
				throw new NullPointerException("None of the pathMatcher should be null");
		}
		if (matchers.size() == 1)
			return matchers.iterator().next();
		return new NoneOf(matchers.toArray(EMPTY));
	}

	public static PathMatcher noneofGlob(Collection<String> globs) {
		if (globs == null)
			throw new NullPointerException("matchers can't be null");
		return new NoneOf(globs(globs));
	}

	public static PathMatcher includes(Collection<String> includes, Collection<String> excludes) {
		if (excludes == null || excludes.isEmpty()) {
			if (includes == null || includes.isEmpty())
				return all();
			return new OneOf(globs(includes));
		}
		if (includes == null || includes.isEmpty())
			return new NoneOf(globs(excludes));

		return new IncludeExclude(globs(includes), globs(excludes));
	}

	private static class OneOf implements PathMatcher {
		private final PathMatcher[] m;

		OneOf(PathMatcher[] m) {
			this.m = m;
		}

		@Override
		public boolean matches(Path path) {
			for (int i = 0; i < m.length; i++) {
				if (m[i].matches(path))
					return true;
			}
			return false;
		}
	}

	private static class NoneOf implements PathMatcher {
		private final PathMatcher[] m;

		NoneOf(PathMatcher[] m) {
			this.m = m;
		}

		@Override
		public boolean matches(Path path) {
			for (int i = 0; i < m.length; i++) {
				if (m[i].matches(path))
					return false;
			}
			return true;
		}
	}

	private static class IncludeExclude implements PathMatcher {
		private final PathMatcher[] includes;
		private final PathMatcher[] excludes;

		IncludeExclude(PathMatcher[] includes, PathMatcher[] exclude) {
			this.includes = includes;
			this.excludes = exclude;
		}

		@Override
		public boolean matches(Path path) {
			for (int i = 0; i < excludes.length; i++) {
				if (excludes[i].matches(path))
					return false;
			}
			if (includes.length == 0)
				return true;
			for (int i = 0; i < includes.length; i++) {
				if (includes[i].matches(path))
					return true;
			}
			return false;
		}
	}
}
