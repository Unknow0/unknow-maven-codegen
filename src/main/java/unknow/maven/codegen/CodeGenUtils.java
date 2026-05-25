/**
 * 
 */
package unknow.maven.codegen;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

/**
 * @author unknow
 */
public class CodeGenUtils {
	public static final Modifier.Keyword[] PUBLIC = { Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL };
	public static final Modifier.Keyword[] PUBLIC_STATIC = { Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL };
	public static final Modifier.Keyword[] PROTECT = { Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL };
	public static final Modifier.Keyword[] PSF = { Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL };
	public static final Modifier.Keyword[] PRIVATE = { Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL };

	private CodeGenUtils() {
	}

	public static StringLiteralExpr text(String s) {
		return new StringLiteralExpr(s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\u0000", "\\u0000"));
	}

	/**
	 * create a node list
	 * 
	 * @param <T> type of element
	 * @param t the values
	 * @return the node list
	 */
	@SafeVarargs
	public static <T extends Node> NodeList<T> list(T... t) {
		return new NodeList<>(t);
	}

	/**
	 * new byte[]{values}
	 * 
	 * @param b the values
	 * @return the byte array creation
	 */
	public static Expression byteArray(byte[] b) {
		NodeList<Expression> nodeList = new NodeList<>();
		for (int i = 0; i < b.length; i++)
			nodeList.add(new IntegerLiteralExpr(Byte.toString(b[i])));

		return array(PrimitiveType.byteType(), nodeList);
	}

	/**
	 * @param values the values
	 * @return new string[] {values}
	 */
	public static ArrayCreationExpr stringArray(String[] values) {
		NodeList<Expression> nodeList = new NodeList<>();
		for (int i = 0; i < values.length; i++)
			nodeList.add(text(values[i]));

		return array(new ClassOrInterfaceType(null, "String"), nodeList);
	}

	public static ArrayCreationExpr array(Type type, int... level) {
		NodeList<ArrayCreationLevel> l = new NodeList<>();
		for (int i = 0; i < level.length; i++)
			l.add(new ArrayCreationLevel(level[i]));
		return new ArrayCreationExpr(type, l, null);
	}

	/**
	 * create a new array
	 * 
	 * @param type the element types
	 * @param init the values
	 * @return the array creation
	 */
	public static ArrayCreationExpr array(Type type, NodeList<Expression> init) {
		return new ArrayCreationExpr(type, list(new ArrayCreationLevel()), new ArrayInitializerExpr(init));
	}

	/**
	 * T n = value
	 * 
	 * @param t the variable type
	 * @param n the variable name
	 * @param value the value
	 * @return the assignement
	 */
	public static AssignExpr assign(Type t, String n, Expression value) {
		return new AssignExpr(new VariableDeclarationExpr(t, n), value, Operator.ASSIGN);
	}

	public static AssignExpr create(ClassOrInterfaceType t, String n, NodeList<Expression> arg) {
		return assign(t, n, new ObjectCreationExpr(null, t, arg));
	}

	/**
	 * add all the expression together
	 * 
	 * @param e expression to add
	 * @return e[0] + e[1] +...
	 */
	public static Expression add(Expression... e) {
		if (e.length == 1)
			return e[0];
		BinaryExpr b = new BinaryExpr(e[0], e[1], BinaryExpr.Operator.PLUS);
		for (int i = 2; i < e.length; i++)
			b = new BinaryExpr(b, e[i], BinaryExpr.Operator.PLUS);
		return b;
	}
}
