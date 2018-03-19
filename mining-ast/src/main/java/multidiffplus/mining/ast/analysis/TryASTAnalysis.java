package multidiffplus.mining.ast.analysis;

import java.util.LinkedList;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.TryStatement;

import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.MiningFactBase;
import multidiffplus.mining.utilities.ModifiedStatementVisitor;

/**
 * Search for try statements that are either modified themselves, or contain a
 * statements which are modified.
 */
public class TryASTAnalysis implements NodeVisitor {
	
	/** The root node being visited. **/
	AstNode root;

	/** Register facts here. */
	MiningFactBase factBase;
	
	/**
	 * @param sourceCodeFileChange used to look up the correct dataset for
	 * storing facts.
	 */
	public TryASTAnalysis(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		this.factBase = MiningFactBase.getInstance(sourceCodeFileChange);
		this.root = root;
	}

	@Override
	public boolean visit(AstNode node) {
		
		/* Stop on function declarations & investigate try/catch statements. */
		switch(node.getType()) {
		case Token.FUNCTION:
			if(node != root) return false;
			break;
		case Token.TRY:
			visitTryStatement((TryStatement)node);
			break;
		}
		
		return true;

	}
	
	/**
	 * Register annotations for new try statements that wrap a set of unchanged
	 * (moved) statements.
	 */
	public void visitTryStatement(TryStatement tryStatement) {
		
		/* Has this try statement, or its contents, changed in some way? */
		if(ModifiedStatementVisitor.hasStructuralModifications(tryStatement)) {
			
			/* Since the try statement or its contents has changed, it is
			* interesting to us. Register a fact so the file pair can be stored
			* for a more detailed analysis later. */

			Annotation annotation = new Annotation("TRY", 
					new LinkedList<DependencyIdentifier>(), 
					tryStatement.getLineno(), 
					tryStatement.getAbsolutePosition(), 
					tryStatement.getLength());
			factBase.registerAnnotationFact(annotation);
			
		}
		
	}

}