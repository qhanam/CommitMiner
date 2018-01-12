package changeimpact;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.transfer.ExpEval;

/**
 * Extract a definition and its dependencies from the statement.
 */
public class DependencyVisitor implements NodeVisitor {
	
	private ExpEval evaluator;
	private Map<String, Change> dependencies;

	/**
	 * @param node The return value or the RHS of an assignment.
	 * @return the data dependencies
	 */
	public static Map<String, Change> getDependencies(ExpEval evaluator, AstNode node) {
		if(node == null) return new HashMap<String, Change>();
		DependencyVisitor visitor = new DependencyVisitor(evaluator);
		node.visit(visitor);
		return visitor.dependencies;
	}
	
	private DependencyVisitor(ExpEval evaluator) {
		this.dependencies = new HashMap<String, Change>();
		this.evaluator = evaluator;
	}

	@Override
	public boolean visit(AstNode node) {

		switch(node.getType()) {
		case Token.CALL:
			FunctionCall call = (FunctionCall)node;
			for(AstNode arg : call.getArguments())
				arg.visit(this);
			return false;
		case Token.NAME:
			dependencies.put(node.toSource(), evaluator.eval(node).change);
		}
		
		return true;

	}
	
}
