package multidiffplus.jsanalysis.hoisting;

import java.util.LinkedList;
import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ScriptNode;

/**
 * Helper class to discover variable declarations to be lifted.
 */
public class FunctionLiftVisitor implements NodeVisitor {

	/**
	 * @return The list of parameters and variables declared in the script or
	 * 		   function.
	 */
	public static List<FunctionNode> getFunctionDeclarations(ScriptNode script) {
		FunctionLiftVisitor visitor = new FunctionLiftVisitor(script);
		script.visit(visitor);
		return visitor.functionDeclarations;
	}

	/** The function or script under analysis. **/
	private ScriptNode script;

	/** The list of variables to be lifted. **/
	private List<FunctionNode> functionDeclarations;

	private FunctionLiftVisitor(ScriptNode script) {
		this.script = script;
		this.functionDeclarations = new LinkedList<FunctionNode>();
	}

	@Override
	public boolean visit(AstNode node) {

		/* Ignore if this is the function we are analyzing. */
		if(node instanceof ScriptNode && node == this.script) {
			return true;
		}
		/* Capture function statements. */
		else if(node instanceof FunctionNode) {
			FunctionNode function = (FunctionNode) node;
			if(function.getFunctionType() == FunctionNode.FUNCTION_STATEMENT) {
				this.functionDeclarations.add(function);
			}
			return false;
		}

		return true;

	}

}