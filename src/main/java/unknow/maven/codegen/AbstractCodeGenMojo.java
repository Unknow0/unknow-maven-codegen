package unknow.maven.codegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;

import unknow.maven.codegen.FileScanner.FileHandler;
import unknow.model.api.ModelLoader;
import unknow.model.api.TypeModel;
import unknow.model.ast.AstModelLoader;
import unknow.model.jvm.JvmModelLoader;

/**
 * base class to get 
 * @author unknow
 */
public abstract class AbstractCodeGenMojo extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(AbstractCodeGenMojo.class);

	private static final PathMatcher JAVA = p -> p.getFileName().endsWith(".java");

	private Path lastSucessfulBuild;

	private Collection<Artifact> artifacts;

	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	protected MavenSession session;
	@Parameter(defaultValue = "${mojo}", required = true, readonly = true)
	protected MojoExecution mojo;
	@Component
	protected RepositorySystem repository;
	@Parameter
	protected CodeGenConfig codegen;

	/** writer to config.sources folder */
	protected CompilationUnitWriter writer;
	/** java parser with symbol resolver on sources and runtime classpath */
	protected JavaParser parser;
	/** symbol resolver on sources and runtime classpath */
	protected JavaSymbolSolver javaSymbolSolver;

	/**
	 * existing public class in output package (simpleName-&gt;fqn).<br>
	 * populated with processSrc
	 */
	protected final Map<String, String> existingClass = new HashMap<>();

	/** all class in src (fqn to classDef), filled by processSrc */
	protected final Map<String, TypeDeclaration<?>> classes = new HashMap<>();
	/** all package in src, filled by processSrc */
	protected final Map<String, PackageDeclaration> packages = new HashMap<>();

	/** modelLoader on source file and artifacts */
	protected ModelLoader loader;

	/** the runtime class loader */
	protected ClassLoader classLoader;

	protected MavenProject project;

	protected String uniquePath;

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		init();
		if (!changed()) {
			logger.info("no change skipping");
			return;
		}
		doexecute();
		try {
			Files.createDirectories(lastSucessfulBuild.getParent());
			Files.writeString(lastSucessfulBuild, Instant.now().toString(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.warn("Failed to update last sucess build", e);
		}
	}

	protected abstract void doexecute() throws MojoExecutionException, MojoFailureException;

	/**
	 * resolved artifacts from codegen.artifacts or empty list if none
	 * @return resolved artifacts to process
	 * @throws MojoFailureException in case of resolving issue
	 */
	protected Collection<Artifact> artifacts() throws MojoFailureException {
		if (artifacts != null)
			return artifacts;
		if (codegen.artifacts == null)
			return artifacts = Collections.emptyList();
		artifacts = new ArrayList<>(codegen.artifacts.size());
		RepositorySystemSession repositorySession = session.getRepositorySession();
		for (String id : codegen.artifacts) {
			try {
				ArtifactRequest r = new ArtifactRequest().setArtifact(new DefaultArtifact(id));
				Artifact a = repository.resolveArtifact(repositorySession, r).getArtifact();
				if (a == null)
					throw new MojoFailureException("Failed to resolv artifact " + id);
				artifacts.add(a);
			} catch (ArtifactResolutionException e) {
				throw new MojoFailureException(e);
			}
		}
		return artifacts;
	}

	/**
	 * initialize internal fields
	 */
	private void init() {
		this.project = session.getCurrentProject();
		PluginDescriptor plugin = mojo.getMojoDescriptor().getPluginDescriptor();
		this.uniquePath = plugin.getGroupId() + "." + plugin.getArtifactId() + "/" + mojo.getGoal() + "-" + mojo.getExecutionId();

		String baseDir = project.getBuild().getDirectory() + "/" + uniquePath;
		this.lastSucessfulBuild = Paths.get(baseDir + "/last-successful-build");

		classLoader = getClassLoader();
		loader = ModelLoader.from(JvmModelLoader.GLOBAL, new AstModelLoader(classes, packages), new JvmModelLoader(classLoader));

		List<String> compileSourceRoots = project.getCompileSourceRoots();

		List<TypeSolver> solver = new ArrayList<>(compileSourceRoots.size() + 1);
		solver.add(new ClassLoaderTypeSolver(classLoader));
		for (String s : compileSourceRoots) {
			if (Files.isDirectory(Paths.get(s)))
				solver.add(new JavaParserTypeSolver(s));
		}
		javaSymbolSolver = new JavaSymbolSolver(new CombinedTypeSolver(solver));
		parser = new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.RAW).setStoreTokens(true).setSymbolResolver(javaSymbolSolver));

		if (this.codegen.sources == null)
			this.codegen.sources = baseDir + "/src";
		project.addCompileSourceRoot(this.codegen.sources);

		if (this.codegen.resources == null)
			this.codegen.resources = baseDir + "/resources";
		addResource(this.codegen.resources);

		writer = new CompilationUnitWriter(this.codegen.sources, this.codegen.formatting.toPrinterConfiguration());
	}

	/**
	 * @return true if the source/resources/pom have changed since the last build
	 * @throws MojoFailureException in case of error
	 */
	private boolean changed() throws MojoFailureException {
		if (!codegen.incremental) {
			logger.info("Not incremental");
			return true;
		}
		if (Files.notExists(lastSucessfulBuild)) {
			logger.info("missing lastSucessfulBuild file");
			return true;
		}
		long last;
		try {
			last = Files.getLastModifiedTime(lastSucessfulBuild).toMillis();
		} catch (IOException e) {
			logger.info("Faile to get lastSucessfullBuild timestamp", e);
			return true;
		}
		logger.info("previous build: {}", Instant.ofEpochMilli(last));
		MavenProject p = project;
		while (p != null) {
			long lastModified = p.getFile().lastModified();
			if (lastModified > last) {
				logger.info("Pom {}:{}:{} changed at {}", p.getGroupId(), p.getArtifactId(), p.getVersion(), Instant.ofEpochMilli(lastModified));
				return true;
			}
			p = p.getParent();
		}

		for (Artifact a : artifacts()) {
			long lastModified = a.getFile().lastModified();
			if (lastModified > last) {
				logger.info("Artifact {}:{}:{} changed at {}", a.getGroupId(), a.getArtifactId(), a.getVersion(), Instant.ofEpochMilli(lastModified));
				return true;
			}
		}

		ChangeWalker w = new ChangeWalker(last);
		for (String s : project.getCompileSourceRoots()) {
			if (w.hasChanged(s, JAVA))
				return true;
		}
		for (Resource r : project.getResources()) {
			List<PathMatcher> includes = r.getIncludes().stream().map(s -> FileScanner.globSystem(s)).collect(Collectors.toList());
			List<PathMatcher> excludes = r.getExcludes().stream().map(s -> FileScanner.globSystem(s)).collect(Collectors.toList());
			if (w.hasChanged(r.getDirectory(), includes, excludes))
				return true;
		}

		return false;
	}

	/**
	 * create a new compilation unit with the right package and symbol resolver set
	 * @return a newly created compilationUnit
	 */
	public CompilationUnit newCu() {
		String p = codegen.packageName;
		CompilationUnit cu = StringUtils.isBlank(p) ? new CompilationUnit() : new CompilationUnit(p);
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);
		return cu;
	}

	/**
	 * process sources folders
	 * @param c accept loaded sources
	 * @throws MojoExecutionException in case of error
	 * @throws MojoFailureException in case of error
	 */
	protected void processSrc(TypeConsumer c) throws MojoExecutionException, MojoFailureException {
		SrcWalker w = new SrcWalker();
		for (String s : project.getCompileSourceRoots())
			w.walk(s);
		for (String q : classes.keySet())
			c.accept(loader.get(q));

		if (codegen.artifacts == null || codegen.artifacts.isEmpty())
			return;
		for (Artifact a : artifacts())
			parseArtifact(a, c);
	}

	/**
	 * process all the resources folder
	 * @param c consumer of fullPath, relativePath
	 */
	protected void processResources(BiConsumer<Path, Path> c) {
		DirectoryWalker scanner = new DirectoryWalker();
		L l = new L(c);
		scanner.addDirectoryWalkListener(l);
		for (Resource r : project.getResources()) {
			l.root = Paths.get(r.getDirectory());
			if (!Files.exists(l.root))
				continue;
			scanner.setBaseDir(l.root.toFile());
			scanner.setIncludes(r.getIncludes());
			scanner.setExcludes(r.getExcludes());
			scanner.scan();
		}
	}

	protected String fullName(String simpleName) {
		if (StringUtils.isBlank(codegen.packageName))
			return simpleName;
		return codegen.packageName + "." + simpleName;
	}

	/**
	 * get project runtime class path
	 * @return the runtime classpath
	 */
	private ClassLoader getClassLoader() {
		try {
			List<String> classpathElements = project.getRuntimeClasspathElements();
			URL[] urls = new URL[classpathElements.size()];

			for (int i = 0; i < urls.length; i++)
				urls[i] = new File(classpathElements.get(i)).toURI().toURL();
			return new URLClassLoader(urls, getClass().getClassLoader());
		} catch (DependencyResolutionRequiredException | MalformedURLException e) {
			logger.error("Failed to get project classpath", e);
			return getClass().getClassLoader();
		}
	}

	private void parseArtifact(Artifact a, TypeConsumer c) throws MojoExecutionException, MojoFailureException {
		try (FileInputStream is = new FileInputStream(a.getFile()); ZipInputStream zip = new ZipInputStream(is)) {
			ZipEntry e;
			while ((e = zip.getNextEntry()) != null) {
				String name = e.getName();
				if (!name.endsWith(".class"))
					continue;
				c.accept(loader.get(name.substring(0, name.length() - 6).replaceAll("[/$]", ".")));
			}
		} catch (IOException e) {
			throw new MojoFailureException("Failed to process artifact " + a.getFile(), e);
		}
	}

	private void addResource(String resources) {
		for (Resource e : project.getResources()) {
			if (resources.equals(e.getDirectory()))
				return;
		}
		Resource resource = new Resource();
		resource.setDirectory(resources);
		project.addResource(resource);

		try {
			Files.createDirectories(Paths.get(resources));
		} catch (@SuppressWarnings("unused") IOException e) { // ignore
		}
	}

	private class SrcWalker implements FileHandler {
		private final String[] part;
		private Path local;
		private int count;
		private Exception ex;
		private String packageName;

		public SrcWalker() {
			this.packageName = codegen.packageName;
			this.part = packageName == null ? new String[0] : packageName.split("\\.");
		}

		public void walk(String s) throws MojoFailureException, MojoExecutionException {
			Path path = Paths.get(s);
			if (!Files.isDirectory(path))
				return;

			local = Paths.get(s, part);
			count = local.getNameCount();
			ex = null;
			try {
				FileScanner.scan(path, this, JAVA);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to process source " + s, e);
			}
			if (ex instanceof MojoExecutionException)
				throw (MojoExecutionException) ex;
			if (ex instanceof MojoFailureException)
				throw (MojoFailureException) ex;
			if (ex != null)
				throw new MojoExecutionException("Failed to process source " + s, ex);
		}

		@Override
		public boolean handle(Path file, Path relative, BasicFileAttributes attrs) throws IOException {
			ParseResult<CompilationUnit> parse = parser.parse(file);

			if (!parse.isSuccessful()) {
				ex = new MojoExecutionException("Failed to parse " + file + ": " + parse.getProblems());
				return false;
			}
			CompilationUnit cu = parse.getResult().orElse(null);
			if (cu == null)
				return true;

			cu.getPackageDeclaration().filter(v -> v.getAnnotations() != null).ifPresent(v -> packages.put(v.getNameAsString(), v));
			for (TypeDeclaration<?> v : cu.findAll(TypeDeclaration.class)) {
				String qualifiedName = v.resolve().getQualifiedName();
				classes.put(qualifiedName, v);
			}
			if (count == file.getNameCount() && file.startsWith(local)) {
				String string = file.getFileName().toString();
				string = string.substring(0, string.length() - 5);
				existingClass.put(string, packageName + "." + string);
			}
			return true;
		}
	}

	private static class L implements DirectoryWalkListener {
		final BiConsumer<Path, Path> c;
		Path root;

		L(BiConsumer<Path, Path> c) {
			this.c = c;
		}

		@Override
		public void directoryWalkStep(int percentage, File file) {
			Path path = file.toPath();
			c.accept(path, root.relativize(path));
		}

		@Override
		public void directoryWalkStarting(File basedir) { // ok
		}

		@Override
		public void directoryWalkFinished() { // ok
		}

		@Override
		public void debug(String message) { // ok
		}
	}

	public static interface TypeConsumer {
		void accept(TypeModel t) throws MojoExecutionException, MojoFailureException;
	}
}
