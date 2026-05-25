/**
 * 
 */
package unknow.maven.codegen;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;

/**
 * write compilation unit 
 * @author unknow
 */
public class CompilationUnitWriter {
	private final Path baseDir;
	private final PrinterConfiguration pp;

	public CompilationUnitWriter(String baseDir, PrinterConfiguration pp) {
		this(Paths.get(baseDir), pp);
	}

	public CompilationUnitWriter(Path baseDir, PrinterConfiguration pp) {
		this.baseDir = baseDir;
		this.pp = pp;
	}

	/**
	 * save a compilation unit, create the folder that match the package and use the first public type (or first type is no public type are found) for the file name
	 * @param cu the compilation unit
	 * @throws MojoExecutionException in case of error
	 */
	public void write(CompilationUnit cu) throws MojoExecutionException {
		String name = cu.findFirst(TypeDeclaration.class, c -> c.isPublic()).map(c -> c.getNameAsString()).orElse(null);
		if (name == null)
			name = cu.findFirst(TypeDeclaration.class).map(c -> c.getNameAsString()).orElse(null);
		if (name == null)
			throw new MojoExecutionException("not type in unit" + cu);

		Path dir = cu.getPackageDeclaration().map(p -> baseDir.resolve(p.getNameAsString().replace('.', '/'))).orElse(baseDir);
		try {
			Files.createDirectories(dir);
			try (BufferedWriter w = Files.newBufferedWriter(dir.resolve(name + ".java"), StandardCharsets.UTF_8)) {
				w.write(cu.toString(pp));
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
	}
}
