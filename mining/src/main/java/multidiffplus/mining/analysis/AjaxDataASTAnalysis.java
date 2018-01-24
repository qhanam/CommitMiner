package multidiffplus.mining.analysis;

import java.util.LinkedList;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NodeVisitor;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.Annotation;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;

public class AjaxDataASTAnalysis implements NodeVisitor {
	
	/** The root node being visited. **/
	AstNode root;

	/** Register facts here. */
	AnnotationFactBase factBase;
	
	/**
	 * @param sourceCodeFileChange used to look up the correct dataset for
	 * storing facts.
	 */
	public AjaxDataASTAnalysis(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		this.factBase = AnnotationFactBase.getInstance(sourceCodeFileChange);
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

		/* Is this a call to $.ajax? */
		AstNode target = call.getTarget();
		if(target.toSource().equals("$.ajax") || target.toSource().equals("jQuery.ajax")) {
			
			/* Has the call been updated? */
			if(call.getChangeType() == ChangeType.UPDATED) {
				
				/* Register an annotation with the fact database. */
				Annotation annotation = new Annotation("AJAX", 
						new LinkedList<DependencyIdentifier>(), 
						call.getLineno(), 
						call.getAbsolutePosition(), 
						call.getLength());
				factBase.registerAnnotationFact(annotation);
				
			}
			
		}

	}

}
