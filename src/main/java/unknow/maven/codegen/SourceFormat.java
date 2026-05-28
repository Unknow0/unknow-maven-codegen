package unknow.maven.codegen;

import java.util.Arrays;
import java.util.List;

import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.PrinterConfiguration;

/**
 * java source formatting configuration.
 */
public class SourceFormat {

	/**
	 * Order imports alphabetically.
	 */
	private boolean orderImports = true;

	/**
	 * The list of package prefix to group together to use (default ["java.", "javax.", "org.", "com."])
	 */
	private List<String> orderImportGroups = Arrays.asList("java.", "javax.", "org.", "com.");

	/**
	 * Print comments. It can be combined with {@code PRINT_JAVADOC} to print regular comments and javadoc.
	 */
	private boolean printComments = true;

	/**
	 * Print javadoc comments. It can be combined with {@code PRINT_COMMENTS} to print regular javadoc and comments.
	 */
	private boolean printJavadoc = true;

	/**
	 * Insert spaces around operators.
	 */
	private boolean spaceAroundOperators = true;

	/**
	 * Align method parameters in columns.
	 */
	private boolean columnAlignParameters = false;

	/**
	 * Align the first element of a method chain.
	 */
	private boolean columnAlignFirstMethodChain = true;

	/**
	 * Indent the case when it is true, don't if false
	 * <pre>{@code
	 * switch(x) {            switch(x) {
	 *    case 1:             case 1:
	 *        return y;           return y;
	 *    case 2:             case 2:
	 *        return z;           return x;
	 * }                       }
	 * }<pre>
	 */
	private boolean indentCaseInSwitch = true;

	/**
	* By default enum constants get aligned like this:
	* <pre>{@code
	*     enum X {
	*        A, B, C, D
	*     }
	* }<pre>
	* until the amount of constants passes this currentValue (5 by default).
	* Then they get aligned like this:
	* <pre>{@code
	*     enum X {
	*        A,
	*        B,
	*        C,
	*        D,
	*        E,
	*        F,
	*        G
	*     }
	* }</pre>
	* Set it to a very large number (e.g. {@code Integer.MAX_VALUE} to always align horizontally.
	* Set it to 1 or less to always align vertically.
	*/
	private int maxEnumConstantsToAlignHorizontally = 5;

	/**
	 * The end-of-line character used when printing code.  Default is system-dependent.
	 */
	private String endOfLineCharacter = System.getProperty("line.separator");

	/**
	 * Indentation property.
	 */
	private Indentation indentation = new Indentation();

	public boolean isOrderImports() {
		return orderImports;
	}

	public void setOrderImports(boolean orderImports) {
		this.orderImports = orderImports;
	}

	public List<String> getOrderImportGroups() {
		return orderImportGroups;
	}

	public void setOrderImportGroups(List<String> orderImportGroups) {
		this.orderImportGroups = orderImportGroups;
	}

	public boolean isPrintComments() {
		return printComments;
	}

	public void setPrintComments(boolean printComments) {
		this.printComments = printComments;
	}

	public boolean isPrintJavadoc() {
		return printJavadoc;
	}

	public void setPrintJavadoc(boolean printJavadoc) {
		this.printJavadoc = printJavadoc;
	}

	public boolean isSpaceAroundOperators() {
		return spaceAroundOperators;
	}

	public void setSpaceAroundOperators(boolean spaceAroundOperators) {
		this.spaceAroundOperators = spaceAroundOperators;
	}

	public boolean isColumnAlignParameters() {
		return columnAlignParameters;
	}

	public void setColumnAlignParameters(boolean columnAlignParameters) {
		this.columnAlignParameters = columnAlignParameters;
	}

	public boolean isColumnAlignFirstMethodChain() {
		return columnAlignFirstMethodChain;
	}

	public void setColumnAlignFirstMethodChain(boolean columnAlignFirstMethodChain) {
		this.columnAlignFirstMethodChain = columnAlignFirstMethodChain;
	}

	public boolean isIndentCaseInSwitch() {
		return indentCaseInSwitch;
	}

	public void setIndentCaseInSwitch(boolean indentCaseInSwitch) {
		this.indentCaseInSwitch = indentCaseInSwitch;
	}

	public int getMaxEnumConstantsToAlignHorizontally() {
		return maxEnumConstantsToAlignHorizontally;
	}

	public void setMaxEnumConstantsToAlignHorizontally(int maxEnumConstantsToAlignHorizontally) {
		this.maxEnumConstantsToAlignHorizontally = maxEnumConstantsToAlignHorizontally;
	}

	public String getEndOfLineCharacter() {
		return endOfLineCharacter;
	}

	public void setEndOfLineCharacter(String endOfLineCharacter) {
		this.endOfLineCharacter = endOfLineCharacter;
	}

	public Indentation getIndentation() {
		return indentation;
	}

	public void setIndentation(Indentation indentation) {
		this.indentation = indentation;
	}

	public PrinterConfiguration toPrinterConfiguration() {
		PrinterConfiguration config = new DefaultPrinterConfiguration().addOption(new DefaultConfigurationOption(ConfigOption.ORDER_IMPORTS, orderImports))
				.addOption(new DefaultConfigurationOption(ConfigOption.SORT_IMPORTS_STRATEGY, new ImportGroupsOrdering(orderImportGroups)))
				.addOption(new DefaultConfigurationOption(ConfigOption.MAX_ENUM_CONSTANTS_TO_ALIGN_HORIZONTALLY, maxEnumConstantsToAlignHorizontally))
				.addOption(new DefaultConfigurationOption(ConfigOption.END_OF_LINE_CHARACTER, endOfLineCharacter))
				.addOption(new DefaultConfigurationOption(ConfigOption.INDENTATION, indentation.toIndentation()));
		if (indentCaseInSwitch)
			config.addOption(new DefaultConfigurationOption(ConfigOption.INDENT_CASE_IN_SWITCH));
		if (spaceAroundOperators)
			config.addOption(new DefaultConfigurationOption(ConfigOption.SPACE_AROUND_OPERATORS));
		if (columnAlignFirstMethodChain)
			config.addOption(new DefaultConfigurationOption(ConfigOption.COLUMN_ALIGN_FIRST_METHOD_CHAIN));
		if (printComments)
			config.addOption(new DefaultConfigurationOption(ConfigOption.PRINT_COMMENTS));
		if (printJavadoc)
			config.addOption(new DefaultConfigurationOption(ConfigOption.PRINT_JAVADOC));
		if (columnAlignParameters)
			config.addOption(new DefaultConfigurationOption(ConfigOption.COLUMN_ALIGN_PARAMETERS));
		return config;

	}
}
