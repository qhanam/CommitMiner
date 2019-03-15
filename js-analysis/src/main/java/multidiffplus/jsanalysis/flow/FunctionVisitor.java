package multidiffplus.jsanalysis.flow;

import java.util.ArrayList;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.NodeVisitor;

/**
 * Generates a list of all the functions in an AST.
 */
public class FunctionVisitor implements NodeVisitor {

    ArrayList<FunctionNode> functionNodes;

    public FunctionVisitor() {
	this.functionNodes = new ArrayList<FunctionNode>();
    }

    public static ArrayList<FunctionNode> getFunctions(AstNode node) {
	FunctionVisitor visitor = new FunctionVisitor();
	node.visit(visitor);
	return visitor.functionNodes;
    }

    @Override
    public boolean visit(AstNode node) {
	if (node instanceof FunctionNode) {
	    this.functionNodes.add((FunctionNode) node);
	}
	return true;
    }

}