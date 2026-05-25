package unknow.maven.codegen;

import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

public class CodeGenConfig {
	/**
	 * package of generated class
	 */
	@Parameter(name = "packageName")
	public String packageName;

	/**
	 * folder for generated source file (default to "target/"+pluginGroupId + "." + pluginArtifactId + "/" + executionId "/src" ) 
	 */
	@Parameter(name = "sources")
	public String sources;

	/**
	 * folder for generated resources (default to "target/"+pluginGroupId + "." + pluginArtifactId + "/" + executionId "/resources" ) 
	 */
	@Parameter(name = "resources")
	public String resources;

	/**
	 * list of artifact to scan
	 */
	@Parameter(name = "artifacts")
	public List<String> artifacts;

	/**
	 * formating of generated sources
	 */
	@Parameter(name = "formatting")
	public SourceFormat formatting = new SourceFormat();

	/**
	 * if true generated file for graalvm native-image
	 */
	@Parameter(defaultValue = "true")
	public boolean graalvm;
}
