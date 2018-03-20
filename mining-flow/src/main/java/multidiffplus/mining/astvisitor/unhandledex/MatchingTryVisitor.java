package multidiffplus.mining.astvisitor.unhandledex;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.TryStatement;

public class MatchingTryVisitor implements NodeVisitor {
	
	public static TryStatement findClone(ScriptNode funct, TryStatement tryStatement) {
		MatchingTryVisitor visitor = new MatchingTryVisitor(funct, tryStatement);
		funct.visit(visitor);
		return visitor.clone;
	}
	
	private ScriptNode root;
	private TryStatement tryStatement;
	private TryStatement clone;
	
	private MatchingTryVisitor(ScriptNode root, TryStatement tryStatement) {
		this.root = root;
		this.tryStatement = tryStatement;
		this.clone = null;
	}

	@Override
	public boolean visit(AstNode node) {
		
		switch(node.getType()) {
		case Token.FUNCTION:
			if(node == root) return true;
			return false;
		case Token.TRY:
			TryStatement current = (TryStatement)node;
			if(current.getID().equals(tryStatement.getID()))
				clone = current;
		default:
			return true;
		}

	}

}
