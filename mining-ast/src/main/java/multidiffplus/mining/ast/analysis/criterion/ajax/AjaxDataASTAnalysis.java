package multidiffplus.mining.ast.analysis.criterion.ajax;

import java.util.Collections;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.AnnotationFactBase;

/**
 * Search for the repair pattern where a JSON object is incorrectly passed as an
 * object literal instead of a stringified JSON object.
 * 
 * From http://api.jquery.com/jquery.ajax/: """ The data option can contain
 * either a query string of the form key1=value1&key2=value2, or an object of
 * the form {key1: 'value1', key2: 'value2'}. If the latter form is used, the
 * data is converted into a query string using jQuery.param() before it is sent.
 * ... The processing might be undesirable... in this case, change the
 * contentType option from application/x-www-form-urlencoded to a more
 * appropriate MIME type. """
 */
public class AjaxDataASTAnalysis implements NodeVisitor {

    /** The root node being visited. **/
    AstNode root;

    /** Register facts here. */
    AnnotationFactBase factBase;

    /**
     * @param sourceCodeFileChange
     *            used to look up the correct dataset for storing facts.
     */
    public AjaxDataASTAnalysis(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
	this.factBase = AnnotationFactBase.getInstance(sourceCodeFileChange);
	this.root = root;
    }

    @Override
    public boolean visit(AstNode node) {

	/* Stop on function declarations & investigate call sites. */
	switch (node.getType()) {
	case Token.FUNCTION:
	    if (node != root)
		return false;
	    break;
	case Token.CALL:
	    visitFunctionCall((FunctionCall) node);
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
	if (!target.toSource().equals("$.ajax") && !target.toSource().equals("jQuery.ajax"))
	    return;

	/* Is this a new or updated call? */
	if (call.getChangeType() != ChangeType.INSERTED
		&& call.getChangeType() != ChangeType.UPDATED)
	    return;

	/* Find the settings argument (usually an object literal). */
	ObjectLiteral settings = null;
	for (AstNode arg : call.getArguments()) {
	    if (arg.getType() == Token.OBJECTLIT)
		settings = (ObjectLiteral) arg;
	}
	if (settings == null)
	    return;

	/* Find the data field. */
	ObjectProperty dataProperty = getDataField(settings);

	if (dataProperty == null) {

	    /* There is no data being sent. */
	    Annotation annotation = new Annotation("AJAX_OTHER", Collections.emptyList(),
		    settings.getLineno(), settings.getAbsolutePosition(), settings.getLength());
	    factBase.registerAnnotationFact(annotation);

	    return;

	}

	/* Find the call to JSON.stringify. */
	FunctionCall stringify = getStringify(dataProperty.getRight());

	if (stringify == null) {

	    /* This is mutation candidate. */
	    Annotation annotation = new Annotation("AJAX_STRINGIFY_MUTATE_ADD",
		    Collections.emptyList(), dataProperty.getRight().getLineno(),
		    dataProperty.getRight().getAbsolutePosition(),
		    dataProperty.getRight().getLength());
	    factBase.registerAnnotationFact(annotation);

	    return;

	}

	/* Register a AJAX_STRINGIFY fact. */
	if (call.getChangeType() == ChangeType.UPDATED
		&& dataProperty.getChangeType() == ChangeType.UPDATED
		&& stringify.getChangeType() == ChangeType.INSERTED) {

	    /* This is a repair. */
	    Annotation annotation = new Annotation("AJAX_STRINGIFY_REPAIR", Collections.emptyList(),
		    stringify.getLineno(), stringify.getAbsolutePosition(), stringify.getLength());
	    factBase.registerAnnotationFact(annotation);

	} else if (call.getChangeType() == ChangeType.UPDATED
		|| call.getChangeType() == ChangeType.INSERTED) {

	    /* This is mutation candidate. */
	    Annotation annotation = new Annotation("AJAX_STRINGIFY_MUTATE_DEL",
		    Collections.emptyList(), stringify.getLineno(), stringify.getAbsolutePosition(),
		    stringify.getLength());
	    factBase.registerAnnotationFact(annotation);

	}

    }

    /**
     * @return the call to JSON.stringify(value), or {@code null} if not stringified
     */
    private FunctionCall getStringify(AstNode value) {

	if (value.getType() != Token.CALL)
	    return null;

	FunctionCall call = (FunctionCall) value;

	/* Call to JSON.stringify with one argument? */
	if (call.getTarget().toSource().equals("JSON.stringify")
		|| call.getTarget().toSource().equals("$.toJSON")
		|| call.getTarget().toSource().equals("jQuery.toJSON")) {
	    return call;
	}

	return null;

    }

    /**
     * @return The object property of the 'data' field, or {@code null} if not
     *         found.
     */
    private ObjectProperty getDataField(ObjectLiteral settings) {

	/* Find and check the data field. */
	for (ObjectProperty property : settings.getElements()) {

	    AstNode field = property.getLeft();

	    switch (field.getType()) {
	    case Token.NAME:
		if (field.toSource().equals("data"))
		    return property;
		break;
	    case Token.STRING:
		if (((StringLiteral) field).getValue().equals("data"))
		    return property;
		break;
	    }

	}

	/* The 'data' field is not in the object literal. */
	return null;

    }

}
