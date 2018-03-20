package multidiffplus.mining.astvisitor.unhandledex;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.TryStatement;

/**
 * Mutates a function definition to extract the contents of a try block outside
 * of the try statement.
 * 
 * Example:
 * 
 * function() {
 * 	try { 
 * 		date = t.date['#text']
 * 	} catch (err) { 
 * 		t.formatted_date = 'Now Playing';
 * 		return true;
 * 	}
 * 
 * is mutated to
 * 
 * function() {
 * 	date = t.date['#text']
 * }
 * 
 */
public class MutateTry {
	
	private TryStatement tryStmt;
	
	/**
	 * @param tryStmt The try statement protecting an exception.
	 */
	public MutateTry(TryStatement tryStmt) {
		this.tryStmt = tryStmt;
	}

	/**
	 * @return The parent function without the try statement.
	 */
	public ScriptNode mutate() {
		
		ScriptNode original = getEnclosingFunction(tryStmt);
		
		/* Clone the function so we don't modify the original. */
		ScriptNode functClone = (ScriptNode)original.clone(original.getParent());
		
		/* Find the try block in the cloned function. */
		TryStatement tryClone = MatchingTryVisitor.findClone(functClone, tryStmt);
		
		/* Pull up the contents of the try block. */
		AstNode tryParent = tryClone.getParent();
		tryParent.addChildBefore(tryClone.getTryBlock(), tryClone);
		tryParent.removeChild(tryClone);
		
		return functClone;
		
	}
	
	/**
	 * @return the enclosing function (or script) of the try statement.
	 */
	private static ScriptNode getEnclosingFunction(TryStatement tryStatement) {
		
		AstNode ancestor = tryStatement.getParent();
		
		do {
			switch(ancestor.getType()) {
			case Token.FUNCTION:
			case Token.SCRIPT:
				return (ScriptNode)ancestor;
			}
			ancestor = ancestor.getParent();
		} while(true);
		
	}

}
