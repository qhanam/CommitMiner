package multidiffplus.jsanalysis.hoisting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.VariableInitializer;

/**
 * Builds a set of the file's undefined globals.
 */
public class GlobalVisitor implements NodeVisitor {
	
	private Set<String> local;
	private Set<String> global;
	
	public static Set<String> getGlobals(ScriptNode script) {
		GlobalVisitor visitor = new GlobalVisitor();
		script.visit(visitor);
		return visitor.global;
	}

	private GlobalVisitor() {
		local = new HashSet<String>();
		global = new HashSet<String>();
	}
	
	private GlobalVisitor(List<AstNode> params) {
		local = new HashSet<String>();
		global = new HashSet<String>();
		for(AstNode param : params) {
			if(param instanceof Name) {
				local.add(param.toSource());
			}
		}
	}
	
	/**
	 * @return The list of undefined globals;
	 */
	public Set<String> getGlobals() {
		return global;
	}

	@Override
	public boolean visit(AstNode node) {
		
		if(node instanceof Name) {
			String name = node.toSource();
			if(!local.contains(name)) global.add(name);
		}
		
		else if(node instanceof VariableInitializer) {
			VariableInitializer vi = (VariableInitializer) node;
			AstNode target = vi.getTarget();
			if(target instanceof Name) {
				global.remove(target.toSource());
				local.add(target.toSource());
			}
			return false;
		}
		
		else if(node instanceof InfixExpression) {
			InfixExpression ie = (InfixExpression) node;
			if(ie.getOperator() == Token.GETPROP) {
				visit(ie.getLeft());
				return false;
			}
		}
		
		else if(node instanceof FunctionNode) {

			FunctionNode fn = (FunctionNode) node;
			
			/* The function name is a local var. */
			Name name = fn.getFunctionName();
			if(name != null) {
				global.remove(name.toSource());
				local.add(name.toSource());
			}
			
			/* Get the list of globals from the function. */
			GlobalVisitor visitor = new GlobalVisitor(fn.getParams());
			fn.getBody().visit(visitor);
			visitor.global.removeAll(local);
			global.addAll(visitor.global);
			return false;

		}

		return true;

	}

}