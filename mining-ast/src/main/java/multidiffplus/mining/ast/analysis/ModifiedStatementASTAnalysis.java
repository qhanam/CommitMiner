package multidiffplus.mining.ast.analysis;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.MiningFactBase;

/**
 * Counts the number of inserted, updated or deleted statements in the file.
 */
public class ModifiedStatementASTAnalysis implements NodeVisitor {
	
	/** The root node being visited. **/
	AstNode root;

	/** Register facts here. */
	MiningFactBase factBase;
	
	/**
	 * @param sourceCodeFileChange used to look up the correct dataset for
	 * storing facts.
	 */
	public ModifiedStatementASTAnalysis(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		this.factBase = MiningFactBase.getInstance(sourceCodeFileChange);
		this.root = root;
	}

	@Override
	public boolean visit(AstNode node) {
		
		/* Stop on function declarations & investigate call sites. */
		switch(node.getType()) {
		case Token.FUNCTION:
			if(node != root) 
				return false;
		default:
			break;
		}
		
		if(node.isStatement() && node.getType() != Token.BLOCK) {
			ChangeType change = node.getChangeType();
			switch(change) {
			case INSERTED:
				factBase.incrementInsertedStatements();
				break;
			case REMOVED:
				factBase.incrementRemovedStatements();
			case UPDATED:
				factBase.incrementUpdatedStatements();
				break;
			default:	
				break;
			}
			
		}
		
		/* Recursively search for AST analysis. */
		return true;

	}
	
}
