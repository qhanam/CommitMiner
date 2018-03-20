package multidiffplus.mining.ast.analysis.unhandledex;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.Loop;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;

/**
 * Use to count the number of statements which were modified without recursively exploring functions or try statements.
 */
public class ModifiedFunctionVisitor implements NodeVisitor {
	
	AstNode root;
	int numModified;
	boolean containsTry;
	
	/**
	 * @param node The structure which contains the statements to inspect (a function or try/catch/finally block).
	 * @return {@code true} if one or more non-function-nested statements have been modified.
	 */
	public static boolean hasStructuralModifications(AstNode root) {
		ModifiedFunctionVisitor visitor = new ModifiedFunctionVisitor(root);
		root.visit(visitor);
		return visitor.numModified > 0 && !visitor.containsTry;
	}

	private ModifiedFunctionVisitor(AstNode root) {
		this.root = root;
		this.numModified = 0;
		this.containsTry = false;
	} 

	@Override
	public boolean visit(AstNode node) {
		
		switch(node.getType()) {
		case Token.FUNCTION:
			if(node != root) return false;
			if(isModified(node)) numModified++;
			((FunctionNode)node).getBody().visit(this);
			return false;
		case Token.TRY:
			/* Avoid reporting this function if the try statement is a mutation
			* candidate. */
			if(ModifiedTryVisitor.hasStructuralModifications(node))
				containsTry = true;
			if(isModified(node)) numModified++;
			((TryStatement)node).getTryBlock().visit(this);;
			return false;
		case Token.IF:
			if(isModified(node)) numModified++;
			((IfStatement)node).getThenPart().visit(this);
			if(((IfStatement)node).getElsePart() != null)
				((IfStatement)node).getElsePart().visit(this);
			return false;
		case Token.FOR:
			if(isModified(node)) numModified++;
			((Loop)node).getBody().visit(this);
			return false;
		case Token.WHILE:
			if(isModified(node)) numModified++;
			((WhileLoop)node).getBody().visit(this);
			return false;
		case Token.WITH:
			if(isModified(node)) numModified++;
			((WithStatement)node).getEnclosingScope().visit(this);
			return false;
		case Token.BLOCK:
			return true;
		}
		
		if(isModified(node)) numModified++;
		
		/* We only want to look at statements, not expressions. */
		return false;

	}
	
	/**
	 * @return {@code true} if the statement is modified.
	 */
	private static boolean isModified(AstNode node) {
		return node.getChangeType() != ChangeType.UNCHANGED;
	}

}
