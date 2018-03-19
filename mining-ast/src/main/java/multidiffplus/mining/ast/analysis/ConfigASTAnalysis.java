package multidiffplus.mining.ast.analysis;

import java.util.LinkedList;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ObjectLiteral;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.MiningFactBase;

/**
 * Search for a jQuery repair pattern where an existing function call is modified.
 */
public class ConfigASTAnalysis implements NodeVisitor {
	
	/** The root node being visited. **/
	AstNode root;

	/** Register facts here. */
	MiningFactBase factBase;
	
	/**
	 * @param sourceCodeFileChange used to look up the correct dataset for
	 * storing facts.
	 */
	public ConfigASTAnalysis(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
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
	 * Register annotations for calls with updated object literals.
	 */
	public void visitFunctionCall(FunctionCall call) {

		/* Is this an existing call? */
		AstNode target = call.getTarget();
		if(call.getChangeType() != ChangeType.UPDATED) return;

		/* Does this call have an object literal as an argument? */
		for(AstNode arg : call.getArguments()) {

			if(!(arg instanceof ObjectLiteral)) continue;

			ObjectLiteral config = (ObjectLiteral)arg;
			if(config.getChangeType() != ChangeType.UPDATED) continue;
			
			/* Register an updated jQuery fact. */
			Annotation annotation = new Annotation(target.toSource(),
					new LinkedList<DependencyIdentifier>(),
					call.getLineno(),
					call.getAbsolutePosition(),
					call.getLength());
			factBase.registerAnnotationFact(annotation);
			break;
			
		}

	}

}
