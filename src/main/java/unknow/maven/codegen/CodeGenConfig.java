package unknow.maven.codegen;

import java.util.List;

public class CodeGenConfig {
	/**
	 * package of generated class
	 */
	private String packageName;

	/**
	 * folder for generated source file (default to "target/"+pluginGroupId + "." + pluginArtifactId + "/" + executionId "/src" ) 
	 */
	private String sources;

	/**
	 * folder for generated resources (default to "target/"+pluginGroupId + "." + pluginArtifactId + "/" + executionId "/resources" ) 
	 */
	private String resources;

	/**
	 * list of artifact to scan
	 */
	private List<String> artifacts;

	/**
	 * formating of generated sources
	 */
	private SourceFormat formatting = new SourceFormat();

	/**
	 * if true generated file for graalvm native-image
	 */
	private boolean graalvm = true;

	/**
	 * rebuild only if something change from the last build
	 */
	private boolean incremental = true;

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getSources() {
		return sources;
	}

	public void setSources(String sources) {
		this.sources = sources;
	}

	public String getResources() {
		return resources;
	}

	public void setResources(String resources) {
		this.resources = resources;
	}

	public List<String> getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(List<String> artifacts) {
		this.artifacts = artifacts;
	}

	public SourceFormat getFormatting() {
		return formatting;
	}

	public void setFormatting(SourceFormat formatting) {
		this.formatting = formatting;
	}

	public boolean isGraalvm() {
		return graalvm;
	}

	public void setGraalvm(boolean graalvm) {
		this.graalvm = graalvm;
	}

	public boolean isIncremental() {
		return incremental;
	}

	public void setIncremental(boolean incremental) {
		this.incremental = incremental;
	}
}
