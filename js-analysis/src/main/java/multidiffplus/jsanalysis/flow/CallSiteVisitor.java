package multidiffplus.jsanalysis.flow;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;

/**
 * A utility for creating an ordered list of function calls within a statement.
 */
public class CallSiteVisitor {

    /**
     * Returns an ordered list of function calls within a statement. Function calls
     * are sorted in topological order, so that the call sites that have
     * dependencies occur in the list before their dependencies, and can therefore
     * be evaluated first.
     */
    public static List<ClassifiedASTNode> getCallSites(AstNode statement) {
	CallSiteVisitor visitor = new CallSiteVisitor();
	visitor.topsort(statement);
	return visitor.callSites;
    }

    private List<ClassifiedASTNode> callSites;

    private CallSiteVisitor() {
	callSites = new ArrayList<>();
    }

    public void topsort(AstNode node) {
	if (node instanceof FunctionCall) {
	    topsort((FunctionCall) node);
	} else if (node instanceof ExpressionStatement) {
	    topsort(((ExpressionStatement) node).getExpression());
	} else if (node instanceof InfixExpression) {
	    topsort(((InfixExpression) node).getLeft());
	    topsort(((InfixExpression) node).getRight());
	} else if (node instanceof UnaryExpression) {
	    topsort(((UnaryExpression) node).getOperand());
	} else if (node instanceof ElementGet) {
	    topsort(((ElementGet) node).getTarget());
	    topsort(((ElementGet) node).getElement());
	} else if (node instanceof ReturnStatement) {
	    topsort(((ReturnStatement) node).getReturnValue());
	} else if (node instanceof ThrowStatement) {
	    topsort(((ThrowStatement) node).getExpression());
	} else if (node instanceof ParenthesizedExpression) {
	    topsort(((ParenthesizedExpression) node).getExpression());
	} else if (node instanceof VariableDeclaration) {
	    ((VariableDeclaration) node).getVariables().forEach(vd -> topsort(vd));
	} else if (node instanceof VariableInitializer) {
	    topsort(((VariableInitializer) node).getInitializer());
	} else if (node instanceof ArrayLiteral) {
	    for (AstNode element : ((ArrayLiteral) node).getElements())
		topsort(element);
	} else if (node instanceof ObjectLiteral) {
	    for (ObjectProperty property : ((ObjectLiteral) node).getElements()) {
		topsort(property);
	    }
	}
    }

    private void topsort(FunctionCall fc) {
	topsort(fc.getTarget());
	for (AstNode arg : fc.getArguments()) {
	    topsort(arg);
	}
	callSites.add(fc);
    }

}
