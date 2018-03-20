package multidiffplus.mining.ast.analysis.unhandledex;

import java.util.LinkedList;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.TryStatement;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.MiningFactBase;

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
			visitFunctionNode((FunctionNode) node);
			break;
		case Token.TRY:
			visitTryStatement((TryStatement)node);
			break;
		}
		
		return true;

	}

	/**
	 * Register annotations for modified functions.
	 */
	private void visitFunctionNode(FunctionNode functionNode) {
		
		/* Has this function, or its contents, changed in some way? */
		if(ModifiedFunctionVisitor.hasStructuralModifications(functionNode)) {

			/* Since the try statement or its contents has changed, it is
			* interesting to us. Register a fact so the file pair can be stored
			* for a more detailed analysis later. */

			Annotation annotation = new Annotation("MODIFIED_FUNCTION", 
					new LinkedList<DependencyIdentifier>(), 
					functionNode.getLineno(), 
					functionNode.getAbsolutePosition(), 
					functionNode.getLength());
			factBase.registerAnnotationFact(annotation);
			
		}
		
	}
	
	/**
	 * Register annotations for new or modified try statements that do not wrap
	 * a set of moved statements and are therefore mutation candidates.
	 */
	private void visitTryStatement(TryStatement tryStatement) {
		
		/* Does this try statement repair an uncaught exception? */
		if(repairsUncaughtException(tryStatement)) {
			
			Annotation annotation = new Annotation("UNCAUGHT_EXCEPTION", 
					new LinkedList<DependencyIdentifier>(), 
					tryStatement.getLineno(), 
					tryStatement.getAbsolutePosition(), 
					tryStatement.getLength());
			factBase.registerAnnotationFact(annotation);

		}
		
		/* Has this try statement, or its contents, changed in some way? */
		else if(ModifiedTryVisitor.hasStructuralModifications(tryStatement)) {
			
			/* Since the try statement or its contents has changed, it is
			* interesting to us. Register a fact so the file pair can be stored
			* for a more detailed analysis later. */

			Annotation annotation = new Annotation("MUTATION_CANDIDATE", 
					new LinkedList<DependencyIdentifier>(), 
					tryStatement.getLineno(), 
					tryStatement.getAbsolutePosition(), 
					tryStatement.getLength());
			factBase.registerAnnotationFact(annotation);
			
		}
		
	}
	
	/**
	 * @return {@code true} if this statement repairs an uncaught exception.
	 */
	private boolean repairsUncaughtException(TryStatement tryStatement) {
		
		if(tryStatement.getChangeType() != ChangeType.INSERTED) return false;
		
		AstNode tryBlock = tryStatement.getTryBlock();
		
		/* We want to check to see if the contents of the try block is
		* unchanged. For now, we will allow nested changes by only inspecting
		* the statements that are children of the try block. A more aggressive
		* approach would be to require that all nodes inside the try block have
		* 'moved' labels. */
		
		switch(tryBlock.getType()) {
		case Token.BLOCK:
			for(Node child : tryBlock) {
				if(child instanceof AstNode) {
					if(((AstNode)child).getChangeType() != ChangeType.MOVED) return false;
				}
			}
			return true; // All child statements are moved (no inserts or updates)
		default:
			return tryBlock.getChangeType() == ChangeType.MOVED;
		}
		
	}

}