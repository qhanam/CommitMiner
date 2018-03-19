package multidiffplus.mining.ast.analysis;

import java.util.LinkedList;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NodeVisitor;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.MiningFactBase;

/**
 * Search for a jQuery repair pattern where an existing function call is modified.
 */
public class WrapperASTAnalysis implements NodeVisitor {
	
	/** The root node being visited. **/
	AstNode root;

	/** Register facts here. */
	MiningFactBase factBase;
	
	/**
	 * @param sourceCodeFileChange used to look up the correct dataset for
	 * storing facts.
	 */
	public WrapperASTAnalysis(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		this.factBase = MiningFactBase.getInstance(sourceCodeFileChange);
		this.root = root;
	}

	@Override
	public boolean visit(AstNode node) {
		
		/* Stop on function declarations & investigate call sites. */
		switch(node.getType()) {
		case Token.FUNCTION:
			if(node != root) return false;
			break;
		case Token.CALL:
			visitFunctionCall((FunctionCall)node);
			break;
		}
		
		/* Recursively search for AST analysis. */
		return true;

	}
	
	/**
	 * Register annotations for updated calls to $.ajax()
	 */
	public void visitFunctionCall(FunctionCall call) {

		/* Is this a call to a jQuery function? */
		AstNode target = call.getTarget();
//		if(!(target.toSource().matches("\\$\\.[A-Za-z0-9]+")  // Let's see what the most common wrapper functions are.
//				|| target.toSource().matches("jQuery\\.[A-Za-z0-9]+"))) return;

		/* Does this call wrap something? */
		if(call.getChangeType() != ChangeType.INSERTED) return;
		if(call.getArguments().size() == 0) return;
		for(AstNode arg : call.getArguments()) {
			if(arg.getChangeType() != ChangeType.MOVED) return;
		}
		
		/* Register an updated jQuery fact. */
		Annotation annotation = new Annotation(target.toSource(),
				new LinkedList<DependencyIdentifier>(),
				call.getLineno(),
				call.getAbsolutePosition(),
				call.getLength());
		factBase.registerAnnotationFact(annotation);

	}

}
