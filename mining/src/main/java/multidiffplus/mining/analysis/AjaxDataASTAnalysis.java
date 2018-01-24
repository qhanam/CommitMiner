package multidiffplus.mining.analysis;

import java.util.LinkedList;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.Annotation;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;

/**
 * Search for the repair pattern where a JSON object is incorrectly passed as an
 * object literal instead of a stringified JSON object.
 * 
 * From http://api.jquery.com/jquery.ajax/:
 * """
 * The data option can contain either a query string of the form
 * key1=value1&key2=value2, or an object of the form {key1: 'value1', key2:
 * 'value2'}. If the latter form is used, the data is converted into a query
 * string using jQuery.param() before it is sent. ... The processing might be
 * undesirable... in this case, change the contentType option from
 * application/x-www-form-urlencoded to a more appropriate MIME type.
 * """
 */
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
		if(!target.toSource().equals("$.ajax") && !target.toSource().equals("jQuery.ajax")) return;
		
		/* Has the call been updated? */
		if(call.getChangeType() != ChangeType.UPDATED) return;
				
		/* Find the settings argument (usually an object literal). */
		ObjectLiteral settings = null;
		for(AstNode arg : call.getArguments()) {
			if(arg.getType() == Token.OBJECTLIT) settings = (ObjectLiteral) arg;
		}
		if(settings == null) return;
		
		/* Find and check the data field. */
		for(ObjectProperty property : settings.getElements()) {

			FunctionCall stringify = null;
			AstNode field = property.getLeft();
			AstNode value = property.getRight();
			
			switch(field.getType()) {
			case Token.NAME:
				if(!field.toSource().equals("data")) continue;
				else break;
			case Token.STRING:
				if(!((StringLiteral)field).getValue().equals("data")) continue;
				else break;
			}

			/* Inserted call site? */
			if(value.getChangeType() != ChangeType.INSERTED
					|| value.getType() != Token.CALL) return;

			stringify = (FunctionCall) value;

			/* Call to JSON.stringify with one argument? */
			if(!stringify.getTarget().toSource().equals("JSON.stringify")
					|| stringify.getArguments().size() != 1) return;
			
			/* Argument is an object literal? */
			for(AstNode node : stringify.getArguments()) {
				if(node.getType() != Token.OBJECTLIT
						|| node.getChangeType() != ChangeType.MOVED) return;
			}

			/* Register an annotation with the fact database. */
			Annotation annotation = new Annotation("AJAX_STRINGIFY", 
					new LinkedList<DependencyIdentifier>(), 
					call.getLineno(), 
					call.getAbsolutePosition(), 
					call.getLength());
			factBase.registerAnnotationFact(annotation);

		}

	}
	
	/**
	 * 
	 */
	public void visitUpdatedAjaxCall(FunctionCall call) {
		
		for(AstNode arg : call.getArguments()) {
			
			switch(arg.getType()) {
			case Token.OBJECTLIT:
				
			}
			
		}
		
	}
	
	public void visitAjaxSettings(ObjectLiteral obj) {
		
	}

}
