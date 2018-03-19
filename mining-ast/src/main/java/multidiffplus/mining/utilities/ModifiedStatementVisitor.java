package multidiffplus.mining.utilities;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Loop;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;

/**
 * Use to determine whether or node a statement inside a block has structural changes.
 */
public class ModifiedStatementVisitor implements NodeVisitor {
	
	AstNode root;
	int numModified;
	
	/**
	 * @param node The structure which contains the statements to inspect (a function or try/catch/finally block).
	 */
	public static boolean hasStructuralModifications(AstNode root) {
		ModifiedStatementVisitor visitor = new ModifiedStatementVisitor(root);
		root.visit(visitor);
		return visitor.numModified > 0;
	}

	private ModifiedStatementVisitor(AstNode root) {
		this.root = root;
		this.numModified = 0;
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
			if(node != root) return false;
			if(isModified(node)) numModified++;
			((TryStatement)node).getTryBlock().visit(this);;
			return false;
		case Token.CATCH:
			if(node != root) return false;
			if(isModified(node)) numModified++;
			((CatchClause)node).getBody().visit(this);
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
