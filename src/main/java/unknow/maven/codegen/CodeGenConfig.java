package unknow.maven.codegen;

import java.util.List;

public class CodeGenConfig {
	/**
	 * package of generated class
	 */
	public String packageName;

	/**
	 * folder for generated source file (default to "target/"+pluginGroupId + "." + pluginArtifactId + "/" + executionId "/src" ) 
	 */
	public String sources;

	/**
	 * folder for generated resources (default to "target/"+pluginGroupId + "." + pluginArtifactId + "/" + executionId "/resources" ) 
	 */
	public String resources;

	/**
	 * list of artifact to scan
	 */
	public List<String> artifacts;

	/**
	 * formating of generated sources
	 */
	public SourceFormat formatting = new SourceFormat();

	/**
	 * if true generated file for graalvm native-image
	 */
	public boolean graalvm = true;

	/**
	 * rebuild only if something change from the last build
	 */
	public boolean incremental = true;
}
