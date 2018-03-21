package multidiffplus.mining.astvisitor.unhandledex;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.TryStatement;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.mining.flow.facts.Slice;
import multidiffplus.mining.flow.facts.SliceChange;
import multidiffplus.mining.flow.facts.SliceFactBase;
import multidiffplus.mining.flow.facts.Statement;

/**
 * Search for try statements that are either modified themselves, or contain a
 * statements which are modified.
 */
public class TryASTAnalysis implements NodeVisitor {
	
	/** The root node being visited. **/
	AstNode root;

	/** Register facts here. */
	SliceFactBase factBase;
	
	/**
	 * @param sourceCodeFileChange used to look up the correct dataset for
	 * storing facts.
	 */
	public TryASTAnalysis(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		this.factBase = SliceFactBase.getInstance(sourceCodeFileChange);
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

			/* Since the function or its contents has changed, it is a new
			* example that we can use for training. Add it as a nominal
			* sequence, which means the sequence should remain unchanged. */

			registerSliceChange(functionNode, functionNode, SliceChange.Type.NOMINAL);
			
		}
		
	}
	
	/**
	 * Register annotations for new or modified try statements that do not wrap
	 * a set of moved statements and are therefore mutation candidates.
	 */
	private void visitTryStatement(TryStatement tryStatement) {
		
		/* Does this try statement repair an uncaught exception? */
		if(repairsUncaughtException(tryStatement)) {
			
			/* Since the try statement repairs an uncaught error exception, it
			* is a concrete example that we can use for training or evaluation.
			* Add it as a concrete repair sequence. */
			
			ScriptNode dstFunct = getEnclosingFunction(tryStatement);
			ScriptNode srcFunct = (ScriptNode)dstFunct.getMapping();
			
			if(srcFunct == null) return; // Avoid case where there is no mapping

			registerSliceChange(srcFunct, dstFunct, SliceChange.Type.REPAIR);

		}
		
		/* Has this try statement, or its contents, changed in some way? */
		else if(ModifiedTryVisitor.hasStructuralModifications(tryStatement)) {
			
			/* Since the try or its contents has changed, it is a new
			* example that we can use for training. Add it as a nominal
			* sequence, which means the sequence should remain unchanged. */

			ScriptNode dstFunct = getEnclosingFunction(tryStatement);
			registerSliceChange(dstFunct, dstFunct, SliceChange.Type.NOMINAL);

			/* Since this is a try statement, we can create a repair example by
			* mutating the function to remove the try statement and use this repair
			* for training. Add it as a mutated repair sequence. */
			
			/* Mutate a repair (remove the try block). */
			MutateTry mutation = new MutateTry(tryStatement);
			ScriptNode mutant = mutation.mutate();
			if(mutant != null)
				registerSliceChange(mutant, dstFunct, SliceChange.Type.MUTANT_REPAIR);
			
		}
		
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
	
	/**
	 * Registers the change to the function's slice.
	 */
	private void registerSliceChange(ScriptNode functA, ScriptNode functB, SliceChange.Type type) {
		Slice before = functA == null ? null : buildSlice(functA);
		Slice after = functB == null ? null : buildSlice(functB);
		factBase.registerSliceFact(new SliceChange(before, after, type));
	}

	/**
	 * Builds a slice. Currently only supports one statement. 
	 * @param node The statement that constitutes the slice.
	 * @return A single-statement slice.
	 */
	private Slice buildSlice(AstNode node) {
		return new Slice(
			new Statement(
					node.toSource(),
					node.getLineno(),
					node.getAbsolutePosition(),
					node.getLength()));
	}

}
