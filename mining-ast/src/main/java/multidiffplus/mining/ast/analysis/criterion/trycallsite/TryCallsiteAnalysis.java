package multidiffplus.mining.ast.analysis.criterion.trycallsite;

import java.util.Collections;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NodeVisitor;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.MiningFactBase;

/**
 * Search for: (1) new callsites (2) new callsites that are protected by a new
 * try/catch block
 */
public class TryCallsiteAnalysis implements NodeVisitor {

    /** The root node being visited. **/
    AstNode root;

    /** Register facts here. */
    MiningFactBase factBase;

    /**
     * @param sourceCodeFileChange
     *            used to look up the correct dataset for storing facts.
     */
    public TryCallsiteAnalysis(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
	this.factBase = MiningFactBase.getInstance(sourceCodeFileChange);
	this.root = root;

	/* Record that TryCallsiteAnalysis was performed on the file. */
	factBase.registerAnnotationFact(
		new Annotation("TRY_CALLSITE_ANALYSIS", Collections.emptyList(), 0, 0, 0));
    }

    @Override
    public boolean visit(AstNode node) {

	/* Stop on new callsites & investigate try/catch error handling. */
	switch (node.getType()) {
	case Token.CALL:
	    if (node.getChangeType() == ChangeType.INSERTED)
		registerNewCallsite((FunctionCall) node);
	}

	return true;

    }

    /**
     * Register annotations for new callsites.
     * 
     * Looks at the callsite's ancestors for a new try/catch block. If one exists,
     * the callsite is protected. If one does not exist, the callsite is
     * unprotected.
     */
    private void registerNewCallsite(FunctionCall functionCall) {
	AstNode ancestor = functionCall.getParent();
	while (true) {
	    switch (ancestor.getType()) {
	    case Token.CATCH:
	    case Token.FUNCTION:
	    case Token.SCRIPT:
		factBase.registerAnnotationFact(new Annotation("CRITERION_NEW_UNPROTECTED_CALLSITE",
			Collections.emptyList(), functionCall.getLineno(),
			functionCall.getAbsolutePosition(), functionCall.getLength()));
		return;
	    case Token.TRY:
		if (ancestor.getChangeType() == ChangeType.INSERTED) {
		    factBase.registerAnnotationFact(
			    new Annotation("CRITERION_NEW_PROTECTED_CALLSITE",
				    Collections.emptyList(), functionCall.getLineno(),
				    functionCall.getAbsolutePosition(), functionCall.getLength()));
		    return;
		}
	    default:
	    }
	    ancestor = ancestor.getParent();
	}
    }

}